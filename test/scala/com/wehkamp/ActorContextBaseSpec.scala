package com.wehkamp

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.wehkamp.TestUtils.actorSystem

class ActorContextBaseSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender {

  def this() = this(actorSystem)

}