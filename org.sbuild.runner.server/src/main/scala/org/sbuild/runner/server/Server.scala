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

case object GetHttpConfig
case class HttpConfig(host: String, port: Int)

object Server extends App {
  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val buildHandler = system.actorOf(Props[BuildReceptionist], "build-receptionist")
  val streamHandler = system.actorOf(Props[JvmStreamProcessor], "stream-processor")

  val httpHandler = system.actorOf(Props(new Receptionist(buildHandler, streamHandler)), "sbuild-service")

  import system.dispatcher
  implicit val timeout = Timeout(5.seconds)

  (httpHandler ? GetHttpConfig).onComplete {
    case Success(HttpConfig(host, port)) =>
      IO(Http) ! Http.Bind(httpHandler, host, port, 1000)
    case Success(s) =>
      throw new IllegalStateException(s"Unexpected result: $s")
    case Failure(t) =>
      t.printStackTrace()
  }
}
