package com.cesarla.models

import java.util.UUID

import com.cesarla.utils.JsonFormatting
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import play.api.libs.json.{Format, JsPath, JsString, Writes}

final case class AccountId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object AccountId extends JsonFormatting {
  implicit val jsonFormats: Format[AccountId] = Format(
    JsPath.read[String].map(UUID.fromString).map(AccountId.apply),
    Writes[AccountId](accountId => JsString(accountId.value.toString)))

  def generate(implicit ug: UUID1Generator): AccountId = AccountId(ug.generate)

  def valueOf(value: String) = AccountId(UUID.fromString(value))
}
