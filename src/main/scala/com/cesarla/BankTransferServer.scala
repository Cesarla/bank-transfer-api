package com.cesarla

import java.time.Clock

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{concat, pathPrefix}
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.cesarla.http._
import com.cesarla.persistence.{CustomerRepository, Ledger, OperationRepository}
import com.cesarla.services.{AccountService, CustomerService, LedgerService}
import com.fasterxml.uuid.{Generators, NoArgGenerator => UUID1Generator}

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object BankTransferServer extends App {
  Await.result(new BankServer().system.whenTerminated, Duration.Inf)
}

class BankServer() extends CustomersRoutes with AccountsRoutes with OperationsRoutes {
  implicit val system: ActorSystem = ActorSystem(s"BankTransferApi")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit override lazy val timeout: Timeout = Timeout(5.seconds)
  override lazy val log = Logging(system, classOf[BankServer])

  lazy val routes: Route =
    pathPrefix("v1") {
      concat(
        customerRoutes,
        accountsRoutes,
        operationsRoutes
      )
    }

  val ledger = new Ledger()
  val customerRepository = new CustomerRepository()

  implicit val clock: Clock = Clock.systemUTC()
  implicit override val uuid1Generator: UUID1Generator = Generators.timeBasedGenerator()
  override val ledgerService: LedgerService =
    new LedgerService(ledger, new OperationRepository(), 9)
  override val accountService: AccountService = new AccountService(customerRepository, ledgerService)
  override val customerService: CustomerService = new CustomerService(customerRepository)
  Http().bindAndHandle(routes, "localhost", 8080)
  System.out.println("Starting BankTransferApi on port 8080")
}
