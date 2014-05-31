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
import org.sbuild.runner.server.HttpConfig

case object GetHttpConfig
case class HttpConfig(host: String, port: Int)

object Server extends App {
  implicit val system = ActorSystem()

  // the handler actor replies to incoming HttpRequests
  val handler = system.actorOf(Props[Foo], "coffee-service")

  import system.dispatcher
  implicit val timeout = Timeout(5.seconds)

  (handler ? GetHttpConfig).onComplete {
    case Success(HttpConfig(host, port)) =>
      IO(Http) ! Http.Bind(handler, host, port, 1000)
    case Success(s) =>
      throw new IllegalStateException(s"Unexpected result: $s")
    case Failure(t) =>
      t.printStackTrace()
  }
}

//object Server extends App with SimpleRoutingApp {
//
//  implicit val actorSystem = ActorSystem()
//
//  private val serverPort = 8080
//
//  startServer(interface = "0.0.0.0", port = serverPort) {
//    post {
//      path("echo") {
//        entity(as[String]) {
//          parameters =>
//            println(parameters.split("&").toList)
//            complete {
//              s"http://localhost:$serverPort/echo/1234"
//            }
//
//        }
//
//      }
//    } ~
//      get {
//        path("echo" / Rest) {
//          id =>
//            complete {
//              s"log for run of $id"
//            }
//        }
//      }
//  }
//}