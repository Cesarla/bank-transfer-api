package com.cesarla.models

import java.time.Instant

import com.cesarla.utils.JsonFormatting
import play.api.libs.json.{Json, OFormat}

final case class Snapshot(accountId: AccountId, balance: Money, updatedAt: Instant)

object Snapshot extends JsonFormatting {
  implicit val jsonFormats: OFormat[Snapshot] = Json.format[Snapshot]
}
