package com.cesarla.models

import com.cesarla.utils.JsonFormatting
import play.api.libs.json._

final case class TransferRequest (
                          targetId: AccountId,
                          money: Money
                         )

object TransferRequest extends JsonFormatting {
  implicit val jsonFormats: OFormat[TransferRequest] = Json.format[TransferRequest]
  }
