package com.cesarla.services

import akka.http.scaladsl.model.StatusCodes
import com.cesarla.models.{Operation, _}
import com.cesarla.persistence.{Ledger, OperationRepository}

import scala.concurrent.Future
import scala.reflect.ClassTag

class LedgerService(ledger: Ledger, operationRepository: OperationRepository) {

  private[this] val pubSubService: PubSubService[Operation] = new PubSubService[Operation]({
    case transfer: Transfer     => processTransfer(transfer)
    case deposit: Deposit       => processDeposit(deposit)
    case withdrawal: Withdrawal => processWithdrawal(withdrawal)
  })

  def dispatchOperation[T <: Operation](operation: T)(implicit tag: ClassTag[T]): Future[Either[Problem, T]] =
    Future.successful {
      operationRepository.setOperation(operation)
      pubSubService.publish(operation)
      operationRepository.getOperation(operation.operationId) match {
        case Some(current: T) => Right(current)
        case _                => Left(Problem("It should not happen", StatusCodes.InternalServerError))
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
      Left(Problem(s"Account $accountId not found", StatusCodes.NotFound))
    } else Right(ledger.replayAccount(accountId))
  }

  private[this] def processDeposit(deposit: Deposit): Unit = {
    ledger.storeRecord(deposit.accountId, deposit.asRecord)
    operationRepository.setOperation(deposit.copy(status = OperationStatus.Successful))
    ()
  }

  private[this] def processWithdrawal(withdrawal: Withdrawal): Unit = {
    val snapshot = ledger.replayAccount(withdrawal.accountId)
    if (withdrawal.money.total > snapshot.balance.total) {
      operationRepository.setOperation(
        withdrawal.copy(status = OperationStatus.Failed, detail = Some("Not enough founds")))
    } else {
      ledger.storeRecord(withdrawal.accountId, withdrawal.asRecord)
      operationRepository.setOperation(withdrawal.copy(status = OperationStatus.Successful))
    }
    ()
  }

  private[this] def processTransfer(transfer: Transfer): Unit = {
    val snapshot = ledger.replayAccount(transfer.sourceId)
    if (transfer.money.total > snapshot.balance.total) {
      operationRepository.setOperation(
        transfer.copy(status = OperationStatus.Failed, detail = Some("Not enough founds")))
    } else {
      ledger.storeRecord(transfer.sourceId, transfer.asSourceRecord)
      ledger.storeRecord(transfer.targetId, transfer.asTargetRecord)
      operationRepository.setOperation(transfer.copy(status = OperationStatus.Successful))
    }
    ()
  }
}
