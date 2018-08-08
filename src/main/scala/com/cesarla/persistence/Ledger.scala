package com.cesarla.persistence

import java.time.Instant

import com.cesarla.models.{AccountId, Money, Record, Snapshot}

import scala.collection.concurrent.TrieMap

class Ledger(log: TrieMap[AccountId, List[Record]] = TrieMap.empty) {

  def storeRecord(accountId: AccountId, record: Record): Option[List[Record]] = {
    log.put(accountId, log.getOrElse(accountId, List.empty[Record]) :+ record)
  }

  def getRecords(accountId: AccountId): List[Record] = {
    log.getOrElse(accountId, List.empty[Record])
  }

  def replayAccount(accountId: AccountId): Snapshot = {
    val records = getRecords(accountId)
    val currency = records.headOption.map(_.balance.currency).getOrElse(Money.Euro)
    records.foldLeft(Snapshot(accountId, Money(BigDecimal(0), currency), Instant.EPOCH)) {
      case (snapshot, record) =>
        snapshot.copy(balance = snapshot.balance + record.balance, updatedAt = record.createdAt)
    }
  }
}
