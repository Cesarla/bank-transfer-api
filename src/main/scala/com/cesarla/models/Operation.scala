package com.cesarla.models

import java.time.Instant

import com.cesarla.utils.JsonFormatting
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._

sealed trait Operation {
  val operationId: OperationId
  val createAt: Instant
  val status: OperationStatus
  val detail: Option[String]

  def setStatus(status: OperationStatus): Operation = {
    this match {
      case transfer: Transfer     => transfer.copy(status = status)
      case deposit: Deposit       => deposit.copy(status = status)
      case withdrawal: Withdrawal => withdrawal.copy(status = status)
    }
  }
}

final case class Transfer(operationId: OperationId,
                          sourceId: AccountId,
                          targetId: AccountId,
                          money: Money,
                          createAt: Instant,
                          status: OperationStatus,
                          detail: Option[String] = None)
    extends Operation {
  def asSourceRecord: Record = Record(sourceId, money, createAt, Some(operationId))
  def asTargetRecord: Record = Record(targetId, -money, createAt, Some(operationId))
}

object Transfer extends JsonFormatting {
  implicit val jsonWrites: OWrites[Transfer] = Json.writes[Transfer]
  implicit val jsonReads: Reads[Transfer] = (
    (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
      (JsPath \ "source_id").read[AccountId] and
      (JsPath \ "target_id").read[AccountId] and
      (JsPath \ "money").read[Money] and
      (JsPath \ "create_at").readWithDefault(Instant.now()) and
      (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.InProgress) and
      (JsPath \ "detail").readNullable[String]
  )(Transfer.apply _)
}

final case class Deposit(operationId: OperationId,
                         accountId: AccountId,
                         money: Money,
                         createAt: Instant,
                         status: OperationStatus,
                         detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, money, createAt, Some(operationId))
}

object Deposit extends JsonFormatting {
  implicit val jsonWrites: OWrites[Deposit] = Json.writes[Deposit]
  implicit val jsonReads: Reads[Deposit] = (
    (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
      (JsPath \ "account_id").read[AccountId] and
      (JsPath \ "money").read[Money] and
      (JsPath \ "create_at").readWithDefault(Instant.now()) and
      (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.InProgress) and
      (JsPath \ "detail").readNullable[String]
  )(Deposit.apply _)
}

final case class Withdrawal(operationId: OperationId,
                            accountId: AccountId,
                            money: Money,
                            createAt: Instant,
                            status: OperationStatus,
                            detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, money, createAt, Some(operationId))
}

object Withdrawal extends JsonFormatting {
  implicit val jsonWrites: OWrites[Withdrawal] = Json.writes[Withdrawal]
  implicit val jsonReads: Reads[Withdrawal] = (
    (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
      (JsPath \ "account_id").read[AccountId] and
      (JsPath \ "money").read[Money] and
      (JsPath \ "create_at").readWithDefault(Instant.now()) and
      (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.InProgress) and
      (JsPath \ "detail").readNullable[String]
  )(Withdrawal.apply _)
}