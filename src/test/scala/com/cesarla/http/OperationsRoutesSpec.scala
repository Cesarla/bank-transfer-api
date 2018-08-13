package com.cesarla.http

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.Timeout
import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.services.LedgerService
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.duration._
import scala.reflect.ClassTag

class OperationsRoutesSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with Fixtures
    with MockFactory
    with OperationsRoutes {

  val log: LoggingAdapter = Logging(system, classOf[OperationsRoutesSpec])
  lazy val routes: Route = pathPrefix("v1") {
    operationsRoutes
  }
  override implicit lazy val timeout: Timeout = Timeout(5.seconds)
  override implicit val uuid1Generator: UUID1Generator = mock[UUID1Generator]
  override val ledgerService: LedgerService = mock[LedgerService]

  "OperationsRoutes" when {
    "GET a operation" should {
      "return the deposit if present" in {
        (ledgerService.get(_:OperationId)(_:ClassTag[Deposit])).expects(*,*).returning(Right(depositFixture)).once()
        val request = Get(s"/v1/operations/${depositFixture.operationId}")
        request ~> routes ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Deposit] shouldBe depositFixture
        }
      }

      "return the withdrawal if present" in {
        (ledgerService.get(_:OperationId)(_:ClassTag[Withdrawal])).expects(*,*).returning(Right(withdrawalFixture)).once()
        val request = Get(s"/v1/operations/${withdrawalFixture.operationId}")
        request ~> routes ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Withdrawal] shouldBe withdrawalFixture
        }
      }

      "return the transfer if present" in {
        (ledgerService.get(_:OperationId)(_:ClassTag[Transfer])).expects(*,*).returning(Right(transferFixture)).once()
        val request = Get(s"/v1/operations/${transferFixture.operationId}")
        request ~> routes ~> check {
          status shouldBe StatusCodes.OK
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Transfer] shouldBe transferFixture
        }
      }

      "return 404 if the operation does not exist" in {
        val problem = Problems.NotFound(s"Operation ${transferFixture.operationId} not found")
        (ledgerService.get(_:OperationId)(_:ClassTag[Transfer])).expects(*,*).returning(Left(problem)).once()
        val request = Get(s"/v1/operations/${transferFixture.operationId}")
        request ~> routes ~> check {
          status shouldBe StatusCodes.NotFound
          contentType shouldBe ContentTypes.`application/json`
          responseAs[Problem] shouldBe problem
        }
      }
    }
  }
}
