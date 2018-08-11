package com.cesarla.services

import com.cesarla.data.Fixtures
import com.cesarla.models.{Customer, CustomerId, Problems}
import com.cesarla.persistence.CustomerRepository
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class CustomerServiceSpec extends WordSpec with Matchers with PlayJsonSupport with MockFactory with Fixtures {
  "CustomerService" can {
    "getCustomer" should {
      "return customer if present" in new WithMocks {
        (mockCustomerRepository.getCustomer(_:CustomerId)).expects(*).returning(Some(customerFixture))
        Await.result(customerService.getCustomer(customerIdFixture), 100.milliseconds) should === (
          Right(customerFixture))
      }

      "return problem if not present" in new WithMocks {
        (mockCustomerRepository.getCustomer(_:CustomerId)).expects(*).returning(None)
        Await.result(customerService.getCustomer(customerIdFixture), 100.milliseconds) should === (
          Left(Problems.NotFound(s"Customer $customerIdFixture not found")))
      }
    }

    "createCustomer" should {
      "if customer freshly registered" in new WithMocks {
        (mockCustomerRepository.existsCustomer(_:String)).expects(*).returning(false)
        (mockCustomerRepository.setCustomer(_:Customer)).expects(*)
        (() => mockTimeBasedGenerator.generate).expects().returning(customerIdFixture.value)
        val Right(customer) = Await.result(customerService.createCustomer(customerFixture.email), 100.milliseconds)

        customer should === (Customer(customerIdFixture, customerFixture.email))
      }

      "return problem if email already registered" in new WithMocks {
        (mockCustomerRepository.existsCustomer(_:String)).expects(*).returning(true)
        Await.result(customerService.createCustomer(customerFixture.email), 100.milliseconds) should === (
          Left(Problems.Conflict("Email already taken")))
      }
    }

    trait WithMocks {
      val mockCustomerRepository: CustomerRepository = mock[CustomerRepository]
      val mockTimeBasedGenerator: UUID1Generator = mock[UUID1Generator]
      val customerService = new CustomerService(mockCustomerRepository)(mockTimeBasedGenerator)
    }
  }

}
