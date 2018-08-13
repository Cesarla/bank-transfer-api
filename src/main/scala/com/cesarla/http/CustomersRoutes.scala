package com.cesarla.http

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.cesarla.models.api.AccountRequest
import com.cesarla.models.{Customer, CustomerId, Problem, Problems}
import com.cesarla.services.{AccountService, CustomerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

trait CustomersRoutes extends PlayJsonSupport {

  implicit val uuid1Generator: UUID1Generator

  implicit def system: ActorSystem

  implicit val timeout: Timeout

  val log: LoggingAdapter

  val accountService: AccountService

  val customerService: CustomerService

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
            entity(as[Option[AccountRequest]]) {
              case Some(accountRequest) =>
                onSuccess(accountService.createAccount(CustomerId.valueOf(customerId), accountRequest.currency)) {
                  case Right(accountId) =>
                    extractHost { host =>
                      respondWithHeaders(Location(s"http://$host:8080/v1/accounts/${accountId.value}")) {
                        complete(StatusCodes.Created)
                      }
                    }
                  case Left(problem: Problem) =>
                    log.info("Account failed to be created: {}", problem)
                    complete((problem.status, problem))
                }
              case _ => complete(Problems.UnprocessableEntity("The request is empty, a payload is expected").asResult)
            }
          }
        )
      },
      post {
        entity(as[Option[Customer]]) {
          case Some(customer) =>
            onSuccess(customerService.createCustomer(customer.email)) {
              case Right(customer: Customer) =>
                complete((StatusCodes.OK, customer))
              case Left(problem: Problem) =>
                log.info("Customer failed to be created: {}", problem)
                complete((problem.status, problem))
            }
          case _ => complete(Problems.UnprocessableEntity("The request is empty, a payload is expected").asResult)
        }
      }
    )
  }
}
