package com.cesarla.http

import java.time.Instant
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.cesarla.models.{Withdrawal, _}
import com.cesarla.services.{AccountService, CustomerService, LedgerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.Future
import scala.concurrent.duration._

trait BankRoutes extends PlayJsonSupport {

  implicit val uuid1Generator: UUID1Generator

  implicit def system: ActorSystem

  implicit lazy val timeout: Timeout = Timeout(5.seconds)

  lazy val log = Logging(system, classOf[BankRoutes])

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

  val accountsRoutes: Route = pathPrefix("accounts") {
    pathPrefix(JavaUUID) { uuid =>
      val accountId = AccountId(uuid)
      concat(
        get {
          onSuccess(accountService.readAccount(accountId)) {
            case Right(snapshot) =>
              complete((StatusCodes.OK, snapshot))
            case Left(problem: Problem) =>
              log.info("Account {} failed to be fetched: {}", uuid, problem)
              complete((problem.status, problem))
          }
        },
        post {
          pathPrefix("transfers") {
            entity(as[Transfer]) {
              transfer: Transfer =>
                if (transfer.money.total <= BigDecimal.valueOf(0)) {
                  complete(Problems.BadRequest("Transfer must be positive").asResult)
                } else if (transfer.targetId == transfer.sourceId) {
                  complete(Problems.BadRequest("Transfers sourceId and targetId must be different").asResult)
                } else {
                  onSuccess(ledgerService.dispatchOperation(transfer)) {
                    case Right(transfer) => complete((StatusCodes.Accepted, transfer))
                    case Left(problem) =>
                      log.info("Transfer failed to be created: {}", problem)
                      complete(problem.asResult)
                  }
                }
            }
          } ~
            pathPrefix("deposits") {
              entity(as[Money]) {
                money: Money =>
                  if (money.total <= BigDecimal.valueOf(0)) {
                    complete(Problems.BadRequest("Deposits must be positive").asResult)
                  } else {
                    onSuccess(
                      ledgerService.dispatchOperation(
                        Deposit(OperationId.generate, accountId, money, Instant.now(), OperationStatus.Progress)
                      )) {
                      case Right(deposit) => complete((StatusCodes.Accepted, deposit))
                      case Left(problem) =>
                        log.info("Deposit failed to be created: {}", problem)
                        complete(problem.asResult)
                    }
                  }
              }
            } ~
            pathPrefix("withdrawals") {
              entity(as[Money]) {
                money: Money =>
                  if (money.total <= BigDecimal.valueOf(0)) {
                    complete(Problems.BadRequest("Withdrawals must be positive").asResult)
                  } else {
                    onSuccess(
                      ledgerService.dispatchOperation(
                        Withdrawal(OperationId.generate, accountId, money, Instant.now(), OperationStatus.Progress)
                      )) {
                      case Right(withdrawal) => complete((StatusCodes.Accepted, withdrawal))
                      case Left(problem: Problem) =>
                        log.info("Withdrawal failed to be created: {}", problem)
                        complete(problem.asResult)
                    }
                  }
              }
            }
        }
      )
    }
  }

  val transferRoutes: Route = pathPrefix("operations") {
    concat(
      path(Segment) { operationId =>
        get {
          onSuccess(Future.successful(ledgerService.get[Operation](OperationId(UUID.fromString(operationId))))) {
            case Right(transfer: Deposit)    => complete((StatusCodes.OK, transfer))
            case Right(transfer: Withdrawal) => complete((StatusCodes.OK, transfer))
            case Right(transfer: Transfer)   => complete((StatusCodes.OK, transfer))
            case Left(problem: Problem) =>
              log.info("Operation {} failed to be fetched: {}", operationId, problem)
              complete((problem.status, problem))
          }
        }
      }
    )
  }

  lazy val bankTransferRoutes: Route =
    pathPrefix("v1") {
      concat(
        customerRoutes,
        accountsRoutes,
        transferRoutes
      )
    }
}
