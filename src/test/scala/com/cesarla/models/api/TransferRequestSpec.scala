package com.cesarla.models.api

import com.cesarla.data.Fixtures
import com.cesarla.models.{AccountId, Money}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class TransferRequestSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "TransferRequest" should {

    "serialize" in {
      val json: JsValue = Json.toJson(transferRequest)
      (json \ "target_id").as[AccountId] shouldBe transferFixture.targetId
      (json \ "money").as[Money] shouldBe transferFixture.money
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "target_id": "21315d41-9327-4787-a135-b33d4f842647",
          |   "money": {
          |     "total": "42.0000", "currency": "EUR"
          |   }
          |}
        """.stripMargin)
      json.as[TransferRequest] shouldBe transferRequest
    }
  }
}
