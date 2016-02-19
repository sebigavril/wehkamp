package com.wehkart

import akka.actor.ActorSystem
import com.google.inject.Singleton

class ExecutionContexts @Singleton()() {

  implicit def ec = dispatcher

  private lazy val system = ActorSystem("ExecutionContextLooker")
  private lazy val dispatcher = system.dispatchers.lookup("my-dispatcher")

}
