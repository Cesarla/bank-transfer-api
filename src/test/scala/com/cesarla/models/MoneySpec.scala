package com.cesarla.models

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class MoneySpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Money" should {
    "serialize" in {
      val json: JsValue = Json.toJson(moneyFixture)
      (json \ "total").as[String] shouldBe "42.0000"
      (json \ "currency").as[String] shouldBe "EUR"
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |  "total": "42.0000",
          |  "currency": "EUR"
          |}
        """.stripMargin)
      json.as[Money] shouldBe moneyFixture
    }
  }
}
