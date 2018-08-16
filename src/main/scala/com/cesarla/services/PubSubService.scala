package com.cesarla.services

import java.util.Comparator
import java.util.concurrent.{Executors, PriorityBlockingQueue}

import com.cesarla.models.Operation

class PubSubService(handler: Operation => Unit) {
  private[this] val queue: PriorityBlockingQueue[Operation] =
    new PriorityBlockingQueue[Operation](11, Comparator.comparingLong[Operation](_.operationId.value.timestamp()))

  private[this] val pool = Executors.newFixedThreadPool(1)

  pool.submit(new Runnable {
    override def run(): Unit = {
      while (!Thread.currentThread.isInterrupted) {
        try {
          handler(queue.take())
        } catch {
          case _: InterruptedException =>
            Thread.currentThread.interrupt()
            ()
        }
      }
    }
  })

  def publish(t: Operation): Unit = {
    queue.put(t)
  }
}
