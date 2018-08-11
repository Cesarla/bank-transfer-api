package com.cesarla.models

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class MoneySpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Money" should {
    "serialize" in {
      val json: JsValue = Json.toJson(moneyFixture)
      (json \ "total").as[String] should ===("42.0000")
      (json \ "currency").as[String] should ===( "EUR")
    }

    "deserialize" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "total": "42.0000",
          |  "currency": "EUR"
          |}
        """.stripMargin)
      json.as[Money] should === (moneyFixture)
    }
  }
}