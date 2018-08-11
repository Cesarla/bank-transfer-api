package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class TransferSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  implicit val tbg: TimeBasedGenerator = Generators.timeBasedGenerator()
  "Transfer" should {

    "serialize" in {
      val json: JsValue = Json.toJson(transferFixture)
      (json \ "operation_id").as[OperationId] should ===(transferFixture.operationId)
      (json \ "source_id").as[AccountId] should ===(transferFixture.sourceId)
      (json \ "target_id").as[AccountId] should ===(transferFixture.targetId)
      (json \ "money").as[Money] should ===(transferFixture.money)
      (json \ "created_at").as[Instant] should ===(transferFixture.createdAt)
      (json \ "status").as[OperationStatus] should ===(transferFixture.status)
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
      json.as[Transfer] should ===(transferFixture)
    }
  }
}
