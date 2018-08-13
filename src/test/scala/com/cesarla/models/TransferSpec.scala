package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class TransferSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Transfer" should {

    "serialize" in {
      val json: JsValue = Json.toJson(transferFixture)
      (json \ "operation_id").as[OperationId] shouldBe transferFixture.operationId
      (json \ "source_id").as[AccountId] shouldBe transferFixture.sourceId
      (json \ "target_id").as[AccountId] shouldBe transferFixture.targetId
      (json \ "money").as[Money] shouldBe transferFixture.money
      (json \ "created_at").as[Instant] shouldBe transferFixture.createdAt
      (json \ "status").as[OperationStatus] shouldBe transferFixture.status
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "operation_id": "3a5aaf3c-0c31-425b-ac09-d0887d3ae2ba",
          |   "source_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
          |   "target_id": "21315d41-9327-4787-a135-b33d4f842647",
          |   "money": {
          |     "total": "42.0000", "currency": "EUR"
          |   },
          |   "created_at": "1970-01-01T00:00:00Z",
          |   "status": "SUCCESSFUL"
          |}
        """.stripMargin)
      json.as[Transfer] shouldBe transferFixture
    }
  }
}
