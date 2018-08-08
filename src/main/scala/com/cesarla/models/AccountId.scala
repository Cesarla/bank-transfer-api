package com.cesarla.models

import java.util.UUID

import com.cesarla.utils.JsonFormatting
import com.fasterxml.uuid.Generators
import play.api.libs.json.{Format, JsPath, JsString, Writes}

final case class AccountId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object AccountId extends JsonFormatting {
  def generate: AccountId = AccountId(Generators.timeBasedGenerator().generate)

  def valueOf(value: String) = AccountId(UUID.fromString(value))

  implicit val jsonFormats: Format[AccountId] = Format(
    JsPath.read[String].map(UUID.fromString).map(AccountId.apply),
    Writes[AccountId](accountId => JsString(accountId.value.toString)))
}
