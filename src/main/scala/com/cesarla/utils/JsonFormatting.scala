package com.cesarla.utils

import play.api.libs.json.JsonConfiguration
import play.api.libs.json.JsonNaming.SnakeCase

trait JsonFormatting {
  implicit val config = JsonConfiguration(SnakeCase)
}
