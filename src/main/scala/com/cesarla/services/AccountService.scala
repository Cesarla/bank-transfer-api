package com.cesarla.services

import java.time.{Clock, Instant}

import com.cesarla.models._
import com.cesarla.persistence.CustomerRepository
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}

import scala.concurrent.Future

class AccountService(customerRepository: CustomerRepository, ledgerService: LedgerService)(implicit clock: Clock,
                                                                                           tbg: UUID1Generator) {
  def readAccount(accountId: AccountId): Either[Problem, Snapshot] = ledgerService.computeBalance(accountId)

  def createAccount(customerId: CustomerId, currency: String): Future[Either[Problem, AccountId]] =
    Future.successful {
      customerRepository
        .getCustomer(customerId)
        .map { customer =>
          if (customer.accounts.contains(currency)) {
            Left(Problems.BadRequest("The customer already has an account with the given currency"))
          } else {
            val accountId = AccountId.generate
            customerRepository.setCustomer(
              customer.copy(accounts = customer.accounts + (currency -> accountId))
            )

            ledgerService.dispatchOperation(
              Deposit(OperationId.generate, accountId, Money.zero("EUR"), Instant.now(clock), OperationStatus.Progress))

            Right(accountId)
          }
        }
        .getOrElse(Left(Problems.NotFound(s"Customer $customerId not found")))
    }

  def existAccount(accountId: AccountId):Boolean = ledgerService.computeBalance(accountId).isRight
}
