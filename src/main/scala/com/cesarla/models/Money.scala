package com.cesarla.models

import com.cesarla.utils.JsonFormatting
import play.api.libs.json.{Format, Json}

final case class Money(total: BigDecimal, currency: String) {
  def unary_- : Money = this.copy(-total)
  def +(that: Money) = Money(total + that.total, currency)
  def -(that: Money) = Money(total - that.total, currency)
}

object Money extends JsonFormatting {
  val Euro = "EUR"
  def zero(currency: String) = Money(0, currency)

  implicit val jsonFormats: Format[Money] = Json.format[Money]
}
