package com.cesarla.persistence

import com.cesarla.models._

import scala.collection.concurrent.TrieMap

class OperationRepository(val map: TrieMap[OperationId, Operation] = TrieMap.empty[OperationId, Operation]) {

  def getOperation(operationId: OperationId): Option[Operation] = map.get(operationId)

  def setOperation(operation: Operation): Operation = {
    map.put(operation.operationId, operation)
    operation
  }

  def setAsSuccessful(operationId: OperationId): Unit = {
    map
      .get(operationId)
      .map {
        case deposit: Deposit       => deposit.copy(status = OperationStatus.Successful)
        case withdrawal: Withdrawal => withdrawal.copy(status = OperationStatus.Successful)
        case transfer: Transfer     => transfer.copy(status = OperationStatus.Successful)
      }
      .foreach(operation => map.put(operationId, operation))
  }

  def setAsFailed(operationId: OperationId, detail: Option[String] = None): Unit = {
    map
      .get(operationId)
      .map {
        case deposit: Deposit       => deposit.copy(status = OperationStatus.Failed, detail = detail)
        case withdrawal: Withdrawal => withdrawal.copy(status = OperationStatus.Failed, detail = detail)
        case transfer: Transfer     => transfer.copy(status = OperationStatus.Failed, detail = detail)
      }
      .foreach(operation => map.put(operationId, operation))
  }
}
