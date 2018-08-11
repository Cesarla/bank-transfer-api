package com.cesarla.models
import java.time.Instant

import com.cesarla.data.Fixtures
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json.{JsValue, Json}

class SnapshotSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Snapshot" should {
    "serialize" in {
      val json: JsValue = Json.toJson(snapshotFixture)
      (json \ "account_id").as[AccountId] should ===(snapshotFixture.accountId)
      (json \ "balance").as[Money] should ===(snapshotFixture.balance)
      (json \ "updated_at").as[Instant] should ===(snapshotFixture.updatedAt)
    }
  }
}
