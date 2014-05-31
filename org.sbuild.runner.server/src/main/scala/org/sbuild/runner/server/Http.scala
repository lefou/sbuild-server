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

class Receptionist extends Actor {
  def receive: Actor.Receive = {
    case GetHttpConfig =>
      sender ! HttpConfig("0.0.0.0", 1234)
    case Connected(_, _) =>
      val processor = context.actorOf(Props[RequestProcessor])

      sender ! Register(processor)
  }
}

class RequestProcessor extends Actor {
  def receive: Actor.Receive = ???
//  {
//    case Count(client, 0) =>
//      client ! ChunkedMessageEnd()
//      context.become(accepting)
//    case Count(client, remaining) =>
//      client ! MessageChunk(s"Count $remaining\n")
//      context.system.scheduler.scheduleOnce(2.seconds, self, Count(client, remaining - 1))
//    //      (new SBuildRunner).run()
//    case HttpRequest(POST, Uri.Path("/execute"), _, _, _) =>
//      val client  = sender
//      client ! ChunkedResponseStart(HttpResponse())
//
//      context.system.scheduler.scheduleOnce(100.millis, self, Count(client, 10))
//      context.become(counting)
//  }
}