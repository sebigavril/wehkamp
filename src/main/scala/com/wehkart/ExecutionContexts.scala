package com.wehkart

object ExecutionContexts {

  //todo configure this
  implicit def ctx = scala.concurrent.ExecutionContext.Implicits.global

}
