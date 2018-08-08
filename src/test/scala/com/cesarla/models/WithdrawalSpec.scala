package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class WithdrawalSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Deposit" should {
    "serialize" in {
      val json: JsValue = Json.toJson(withdrawalFixture)
      (json \ "operation_id").as[OperationId] should === (withdrawalFixture.operationId)
      (json \ "account_id").as[AccountId] should === (withdrawalFixture.accountId)
      (json \ "money").as[Money] should === (withdrawalFixture.money)
      (json \ "create_at").as[Instant] should === (withdrawalFixture.createAt)
      (json \ "status").as[OperationStatus] should === (withdrawalFixture.status)
    }

    "deserialize" in {
      val json: JsValue = Json.parse(
        """
          |{
          |   "operation_id": "3a5aaf3c-0c31-425b-ac09-d0887d3ae2ba",
          |   "account_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
          |   "money": {
          |     "total": "42", "currency": "EUR"
          |   },
          |   "create_at": "1970-01-01T00:00:00Z",
          |   "status": "SUCCESSFUL"
          |}
        """.stripMargin)
      json.as[Withdrawal] should === (withdrawalFixture)
    }
  }
}