package com.cesarla.http

import java.time.Clock

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.{ByteString, Timeout}
import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.services.{AccountService, LedgerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag

class AccountsRoutesSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with Fixtures
    with MockFactory
    with AccountsRoutes {

  val log: LoggingAdapter = Logging(system, classOf[AccountsRoutesSpec])
  lazy val routes: Route = pathPrefix("v1") {
    accountsRoutes
  }
  override implicit lazy val timeout: Timeout = Timeout(5.seconds)
  override implicit val uuid1Generator: UUID1Generator = mock[UUID1Generator]
  override val accountService: AccountService = mock[AccountService]
  override val ledgerService: LedgerService = mock[LedgerService]
  override val clock: Clock = Clock.systemUTC()

  "AccountsRoutes" can {
    "GET an account" should {
      "get the requested account" in {
        (accountService
          .readAccount(_: AccountId))
          .expects(accountId1Fixture)
          .returning(Right(snapshotFixture))
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
          .returning(Left(problem))
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
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).once()
        (ledgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
          .once()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(true).once()
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

      "return 404 if the account does not exists" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(false).once()
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
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.NotFound(s"Account ${withdrawalFixture.accountId} does not exists"))
        }
      }

      "return 400 if the amount is negative" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(true).never()
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

      "return 422 if the payload is missing" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Deposit)(_: ClassTag[Deposit]))
          .expects(*, *)
          .returning(Future.successful(Right(depositFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(true).never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/deposits")
        request ~> routes ~> check {
          status should ===(StatusCodes.UnprocessableEntity)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.UnprocessableEntity("The request is empty, a payload is expected"))
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
          .once()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(true).once()
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

      "return 404 if the account does not exist" in {
        (() => uuid1Generator.generate()).expects().returning(withdrawalFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Withdrawal)(_: ClassTag[Withdrawal]))
          .expects(*, *)
          .returning(Future.successful(Right(withdrawalFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(withdrawalFixture.accountId).returning(false).once()
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
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.NotFound(s"Account ${withdrawalFixture.accountId} does not exists"))
        }
      }

      "return 400 if the amount is negative" in {
        (() => uuid1Generator.generate()).expects().returning(withdrawalFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Withdrawal)(_: ClassTag[Withdrawal]))
          .expects(*, *)
          .returning(Future.successful(Right(withdrawalFixture)))
          .never()
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

      "return 422 if the payload is missing" in {
        (() => uuid1Generator.generate()).expects().returning(withdrawalFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Withdrawal)(_: ClassTag[Withdrawal]))
          .expects(*, *)
          .returning(Future.successful(Right(withdrawalFixture)))
          .never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/withdrawals")
        request ~> routes ~> check {
          status should ===(StatusCodes.UnprocessableEntity)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.UnprocessableEntity("The request is empty, a payload is expected"))
        }
      }
    }
    "POST a transfer" should {
      "submit a valid transfer" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).once()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .once()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.sourceId).returning(true).once()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.targetId).returning(true).once()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(HttpEntity(
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

      "return 404 if the source account does not exist" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.sourceId).returning(false).once()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.targetId).returning(true).never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(HttpEntity(
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
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(
            Problems.NotFound(s"Source account ${transferFixture.sourceId} does not exists"))
        }
      }

      "return 400 if the target account does not exist" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.sourceId).returning(true).once()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.targetId).returning(false).once()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(HttpEntity(
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
          status should ===(StatusCodes.NotFound)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(
            Problems.NotFound(s"Target account ${transferFixture.targetId} does not exists"))
        }
      }

      "return 400 if the source_id and target_id matches" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .never()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.sourceId).returning(true).never()
        (accountService.existAccount(_: AccountId)).expects(transferFixture.targetId).returning(true).never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(HttpEntity(
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
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers").withEntity(HttpEntity(
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

      "return 422 if the payload is missing" in {
        (() => uuid1Generator.generate()).expects().returning(depositFixture.operationId.value).never()
        (ledgerService
          .dispatchOperation(_: Transfer)(_: ClassTag[Transfer]))
          .expects(*, *)
          .returning(Future.successful(Right(transferFixture)))
          .never()
        val request = Post(s"/v1/accounts/$accountId1Fixture/transfers")
        request ~> routes ~> check {
          status should ===(StatusCodes.UnprocessableEntity)
          contentType should ===(ContentTypes.`application/json`)
          responseAs[Problem] should ===(Problems.UnprocessableEntity("The request is empty, a payload is expected"))
        }
      }
    }
  }
}
