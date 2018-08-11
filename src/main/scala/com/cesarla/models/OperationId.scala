package com.cesarla.models

import java.util.UUID

import com.cesarla.utils.JsonFormatting
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import play.api.libs.json.{Format, JsPath, JsString, Writes}

final case class OperationId(value: UUID) extends AnyVal {
  override def toString: String = value.toString
}

object OperationId extends JsonFormatting {
  implicit val jsonFormats: Format[OperationId] = Format(
    JsPath.read[String].map(UUID.fromString).map(OperationId.apply),
    Writes[OperationId](operationId => JsString(operationId.value.toString)))

  def generate(implicit ug: UUID1Generator): OperationId = OperationId(ug.generate)

  def valueOf(value: String) = OperationId(UUID.fromString(value))
}
