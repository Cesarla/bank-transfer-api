package com.cesarla.models
import java.time.{Clock, Instant}

import com.cesarla.data.Fixtures
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class WithdrawalSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  implicit val clock: Clock = Clock.systemUTC()
  implicit val tbg: TimeBasedGenerator = Generators.timeBasedGenerator()
  "Deposit" should {
    "serialize" in {
      val json: JsValue = Json.toJson(withdrawalFixture)
      (json \ "operation_id").as[OperationId] shouldBe withdrawalFixture.operationId
      (json \ "account_id").as[AccountId] shouldBe withdrawalFixture.accountId
      (json \ "money").as[Money] shouldBe withdrawalFixture.money
      (json \ "created_at").as[Instant] shouldBe withdrawalFixture.createdAt
      (json \ "status").as[OperationStatus] shouldBe withdrawalFixture.status
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "operation_id": "3a5aaf3c-0c31-425b-ac09-d0887d3ae2ba",
          |   "account_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
          |   "money": {
          |     "total": "42.0000", "currency": "EUR"
          |   },
          |   "created_at": "1970-01-01T00:00:00Z",
          |   "status": "SUCCESSFUL"
          |}
        """.stripMargin)
      json.as[Withdrawal] shouldBe withdrawalFixture
    }
  }
}
