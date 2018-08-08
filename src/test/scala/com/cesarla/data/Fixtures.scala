package com.cesarla.data
import java.util.UUID

import com.cesarla.models.{AccountId, Customer, CustomerId}

trait Fixtures {
  lazy val accountIdFixture = AccountId(UUID.fromString("3983a173-b4a5-4c22-ac34-288fcc095fa7"))

  lazy val customerIdFixture = CustomerId(UUID.fromString("50554d6e-29bb-11e5-b345-feff819cdc9f"))

  lazy val customerFixture: Customer = Customer(
    customerIdFixture,
    "bob@example.com",
    accounts = Map(
      "EUR" -> accountIdFixture
    )
  )

  lazy val customerWithNoAccountsFixture: Customer = customerFixture.copy(accounts = Map.empty)
}
