package com.cesarla.http

import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.{ByteString, Timeout}
import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.services.{AccountService, CustomerService}
import com.fasterxml.uuid.{NoArgGenerator => UUID1Generator}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Future
import scala.concurrent.duration._

class CustomersRoutesSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with Fixtures
    with MockFactory
    with CustomersRoutes {

  val log: LoggingAdapter = Logging(system, classOf[CustomersRoutesSpec])
  lazy val routes: Route = pathPrefix("v1") {
    customerRoutes
  }
  override implicit lazy val timeout: Timeout = Timeout(5.seconds)
  override implicit val uuid1Generator: UUID1Generator = mock[UUID1Generator]
  override val accountService: AccountService = mock[AccountService]
  override val customerService: CustomerService = mock[CustomerService]

  "CustomersRoutes" when {
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
          status should ===(StatusCodes.Created)
          status should ===(StatusCodes.Created)
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
  }
}
