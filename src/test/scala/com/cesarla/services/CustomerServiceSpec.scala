package com.cesarla.services

import com.cesarla.data.Fixtures
import com.cesarla.models.{Customer, CustomerId, Problems}
import com.cesarla.persistence.CustomerRepository
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
        val Right(customer) = Await.result(customerService.createCustomer(customerFixture.email), 100.milliseconds)

        customer.email === ( customerFixture.email)
      }

      "return problem if email already registered" in new WithMocks {
        (mockCustomerRepository.existsCustomer(_:String)).expects(*).returning(true)
        Await.result(customerService.createCustomer(customerFixture.email), 100.milliseconds) should === (
          Left(Problems.Conflict("Email already taken")))
      }
    }


    trait WithMocks {
      val mockCustomerRepository = mock[CustomerRepository]
      val customerService = new CustomerService(mockCustomerRepository)
    }
  }

}
