package com.cesarla.models

import com.cesarla.utils.JsonFormatting
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.json.Json
import play.api.libs.functional.syntax._

final case class Customer(id: CustomerId,
                          email: String,
                          accounts: Map[String, AccountId] = Map.empty[String, AccountId])

object Customer extends JsonFormatting {
  implicit val jsonWrites = Json.writes[Customer]
  implicit val jsonReads: Reads[Customer] = (
    (JsPath \ "id").readWithDefault(CustomerId.generate) and
      (JsPath \ "email").read[String] and
      (JsPath \ "accounts").readWithDefault(Map.empty[String, AccountId])
  )(Customer.apply _)
}
