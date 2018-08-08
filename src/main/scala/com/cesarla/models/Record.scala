package com.cesarla.models

import java.time.Instant

import com.cesarla.utils.JsonFormatting
import play.api.libs.json.Json

final case class Record(accountId: AccountId,
                        balance: Money,
                        createdAt: Instant,
                        operationId: Option[OperationId] = None)

object Record extends JsonFormatting {
  implicit val jsonFormats = Json.format[Record]
}
