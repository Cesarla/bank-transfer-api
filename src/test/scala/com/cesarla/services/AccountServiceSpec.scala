package com.cesarla.services

import java.time.Clock

import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.persistence.CustomerRepository
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

class AccountServiceSpec extends WordSpec with Matchers with PlayJsonSupport with MockFactory with Fixtures {
  "AccountService" can {

    "createAccount" should {
      "allow to add a new account to an existing customer" in new WithMocks {
        (mockCustomerRepository
          .getCustomer(_: CustomerId))
          .expects(customerIdFixture)
          .returning(Some(customerFixture))
          .once()
        (mockCustomerRepository.setCustomer(_: Customer)).expects(*).once()
        (mockLedgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
          .once()
        (() => mockTimeBasedGenerator.generate).expects().returning(accountId1Fixture.value).twice()
        val Right(accountId) =
          Await.result(accountService.createAccount(customerIdFixture, Money.Dollar), 100.milliseconds)
        accountId should ===(accountId1Fixture)
      }

      "return problem if customer does not exists" in new WithMocks {
        (mockCustomerRepository.getCustomer(_: CustomerId)).expects(*).returning(None)
        val Left(problem) = Await.result(accountService.createAccount(customerIdFixture, Money.Euro), 100.milliseconds)
        problem should ===(Problems.NotFound(s"Customer $customerIdFixture not found"))
      }

      "if the customer already has an account for that currency" in new WithMocks {
        (mockCustomerRepository.getCustomer(_: CustomerId)).expects(*).returning(Some(customerFixture))
        val Left(problem) = Await.result(accountService.createAccount(customerIdFixture, Money.Euro), 100.milliseconds)
        problem should ===(Problems.BadRequest("The customer already has an account with the given currency"))
      }
    }
  }

  trait WithMocks {
    val mockCustomerRepository: CustomerRepository = mock[CustomerRepository]
    val mockLedgerService: LedgerService = mock[LedgerService]
    val mockTimeBasedGenerator: UUID1Generator = mock[UUID1Generator]
    val mockClock: Clock = Clock.systemUTC()
    val accountService =
      new AccountService(mockCustomerRepository, mockLedgerService)(mockClock, mockTimeBasedGenerator)
  }

}
