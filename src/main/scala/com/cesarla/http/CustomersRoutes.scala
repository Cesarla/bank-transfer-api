package com.cesarla.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives.{as, concat, entity, get, onSuccess, pathPrefix, post, respondWithHeaders}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.cesarla.models.{Customer, CustomerId, Problem}
import com.cesarla.services.{AccountService, CustomerService, LedgerService}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import akka.http.scaladsl.server.Directives._

trait CustomersRoutes extends PlayJsonSupport {

  implicit val uuid1Generator: UUID1Generator

  implicit def system: ActorSystem

  implicit val timeout: Timeout

  val log: LoggingAdapter

  val accountService: AccountService

  val customerService: CustomerService

  val ledgerService: LedgerService

  val customerRoutes: Route = pathPrefix("customers") {
    concat(
      pathPrefix(Segment) { customerId =>
        concat(
          get {
            onSuccess(customerService.getCustomer(CustomerId.valueOf(customerId))) {
              case Right(customer) => complete((StatusCodes.OK, customer))
              case Left(problem: Problem) =>
                log.info("Customer {} failed to be fetched: {}", customerId, problem)
                complete((problem.status, problem))
            }
          },
          post {
            onSuccess(accountService.createAccount(CustomerId.valueOf(customerId), "EUR")) {
              case Right(accountId) =>
                respondWithHeaders(Location(accountId.value.toString)) {
                  complete(StatusCodes.NoContent)
                }
              case Left(problem: Problem) =>
                log.info("Account failed to be created: {}", problem)
                complete((problem.status, problem))
            }
          }
        )
      },
      post {
        entity(as[Customer]) { customer: Customer =>
          onSuccess(customerService.createCustomer(customer.email)) {
            case Right(customer: Customer) =>
              complete((StatusCodes.OK, customer))
            case Left(problem: Problem) =>
              log.info("Customer failed to be created: {}", problem)
              complete((problem.status, problem))
          }
        }
      }
    )
  }
}
