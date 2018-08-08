package com.cesarla.data
import java.time.Instant
import java.util.UUID

import com.cesarla.models.{OperationId, _}

trait Fixtures {
  lazy val accountId1Fixture = AccountId(UUID.fromString("3983a173-b4a5-4c22-ac34-288fcc095fa7"))

  lazy val accountId2Fixture = AccountId(UUID.fromString("21315d41-9327-4787-a135-b33d4f842647"))

  lazy val customerIdFixture = CustomerId(UUID.fromString("50554d6e-29bb-11e5-b345-feff819cdc9f"))

  lazy val customerFixture: Customer = Customer(
    customerIdFixture,
    "bob@example.com",
    accounts = Map(
      "EUR" -> accountId1Fixture
    )
  )

  lazy val customerWithNoAccountsFixture: Customer = customerFixture.copy(accounts = Map.empty)

  lazy val operationIdFixture: OperationId = OperationId(UUID.fromString("3a5aaf3c-0c31-425b-ac09-d0887d3ae2ba"))

  lazy val moneyFixture = Money(
    total = 42, currency = "EUR"
  )

  lazy val transferFixture: Transfer = Transfer(
    operationIdFixture,
    accountId1Fixture,
    accountId2Fixture,
    moneyFixture,
    Instant.EPOCH,
    status = OperationStatus.Successful
  )

  lazy val depositFixture: Deposit = Deposit(
    operationIdFixture,
    accountId1Fixture,
    moneyFixture,
    Instant.EPOCH,
    status = OperationStatus.Successful
  )

  lazy val withdrawalFixture: Withdrawal = Withdrawal(
    operationIdFixture,
    accountId1Fixture,
    moneyFixture,
    Instant.EPOCH,
    status = OperationStatus.Successful
  )

  lazy val snapshotFixture: Snapshot = Snapshot(
    accountId1Fixture, moneyFixture, Instant.EPOCH
  )

  lazy val recordFixture: Record = Record(
    accountId1Fixture, moneyFixture, Instant.EPOCH, Some(operationIdFixture)
  )
}
