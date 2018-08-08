package com.cesarla.models

import akka.http.scaladsl.model.StatusCode
import com.cesarla.utils.JsonFormatting
import play.api.libs.json._

final case class Problem(reason: String, status: StatusCode)

object Problem extends JsonFormatting {
  implicit val statusFormats: Format[StatusCode] = Format(
    JsPath.read[Int].map(StatusCode.int2StatusCode),
    Writes[StatusCode](statusCode => JsNumber(statusCode.intValue())))

  implicit val jsonFormats: Format[Problem] = Json.format[Problem]
}
