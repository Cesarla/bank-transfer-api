package com.cesarla.persistence

import com.cesarla.models.{Customer, CustomerId}

import scala.collection.concurrent.TrieMap

class CustomerRepository {
  private[this] val customersById: TrieMap[CustomerId, Customer] = TrieMap.empty[CustomerId, Customer]
  private[this] val customersByEmail: TrieMap[String, Customer] = TrieMap.empty[String, Customer]

  def getCustomer(customerId: CustomerId): Option[Customer] = customersById.get(customerId)

  def existsCustomer(email: String): Boolean = customersByEmail.contains(email)

  def setCustomer(customer: Customer): Unit = {
    customersById.put(customer.id, customer)
    customersByEmail.put(customer.email, customer)
    ()
  }
}
