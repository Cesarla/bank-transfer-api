package com.cesarla.models
import com.cesarla.data.Fixtures
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedGenerator
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class CustomerSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  implicit val tbg: TimeBasedGenerator = Generators.timeBasedGenerator()
  "Customer" should {
    "serialize" in {
      val json: JsValue = Json.toJson(customerFixture)
      (json \ "id").as[CustomerId] should ===(customerFixture.id)
      (json \ "email").as[String] should ===(customerFixture.email)
      (json \ "accounts").as[Map[String, AccountId]] should ===(customerFixture.accounts)
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
          |{
          |   "id": "50554d6e-29bb-11e5-b345-feff819cdc9f",
          |   "email": "bob@example.com",
          |   "accounts": {
                "EUR": "3983a173-b4a5-4c22-ac34-288fcc095fa7"
          |   }
          |}
        """.stripMargin)
      json.as[Customer] should ===(customerFixture)
    }
  }
}
