package com.cesarla.models

import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import com.cesarla.utils.JsonFormatting
import play.api.libs.json._

final case class Problem(reason: String, status: StatusCode) {
  def asResult: (StatusCode, Problem) = (status, this)
}

object Problem extends JsonFormatting {
  implicit val statusFormats: Format[StatusCode] = Format(
    JsPath.read[Int].map(StatusCode.int2StatusCode),
    Writes[StatusCode](statusCode => JsNumber(statusCode.intValue())))

  implicit val jsonFormats: Format[Problem] = Json.format[Problem]
}

object Problems {
  def NotFound(reason: String): Problem = Problem(reason, StatusCodes.NotFound)
  def BadRequest(reason: String): Problem = Problem(reason, StatusCodes.BadRequest)
  def Conflict(reason: String): Problem = Problem(reason, StatusCodes.Conflict)
  def InternalServerError(reason: String): Problem = Problem(reason, StatusCodes.InternalServerError)
}
