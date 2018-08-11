package com.cesarla.services

import java.util.concurrent.LinkedBlockingQueue

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PubSubService[T](handler: T => Unit) {
  private[this] val queue = new LinkedBlockingQueue[T]()

  Future {
    while (!Thread.currentThread.isInterrupted) {
      try {
        handler(queue.take())
      } catch {
        case _: InterruptedException => ()
      }
    }
  }

  def publish(t: T): Unit = {
    queue.put(t)
  }
}
