package com.cesarla.models

import com.cesarla.utils.JsonFormatting
import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Money(total: BigDecimal, currency: String) {
  def unary_- : Money = this.copy(-total)
  def +(that: Money) = Money(total + that.total, currency)
  def -(that: Money) = Money(total - that.total, currency)
}

object Money extends JsonFormatting {
  val Euro = "EUR"
  def zero(currency: String) = Money(0, currency)

  implicit val jsonReads: Reads[Money] = (
    (JsPath \ "total").read[String].map(BigDecimal.apply) and
      (JsPath \ "currency").read[String]
  )(Money.apply _)

  implicit val jsonWrites: Writes[Money] = (
    (JsPath \ "total").write[String].contramap((b: BigDecimal) => b.setScale(4, BigDecimal.RoundingMode.DOWN).toString()) and
      (JsPath \ "currency").write[String]
  )(unlift(Money.unapply))
}
