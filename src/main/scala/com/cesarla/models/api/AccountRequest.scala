package com.cesarla.models.api
import com.cesarla.utils.JsonFormatting
import play.api.libs.json.{Json, OFormat}

final case class AccountRequest(
    currency: String
)

object AccountRequest extends JsonFormatting {
  implicit val jsonFormats: OFormat[AccountRequest] = Json.format[AccountRequest]
}
