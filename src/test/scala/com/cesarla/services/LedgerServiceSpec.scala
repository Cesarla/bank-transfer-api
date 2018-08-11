package com.cesarla.services
import akka.http.scaladsl.model.StatusCodes
import com.cesarla.data.Fixtures
import com.cesarla.models._
import com.cesarla.persistence.{Ledger, OperationRepository}
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Span}
import org.scalatest.{Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._
class LedgerServiceSpec extends WordSpec with Matchers with Eventually with MockFactory with Fixtures {
  implicit val patientConfig = PatienceConfig(timeout = scaled(1.second), interval= scaled(Span(200, Millis)))
  "LedgerService" can {
    "dispatch" should {
      "valid deposits" in new WithStubs {
        val Right(_) =
          Await.result(ledgerService.dispatchOperation(depositFixture.copy(status = OperationStatus.Progress)),
            150.milliseconds)
        eventually {
          val Right(deposit: Deposit) = ledgerService.get[Deposit](depositFixture.operationId)
          deposit.status should ===(OperationStatus.Successful)
        }
      }

      "valid withdrawal" in new WithStubs {
        (mockLedger.replayAccount _).when(*).returns(snapshotFixture)
        val Right(_) =
          Await.result(ledgerService.dispatchOperation(withdrawalFixture.copy(status = OperationStatus.Progress)),
            150.milliseconds)

        eventually {
          val Right(withdrawal: Withdrawal) = ledgerService.get[Withdrawal](withdrawalFixture.operationId)
          withdrawal.status should ===(OperationStatus.Successful)
        }
      }

      "set withdrawal as failed if there is not enough founds" in new WithStubs {
        (mockLedger.replayAccount _).when(*).returns(snapshotFixture)
        val Right(_) =
          Await.result(ledgerService.dispatchOperation(
                         withdrawalFixture.copy(money = Money(100, "EUR"), status = OperationStatus.Progress)),
            150.milliseconds)
        eventually {
          val Right(withdrawal: Withdrawal) = ledgerService.get[Withdrawal](withdrawalFixture.operationId)
          withdrawal.status should ===(OperationStatus.Failed)
          withdrawal.detail should ===(Some("Not enough founds"))
        }
      }

      "valid transfers" in new WithStubs {
        (mockLedger.replayAccount _).when(*).returns(snapshotFixture)
        val Right(_) =
          Await.result(ledgerService.dispatchOperation(transferFixture.copy(status = OperationStatus.Progress)),
            150.milliseconds)
        eventually {
          val Right(transfer: Transfer) = ledgerService.get[Transfer](transferFixture.operationId)
          transfer.status should ===(OperationStatus.Successful)
        }
      }

      "set transfer as failed if there is not enough founds" in new WithStubs {
        (mockLedger.replayAccount _).when(*).returns(snapshotFixture)
        val Right(_) =
          Await.result(ledgerService.dispatchOperation(
                         transferFixture.copy(money = Money(100, "EUR"), status = OperationStatus.Progress)),
            150.milliseconds)
        eventually {
          val Right(transfer: Transfer) = ledgerService.get[Transfer](transferFixture.operationId)
          transfer.status should ===(OperationStatus.Failed)
          transfer.detail should ===(Some("Not enough founds"))
        }
      }
    }

    "get operation" should {
      "return a deposit if present" in new WithMocks {
        (mockOperationRepository.getOperation(_: OperationId)).expects(*).returning(Some(depositFixture))
        val Right(deposit: Deposit) = ledgerService.get[Deposit](operationIdFixture)
        deposit should ===(depositFixture)
      }

      "return a withdrawal if present" in new WithMocks {
        (mockOperationRepository.getOperation(_: OperationId)).expects(*).returning(Some(withdrawalFixture))
        val Right(withdrawal: Withdrawal) = ledgerService.get[Withdrawal](operationIdFixture)
        withdrawal should ===(withdrawalFixture)
      }

      "return a transfer if present" in new WithMocks {
        (mockOperationRepository.getOperation(_: OperationId)).expects(*).returning(Some(transferFixture))
        val Right(transfer: Transfer) = ledgerService.get[Transfer](operationIdFixture)
        transfer should ===(transferFixture)
      }

      "return problem if not present" in new WithMocks {
        (mockOperationRepository.getOperation(_: OperationId)).expects(*).returning(None)
        val Left(problem) = ledgerService.get[Transfer](operationIdFixture)
        problem.status should ===(StatusCodes.NotFound)
      }
    }

    "computeBalance" should {
      "with records" in new WithMocks {
        (mockLedger.getRecords(_: AccountId)).expects(*).returning(List(recordFixture)).once()
        (mockLedger.replayAccount(_: AccountId)).expects(*).returning(snapshotFixture).once()

        val Right(snapshot) = Await.result(ledgerService.computeBalance(accountId1Fixture), 1.second)
        snapshot should ===(snapshotFixture)
      }

      "with no records" in new WithMocks {
        (mockLedger.getRecords(_: AccountId)).expects(*).returning(List.empty[Record]).once()

        val Left(problem) = Await.result(ledgerService.computeBalance(accountId1Fixture), 1.second)
        problem.status should ===(StatusCodes.NotFound)
      }
    }

    trait WithMocks {
      val mockLedger: Ledger = mock[Ledger]
      val mockOperationRepository: OperationRepository = mock[OperationRepository]
      val ledgerService: LedgerService = new LedgerService(mockLedger, mockOperationRepository)

    }

    trait WithStubs {
      val mockLedger: Ledger = stub[Ledger]
      val ledgerService: LedgerService = new LedgerService(mockLedger, new OperationRepository())
    }

  }

}
