package com.cesarla.models

import java.time.Instant

import com.cesarla.utils.JsonFormatting
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import play.api.libs.json.Json
import play.api.libs.json._
import play.api.libs.functional.syntax._

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
                          status: OperationStatus,
                          detail: Option[String] = None)
    extends Operation {
  def asSourceRecord: Record = Record(sourceId, money, createdAt, Some(operationId))
  def asTargetRecord: Record = Record(targetId, -money, createdAt, Some(operationId))
}

object Transfer extends JsonFormatting {
  implicit val jsonWrites: OWrites[Transfer] = Json.writes[Transfer]
  implicit def jsonReads(implicit ug: UUID1Generator): Reads[Transfer] =
    (
      (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
        (JsPath \ "source_id").read[AccountId] and
        (JsPath \ "target_id").read[AccountId] and
        (JsPath \ "money").read[Money] and
        (JsPath \ "created_at").readWithDefault(Instant.now()) and
        (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.Progress) and
        (JsPath \ "detail").readNullable[String]
    )(Transfer.apply _)
}

final case class Deposit(operationId: OperationId,
                         accountId: AccountId,
                         money: Money,
                         createdAt: Instant,
                         status: OperationStatus,
                         detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, money, createdAt, Some(operationId))
}

object Deposit extends JsonFormatting {
  implicit val jsonWrites: OWrites[Deposit] = Json.writes[Deposit]
  implicit def jsonReads(implicit ug: UUID1Generator): Reads[Deposit] =
    (
      (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
        (JsPath \ "account_id").read[AccountId] and
        (JsPath \ "money").read[Money] and
        (JsPath \ "created_at").readWithDefault(Instant.now()) and
        (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.Progress) and
        (JsPath \ "detail").readNullable[String]
    )(Deposit.apply _)
}

final case class Withdrawal(operationId: OperationId,
                            accountId: AccountId,
                            money: Money,
                            createdAt: Instant,
                            status: OperationStatus,
                            detail: Option[String] = None)
    extends Operation {
  def asRecord: Record = Record(accountId, -money, createdAt, Some(operationId))
}

object Withdrawal extends JsonFormatting {
  implicit val jsonWrites: OWrites[Withdrawal] = Json.writes[Withdrawal]
  implicit def jsonReads(implicit ug: UUID1Generator): Reads[Withdrawal] =
    (
      (JsPath \ "operation_id").readWithDefault(OperationId.generate) and
        (JsPath \ "account_id").read[AccountId] and
        (JsPath \ "money").read[Money] and
        (JsPath \ "created_at").readWithDefault(Instant.now()) and
        (JsPath \ "status").readWithDefault[OperationStatus](OperationStatus.Progress) and
        (JsPath \ "detail").readNullable[String]
    )(Withdrawal.apply _)
}
