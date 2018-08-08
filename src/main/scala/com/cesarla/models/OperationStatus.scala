package com.cesarla.models

import enumeratum.EnumEntry.Uppercase
import enumeratum._

import scala.collection.immutable

sealed trait OperationStatus extends EnumEntry with Uppercase

object OperationStatus extends Enum[OperationStatus] with PlayJsonEnum[OperationStatus] {
  val values: immutable.IndexedSeq[OperationStatus] = findValues

  case object Successful extends OperationStatus
  case object Failed extends OperationStatus
  case object InProgress extends OperationStatus
}
