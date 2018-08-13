package com.cesarla.models.api

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class AccountRequestSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "AccountRequest" should {

    "serialize" in {
      val json: JsValue = Json.toJson(accountRequest)
      (json \ "currency").as[String] shouldBe accountRequest.currency
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "currency": "EUR"
          |}
        """.stripMargin)
      json.as[AccountRequest] shouldBe accountRequest
    }
  }
}
