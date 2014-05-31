package org.sbuild.runner.server

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.io.IO
import spray.can.Http
import scala.util.{Failure, Success}
import spray.http._
import HttpMethods._
import spray.can.Http.{Connected, Register}
import spray.http.HttpRequest
import spray.http.HttpResponse
import scala.util.Success
import spray.can.Http.Register
import scala.util.Failure

class BuildReceptionist extends Actor {
  def receive: Actor.Receive = {
    case e =>
      println(e)
  }
}

class BuildWorker extends Actor {
  def receive: Actor.Receive = {
    case e =>
      println(e)
  }
}