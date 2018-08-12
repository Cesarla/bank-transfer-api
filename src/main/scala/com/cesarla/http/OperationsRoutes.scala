package com.cesarla.http
import java.util.UUID

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.PathDirectives.path
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.util.Timeout
import com.cesarla.models._
import com.cesarla.services.LedgerService
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.Future

trait OperationsRoutes extends PlayJsonSupport {

  implicit val uuid1Generator: UUID1Generator

  implicit def system: ActorSystem

  implicit val timeout: Timeout

  val log: LoggingAdapter

  val ledgerService: LedgerService

  val operationsRoutes: Route = pathPrefix("operations") {
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
}
