package com.cesarla.services

import com.cesarla.models.{Customer, CustomerId, Problem, Problems}
import com.cesarla.persistence.CustomerRepository

import scala.concurrent.Future

class CustomerService(customerRepository: CustomerRepository) {
  def getCustomer(customerId: CustomerId): Future[Either[Problem, Customer]] =
    Future.successful {
      customerRepository.getCustomer(customerId) match {
        case Some(customer) => Right(customer)
        case _              => Left(Problems.NotFound(s"Customer $customerId not found"))
      }
    }

  def createCustomer(email: String): Future[Either[Problem, Customer]] =
    Future.successful {
      if (customerRepository.existsCustomer(email)) {
        Left(Problems.Conflict("Email already taken"))
      } else {
        val customer = Customer(CustomerId.generate, email)
        customerRepository.setCustomer(customer)
        Right(customer)
      }
    }

  def updateCustomer(customer: Customer): Future[Either[Problem, Unit]] = Future.successful {
    Right(customerRepository.setCustomer(customer))
  }
}
