package com.cesarla.models

import java.time.Instant

import com.cesarla.utils.JsonFormatting
import play.api.libs.json._

sealed trait Operation {
  val operationId: OperationId
  val createdAt: Instant
  val status: OperationStatus
  val detail: Option[String]
}

final case class Transfer(operationId: OperationId,
                          sourceId: AccountId,
                          targetId: AccountId,
                          money: Money,
                          createdAt: Instant,
                          status: OperationStatus = OperationStatus.Progress,
                          detail: Option[String] = None)
    extends Operation {
  def asDeposit: Deposit = Deposit(operationId, targetId, money, createdAt, status)
  def asRecord: Record = Record(targetId, -money, createdAt, Some(operationId))
}

object Transfer extends JsonFormatting {
  implicit val jsonFormat: OFormat[Transfer] = Json.format[Transfer]
}

final case class Deposit(operationId: OperationId,
                         accountId: AccountId,
                         money: Money,
                         createdAt: Instant,
                         status: OperationStatus = OperationStatus.Progress,
                         detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, money, createdAt, Some(operationId))
}

object Deposit extends JsonFormatting {
  implicit val jsonFormats: OFormat[Deposit] = Json.format[Deposit]
}

final case class Withdrawal(operationId: OperationId,
                            accountId: AccountId,
                            money: Money,
                            createdAt: Instant,
                            status: OperationStatus = OperationStatus.Progress,
                            detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, -money, createdAt, Some(operationId))
}

object Withdrawal extends JsonFormatting {
  implicit val jsonFormats: OFormat[Withdrawal] = Json.format[Withdrawal]
}
