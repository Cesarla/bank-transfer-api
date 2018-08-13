package com.cesarla.models.api

import com.cesarla.models.{AccountId, Money}
import com.cesarla.utils.JsonFormatting
import play.api.libs.json._

final case class TransferRequest(
    targetId: AccountId,
    money: Money
)

object TransferRequest extends JsonFormatting {
  implicit val jsonFormats: OFormat[TransferRequest] = Json.format[TransferRequest]
}
