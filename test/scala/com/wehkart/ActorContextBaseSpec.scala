package com.wehkart

import scala.concurrent.Await
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.wehkart.ActorConstants.duration
import org.scalatest.{BeforeAndAfterEach, Suite}

class ActorContextBaseSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with Suite
    with BeforeAndAfterEach {

  override def beforeEach: Unit = {
    system.terminate()
    Await.result(system.whenTerminated, duration)
  }
}
