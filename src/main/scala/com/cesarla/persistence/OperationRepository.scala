package com.cesarla.persistence

import com.cesarla.models.{Operation, OperationId}

import scala.collection.concurrent.TrieMap

class OperationRepository(val map: TrieMap[OperationId, Operation] = TrieMap.empty[OperationId, Operation]) {

  def getOperation(operationId: OperationId): Option[Operation] = map.get(operationId)

  def setOperation(operation: Operation): Operation = {
    map.put(operation.operationId, operation)
    operation
  }
}
