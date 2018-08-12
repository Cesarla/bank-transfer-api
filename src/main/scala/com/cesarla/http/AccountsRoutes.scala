package com.cesarla.http

import java.time.Instant

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{pathPrefix, _}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.cesarla.models.{Withdrawal, _}
import com.cesarla.services.{AccountService, CustomerService, LedgerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.Future

trait AccountsRoutes extends PlayJsonSupport {

  implicit val uuid1Generator: UUID1Generator

  implicit def system: ActorSystem

  implicit val timeout: Timeout

  val log: LoggingAdapter

  val accountService: AccountService

  val customerService: CustomerService

  val ledgerService: LedgerService

  val accountsRoutes: Route = pathPrefix("accounts") {
    pathPrefix(JavaUUID) { uuid =>
      val accountId = AccountId(uuid)
      concat(
        get {
          onSuccess(Future.successful(accountService.readAccount(accountId))) {
            case Right(snapshot) =>
              complete((StatusCodes.OK, snapshot))
            case Left(problem: Problem) =>
              log.info("Account {} failed to be fetched: {}", uuid, problem)
              complete((problem.status, problem))
          }
        },
        post {
          concat(
            pathPrefix("transfers") {
              entity(as[Transfer]) {
                transfer: Transfer =>
                  if (transfer.money.total <= BigDecimal.valueOf(0)) {
                    complete(Problems.BadRequest("Transfer must be positive").asResult)
                  } else if (transfer.targetId == transfer.sourceId) {
                    complete(Problems.BadRequest("Transfers sourceId and targetId must be different").asResult)
                  } else if (!accountService.existAccount(transfer.sourceId)) {
                    complete(Problems.NotFound(s"Source account ${transfer.sourceId} does not exists").asResult)
                  } else if (!accountService.existAccount(transfer.targetId)) {
                    complete(Problems.NotFound(s"Target account ${transfer.targetId} does not exists").asResult)
                  } else {
                    onSuccess(ledgerService.dispatchOperation(transfer)) {
                      case Right(transfer) => complete((StatusCodes.Accepted, transfer))
                      case Left(problem) =>
                        log.info("Transfer failed to be created: {}", problem)
                        complete(problem.asResult)
                    }
                  }
              }
            },
            pathPrefix("deposits") {
              entity(as[Money]) {
                money: Money =>
                  if (money.total <= BigDecimal.valueOf(0)) {
                    complete(Problems.BadRequest("Deposits must be positive").asResult)
                  } else if (!accountService.existAccount(accountId)) {
                    complete(Problems.NotFound(s"Account $accountId does not exists").asResult)
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
            },
            pathPrefix("withdrawals") {
              entity(as[Money]) {
                money: Money =>
                  if (money.total <= BigDecimal.valueOf(0)) {
                    complete(Problems.BadRequest("Withdrawals must be positive").asResult)
                  } else if (!accountService.existAccount(accountId)) {
                    complete(Problems.NotFound(s"Account $accountId does not exists").asResult)
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
          )
        }
      )
    }
  }
}
