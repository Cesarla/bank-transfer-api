package com.cesarla.services

import akka.http.scaladsl.model.StatusCodes
import com.cesarla.models.{Operation, _}
import com.cesarla.persistence.{Ledger, OperationRepository}
import com.google.common.hash.Hashing

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.reflect.ClassTag

class LedgerService(ledger: Ledger, operationRepository: OperationRepository, shards: Int) {

  private[this] val pubSubServices: TrieMap[Int, PubSubService] = {
    val trieMap = TrieMap.empty[Int, PubSubService]
    (0 to shards).foreach(
      i =>
        trieMap.update(
          i,
          new PubSubService({
            case transfer: Transfer     => processTransfer(transfer)
            case deposit: Deposit       => processDeposit(deposit)
            case withdrawal: Withdrawal => processWithdrawal(withdrawal)
          })
      ))
    trieMap
  }

  def getPubSubService(operation: Operation): PubSubService = {
    val accountId = operation match {
      case deposit: Deposit       => deposit.accountId
      case withdrawal: Withdrawal => withdrawal.accountId
      case transfer: Transfer     => transfer.sourceId
    }

    // Using .head since pubSubServices is not mutated after instantiation
    pubSubServices.get(Hashing.consistentHash(accountId.hashCode().toLong, shards)).head
  }

  def dispatchOperation[T <: Operation](operation: T)(implicit tag: ClassTag[T]): Future[Either[Problem, T]] = {
    Future.successful {
      operationRepository.setOperation(operation)
      getPubSubService(operation).publish(operation)
      operationRepository.getOperation(operation.operationId) match {
        case Some(current: T) => Right(current)
        case _                => Left(Problem("It should not happen", StatusCodes.InternalServerError))
      }
    }
  }

  def get[T](operationId: OperationId)(implicit tag: ClassTag[T]): Either[Problem, T] = {
    operationRepository.getOperation(operationId) match {
      case Some(operation: T) => Right(operation)
      case _                  => Left(Problem(s"Operation $operationId not found", StatusCodes.NotFound))
    }
  }

  def computeBalance(accountId: AccountId): Future[Either[Problem, Snapshot]] = Future.successful {
    if (ledger.getRecords(accountId).isEmpty) {
      Left(Problems.NotFound(s"Account $accountId not found"))
    } else Right(ledger.replayAccount(accountId))
  }

  private[this] def processDeposit(deposit: Deposit): Unit = {
    ledger.storeRecord(deposit.accountId, deposit.asRecord)
    operationRepository.setAsSuccessful(deposit.operationId)
  }

  private[this] def processWithdrawal(withdrawal: Withdrawal): Unit = {
    val snapshot = ledger.replayAccount(withdrawal.accountId)
    if (withdrawal.money.total > snapshot.balance.total) {
      operationRepository.setAsFailed(withdrawal.operationId, Some("Not enough founds"))
    } else {
      ledger.storeRecord(withdrawal.accountId, withdrawal.asRecord)
      operationRepository.setAsSuccessful(withdrawal.operationId)
    }
  }

  private[this] def processTransfer(transfer: Transfer): Unit = {
    val snapshot = ledger.replayAccount(transfer.sourceId)
    if (transfer.money.total > snapshot.balance.total) {
      operationRepository.setAsFailed(transfer.operationId, Some("Not enough founds"))
    } else {
      val deposit: Deposit = transfer.asDeposit
      ledger.storeRecord(transfer.sourceId, transfer.asRecord)
      if (belongsToSameBucket(transfer.sourceId, transfer.targetId)) {
        processDeposit(deposit)
      } else {
        getPubSubService(deposit).publish(deposit)
      }
    }
  }

  private[this] def belongsToSameBucket(accountA: AccountId, accountB: AccountId): Boolean = {
    Hashing.consistentHash(accountA.hashCode().toLong, shards) == Hashing.consistentHash(accountB.hashCode().toLong,
                                                                                         shards)
  }
}
