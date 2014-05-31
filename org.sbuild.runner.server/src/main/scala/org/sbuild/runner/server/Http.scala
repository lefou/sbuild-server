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
import scala.util.Success
import spray.can.Http.Register
import scala.util.Failure
import akka.io.Tcp.PeerClosed
import spray.http.HttpRequest
import spray.can.Http.Register
import spray.http.HttpEntity.{NonEmpty, Empty}

class Receptionist(buildHandler: ActorRef, streamHandler: ActorRef) extends Actor {
  def receive: Actor.Receive = initializing

  def initializing: Actor.Receive = {
    case GetHttpConfig =>
      sender ! HttpConfig("0.0.0.0", 1234)
      context become accepting
  }

  def accepting: Actor.Receive = {
    case Connected(a, b) =>
      val processor = context.actorOf(Props(new RequestProcessor(buildHandler, streamHandler)))

      println("fooo"  + a + " " + b)
      sender ! Register(processor)
  }
}

class RequestProcessor(buildHandler: ActorRef, streamHandler: ActorRef) extends Actor {
  def receive: Actor.Receive = {
      case PeerClosed =>
          self ! PoisonPill
//    case Count(client, 0) =>
//      client ! ChunkedMessageEnd()
//      context.become(accepting)
//    case Count(client, remaining) =>
//      client ! MessageChunk(s"Count $remaining\n")
//      context.system.scheduler.scheduleOnce(2.seconds, self, Count(client, remaining - 1))
//    //      (new SBuildRunner).run()
    case HttpRequest(POST, Uri.Path("/run"), _, Empty, _) =>
      sender ! HttpResponse(StatusCodes.BadRequest, "Please provide working directory in the body of the request.")
    case HttpRequest(POST, Uri.Path("/run"), _, NonEmpty(_, entity), _) =>
      val lines  = new String(entity.toByteArray, "UTF-8").lines

      lines.zipWithIndex foreach {case (a, b) => println(a, b)}
      sender ! HttpResponse(entity = "Ok!!")
    case a =>
      sender ! HttpResponse(StatusCodes.NotFound)
  }
}