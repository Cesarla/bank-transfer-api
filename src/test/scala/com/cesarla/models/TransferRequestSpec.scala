package com.cesarla.models

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class TransferRequestSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "TransferRequest" should {

    "serialize" in {
      val json: JsValue = Json.toJson(transferRequest)
      (json \ "target_id").as[AccountId] should ===(transferFixture.targetId)
      (json \ "money").as[Money] should ===(transferFixture.money)
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
      json.as[TransferRequest] should ===(transferRequest)
    }
  }
}
