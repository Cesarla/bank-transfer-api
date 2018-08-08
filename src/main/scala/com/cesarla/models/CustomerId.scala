package com.cesarla.models

import java.util.UUID

import com.cesarla.utils.JsonFormatting
import com.fasterxml.uuid.Generators
import play.api.libs.json._

final case class CustomerId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object CustomerId extends JsonFormatting {
  def generate: CustomerId = CustomerId(Generators.timeBasedGenerator().generate)
  def valueOf(value: String) = CustomerId(UUID.fromString(value))
  implicit val jsonFormats = Format(JsPath.read[String].map(UUID.fromString).map(CustomerId.apply),
                                    Writes[CustomerId](customerId => JsString(customerId.value.toString)))
}
