package com.cesarla.models
import akka.http.scaladsl.model.{StatusCode, StatusCodes}
import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}
import com.cesarla.models.Problem.statusFormats

class ProblemSpec  extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Problem" should {
    "serialize" in {
      val json: JsValue = Json.toJson(problemFixture)
      (json \ "status").as[StatusCode] shouldBe StatusCodes.InternalServerError
      (json \ "reason").as[String] shouldBe "Something went wrong"
    }

    "deserialize" in {
      val json: JsValue = Json.parse("""
                                       |{
                                       |  "status": 500,
                                       |  "reason": "Something went wrong"
                                       |}
                                     """.stripMargin)
      json.as[Problem] shouldBe problemFixture
    }
  }
}