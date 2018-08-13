package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class RecordSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Record" should {
    "serialize" in {
      val json: JsValue = Json.toJson(recordFixture)
      (json \ "account_id").as[AccountId] shouldBe recordFixture.accountId
      (json \ "balance").as[Money] shouldBe recordFixture.balance
      (json \ "created_at").as[Instant] shouldBe recordFixture.createdAt
      (json \ "operation_id").asOpt[OperationId] shouldBe recordFixture.operationId
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "account_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
          |   "balance": {
          |     "total": "42.0000", "currency": "EUR"
          |   },
          |   "created_at": "1970-01-01T00:00:00Z",
          |   "operation_id": "3a5aaf3c-0c31-425b-ac09-d0887d3ae2ba"
          |}
        """.stripMargin)
      json.as[Record] shouldBe recordFixture
    }
  }
}
