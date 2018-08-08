package com.cesarla.persistence
import java.time.Instant

import com.cesarla.data.Fixtures
import com.cesarla.models.{AccountId, Money, Record, Snapshot}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.{Matchers, WordSpec}

import scala.collection.concurrent.TrieMap

class LedgerSpec extends WordSpec with Matchers with PlayJsonSupport with Fixtures {
  "Ledger" can {
    "storeRecord" should {
      "with records stored" in {
        val log = TrieMap(accountId1Fixture->List(recordFixture))
        val ledger = new Ledger(log)
        ledger.storeRecord(accountId1Fixture, recordFixture)
        log.getOrElse(accountId1Fixture, TrieMap.empty[AccountId, List[Record]]).size should ===(2)
      }

      "with no records stored" in {
        val log = TrieMap.empty[AccountId, List[Record]]
        val ledger = new Ledger(log)
        ledger.storeRecord(accountId1Fixture, recordFixture)
        log.getOrElse(accountId1Fixture, TrieMap.empty[AccountId, List[Record]]).size should ===(1)
      }
    }

    "getRecords" should {
      "with records stored" in {
        val ledger = new Ledger(TrieMap(accountId1Fixture->List(recordFixture)))
        ledger.getRecords(accountId1Fixture).isEmpty should ===(false)
      }

      "with no records stored" in {
        val ledger = new Ledger(TrieMap.empty[AccountId, List[Record]])
        ledger.getRecords(accountId1Fixture).isEmpty should ===(true)
      }
    }

    "replayAccount" should {
      "with records stored" in {
        val ledger = new Ledger(TrieMap(accountId1Fixture->List(recordFixture)))
        ledger.replayAccount(accountId1Fixture) should ===(Snapshot(accountId1Fixture, recordFixture.balance, Instant.EPOCH))
      }

      "with no records stored" in {
        val ledger = new Ledger(TrieMap.empty[AccountId, List[Record]])
        ledger.replayAccount(accountId1Fixture) should ===(Snapshot(accountId1Fixture, Money(0, "EUR"), Instant.EPOCH))
      }
    }
  }
}
