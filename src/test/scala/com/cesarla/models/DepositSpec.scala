package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class DepositSpec  extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Deposit" should {
    "serialize" in {
      val json: JsValue = Json.toJson(depositFixture)
      (json \ "operation_id").as[OperationId] should === (depositFixture.operationId)
      (json \ "account_id").as[AccountId] should === (depositFixture.accountId)
      (json \ "money").as[Money] should === (depositFixture.money)
      (json \ "created_at").as[Instant] should === (depositFixture.createdAt)
      (json \ "status").as[OperationStatus] should === (depositFixture.status)
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
          |   "created_at": "1970-01-01T00:00:00Z",
          |   "status": "SUCCESSFUL"
          |}
        """.stripMargin)
      json.as[Deposit] should === (depositFixture)
    }
  }
}