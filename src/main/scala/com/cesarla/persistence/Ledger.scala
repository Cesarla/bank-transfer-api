package com.cesarla.persistence

import java.time.Instant

import com.cesarla.models.{AccountId, Money, Record, Snapshot}

import scala.collection.concurrent.TrieMap

class Ledger(log: TrieMap[AccountId, List[Record]] = TrieMap.empty) {

  def storeRecord(accountId: AccountId, record: Record): Unit = {
    log.put(accountId, log.getOrElse(accountId, List.empty[Record]) :+ record)
    ()
  }

  def getRecords(accountId: AccountId): List[Record] = {
    log.getOrElse(accountId, List.empty[Record])
  }

  def replayAccount(accountId: AccountId): (Snapshot, Int) = {
    val records = getRecords(accountId)
    val currency = records.headOption.map(_.balance.currency).getOrElse(Money.Euro)
    val snapshot = records.foldLeft(Snapshot(accountId, Money(BigDecimal(0), currency), Instant.EPOCH)) {
      case (snapshot, record) =>
        snapshot.copy(balance = snapshot.balance + record.balance, updatedAt = record.createdAt)
    }
    (snapshot, records.size)
  }
}
