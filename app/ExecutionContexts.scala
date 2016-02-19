package com.wehkart

import com.google.inject.Inject

class ExecutionContexts @Inject() () {

  //todo configure this
  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global

}
