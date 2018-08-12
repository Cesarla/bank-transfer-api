package com.cesarla.http

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.services.{AccountService, CustomerService, LedgerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.reflect.ClassTag

class BankRoutesSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with Fixtures
    with MockFactory
    with BankRoutes {

  lazy val routes: Route = bankTransferRoutes
  override implicit val uuid1Generator: UUID1Generator = mock[UUID1Generator]
  override val accountService: AccountService = mock[AccountService]
  override val customerService: CustomerService = mock[CustomerService]
  override val ledgerService: LedgerService = mock[LedgerService]

  "BankRoutesSpec" when {
    "GET a customer" should {
      "return the customer if present" in {
        (() => uuid1Generator.generate()).expects().returning(customerIdFixture.value).once()
        (customerService
          .getCustomer(_: CustomerId))
          .expects(customerIdFixture)
          .returning(Future.successful(Right(customerFixture)))
          .once()
        val request = Get(s"/v1/customers/$customerIdFixture")
        request ~> routes ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Customer] should ===(customerFixture)
        }
      }

      "return 404 if no present" in {
        val problem = Problems.NotFound("Not Found")
        (customerService
          .getCustomer(_: CustomerId))
          .expects(customerIdFixture)
          .returning(Future.successful(Left(problem)))
          .once()
        val request = Get(s"/v1/customers/$customerIdFixture")
        request ~> routes ~> check {
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(problem)
        }
      }
    }

    "POST a customer" should {
      "create a customer" in {
        (() => uuid1Generator.generate()).expects().returning(customerIdFixture.value).twice()
        (customerService
          .createCustomer(_: String))
          .expects(*)
          .returning(Future.successful(Right(customerFixture)))
          .once()
        val request = Post("/v1/customers").withEntity(
          HttpEntity(MediaTypes.`application/json`,
                     ByteString("""
            |{
            |  "email": "bob@example.com"
            |}
          """.stripMargin)))
        request ~> routes ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Customer] should ===(customerFixture)
        }
      }
      "returning 409 if the email is already in use" in {
        val problem = Problems.Conflict("Email already taken")
        (() => uuid1Generator.generate()).expects().returning(customerIdFixture.value)
        (customerService.createCustomer(_: String)).expects(*).returning(Future.successful(Left(problem))).once()
        val request = Post("/v1/customers/").withEntity(
          HttpEntity(
            MediaTypes.`application/json`,
            ByteString("""
                         |{
                         |  "email": "bob@example.com"
                         |}
                       """.stripMargin)
          ))
        request ~> routes ~> check {
          status should ===(StatusCodes.Conflict)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(problem)
        }
      }
    }

    "POST an account" should {
      "create a new  account" in {
        (accountService
          .createAccount(_: CustomerId, _: String))
          .expects(*, *)
          .returning(Future.successful(Right(accountId1Fixture)))
          .once()
        val request = Post(s"/v1/customers/$customerIdFixture/accounts")
        request ~> routes ~> check {
          status should ===(StatusCodes.NoContent)
        }
      }
      "returning 409 if the email is already in use" in {
        val problem = Problems.Conflict("Email already taken")
        (accountService
          .createAccount(_: CustomerId, _: String))
          .expects(*, *)
          .returning(Future.successful(Left(problem)))
          .once()
        val request = Post(s"/v1/customers/$customerIdFixture/accounts")
        request ~> routes ~> check {
          status should ===(StatusCodes.Conflict)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(problem)
        }
      }
    }

    "GET an account" should {
      "get the requested account" in {
        (accountService
          .readAccount(_: AccountId))
          .expects(accountId1Fixture)
          .returning(Future.successful(Right(snapshotFixture)))
          .once()
        val request = Get(s"/v1/accounts/$accountId1Fixture")
        request ~> routes ~> check {
          status should ===(StatusCodes.OK)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Snapshot] should ===(snapshotFixture)
        }
      }
      "returning 404 if the customer already has an account with the give currency" in {
        val problem = Problems.NotFound(s"Account $accountId1Fixture not found")
        (accountService
          .readAccount(_: AccountId))
          .expects(accountId1Fixture)
          .returning(Future.successful(Left(problem)))
          .once()
        val request = Get(s"/v1/accounts/$accountId1Fixture")
        request ~> routes ~> check {
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(problem)
        }
      }
    }

    "POST a deposit" should {
      "handle a valid deposit" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).twice()
        (ledgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
        val request = Post(s"/v1/accounts/$accountId1Fixture/deposits").withEntity(HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                         |{
                         |  "total": "42.0000",
                         |  "currency": "EUR"
                         |}
                       """.stripMargin)
        ))
        request ~> routes ~> check {
          status should ===(StatusCodes.Accepted)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Deposit] should ===(depositFixture)
        }
      }

      "return 400 if the amount is negative" in {
        val request = Post(s"/v1/accounts/$accountId1Fixture/deposits").withEntity(HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                         |{
                         |  "total": "-42.0000",
                         |  "currency": "EUR"
                         |}
                       """.stripMargin)
        ))
        request ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.BadRequest("Deposits must be positive"))
        }
      }
    }

    "POST a withdrawal" should {
      "submit a valid withdrawal" in {
        (() => uuid1Generator.generate()).expects().returning(withdrawalFixture.operationId.value).once()
        (ledgerService
          .dispatchOperation(_: Withdrawal)(_: ClassTag[Withdrawal]))
          .expects(*, *)
          .returning(Future.successful(Right(withdrawalFixture)))
        val request = Post(s"/v1/accounts/$accountId1Fixture/withdrawals").withEntity(HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                         |{
                         |  "total": "42.0000",
                         |  "currency": "EUR"
                         |}
                       """.stripMargin)
        ))

        request ~> routes ~> check {
          status should ===(StatusCodes.Accepted)
          contentType should ===(ContentTypes.`application/json`)
        }
      }

      "return 400 if the amount is negative" in {
        val request = Post(s"/v1/accounts/$accountId1Fixture/withdrawals").withEntity(HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                         |{
                         |  "total": "-42.0000",
                         |  "currency": "EUR"
                         |}
                       """.stripMargin)
        ))
        request ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.BadRequest("Withdrawals must be positive"))
        }
      }
    }
  }

  "POST a transfer" should {
    "submit a valid transfer" in {
      (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).twice()
      (ledgerService
        .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
        .expects(*, *)
        .returning(Future.successful(Right(transferFixture)))
        .once()
      val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(
        HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                     |{
                     |  "source_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
                     |  "target_id": "21315d41-9327-4787-a135-b33d4f842647",
                     |  "money": {
                     |    "total": "42.0000",
                     |    "currency": "EUR"
                     |   }
                     |}
                   """.stripMargin)
        ))
      request ~> routes ~> check {
        status should ===(StatusCodes.Accepted)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[Transfer] should ===(transferFixture)
      }
    }

    "return 400 if the source_id and target_id matches" in {
      (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).once()
      (ledgerService
        .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
        .expects(*, *)
        .returning(Future.successful(Right(transferFixture)))
        .never()
      val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(
        HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                     |{
                     |  "source_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
                     |  "target_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
                     |  "money": {
                     |    "total": "42.0000",
                     |    "currency": "EUR"
                     |   }
                     |}
                   """.stripMargin)
        ))
      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[Problem] should ===(Problems.BadRequest("Transfers sourceId and targetId must be different"))
      }
    }

    "return 400 if the amount is negative" in {
      (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).once()
      (ledgerService
        .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
        .expects(*, *)
        .returning(Future.successful(Right(transferFixture)))
        .never()
      val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(
        HttpEntity(
          MediaTypes.`application/json`,
          ByteString("""
                     |{
                     |  "source_id": "3983a173-b4a5-4c22-ac34-288fcc095fa7",
                     |  "target_id": "21315d41-9327-4787-a135-b33d4f842647",
                     |  "money": {
                     |    "total": "-42.0000",
                     |    "currency": "EUR"
                     |   }
                     |}
                   """.stripMargin)
        ))
      request ~> routes ~> check {
        status should ===(StatusCodes.BadRequest)
        contentType should ===(ContentTypes.`application/json`)
        responseAs[Problem] should ===(Problems.BadRequest("Transfer must be positive"))
      }
    }
  }
}
