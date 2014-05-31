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

case class Count(client: ActorRef, remaining: Int)

class Foo extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  def receive = accepting

  def accepting: Actor.Receive = {
    case GetHttpConfig =>
      sender ! HttpConfig("0.0.0.0", 1234)
    case Connected(_, _) =>
      sender ! Register(self)
    case HttpRequest(POST, Uri.Path("/execute"), _, _, _) =>
      val client  = sender
      client ! ChunkedResponseStart(HttpResponse())

      context.system.scheduler.scheduleOnce(100.millis, self, Count(client, 10))
      context.become(counting)
  }

  def counting: Actor.Receive = {
    case Count(client, 0) =>
      client ! ChunkedMessageEnd()
      context.become(accepting)
    case Count(client, remaining) =>
      client ! MessageChunk(s"Count $remaining\n")
      context.system.scheduler.scheduleOnce(2.seconds, self, Count(client, remaining - 1))
  }
}