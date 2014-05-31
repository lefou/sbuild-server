package org.sbuild.runner.server

import java.io.File
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.io.Tcp.PeerClosed
import spray.can.Http.Connected
import spray.can.Http.Register
import spray.http.HttpEntity.Empty
import spray.http.HttpEntity.NonEmpty
import spray.http.HttpEntity.apply
import spray.http.HttpMethods.POST
import spray.http._
import spray.http.HttpRequest
import spray.can.Http.Register
import spray.http.HttpEntity.NonEmpty
import spray.http.HttpResponse
import spray.http.StatusCodes
import spray.http.Uri
import java.nio.ByteBuffer
import spray.httpx.unmarshalling.FormDataUnmarshallers
import spray.httpx.unmarshalling.Unmarshaller
import java.net.URLEncoder
import java.net.URLDecoder
import scala.concurrent.Future

object Receptionist {

  def props(host: String, port: Int, buildHandler: ActorRef, streamHandler: ActorRef): Props =
    Props(new Receptionist(host, port, buildHandler, streamHandler))

}

class Receptionist(host: String, port: Int, buildHandler: ActorRef, streamHandler: ActorRef) extends Actor {
  def receive: Actor.Receive = initializing

  def initializing: Actor.Receive = {
    case GetHttpConfig =>
      sender ! HttpConfig(host, port)
      context become accepting
  }

  def accepting: Actor.Receive = {
    case Connected(a, b) =>
      val processor = context.actorOf(Props(new RequestProcessor(buildHandler, streamHandler)))

      println("fooo" + a + " " + b)
      sender ! Register(processor)
  }
}

object RequestProcessor {
  case class BuildStarted(buildId: Long)

}

class RequestProcessor(buildHandler: ActorRef, streamHandler: ActorRef) extends Actor {
  import RequestProcessor._
  import JvmStreamProcessor._


  def receive: Actor.Receive = awaitRequest

  def awaitRequest: Actor.Receive = generalAction orElse {
    //    case Count(client, 0) =>
    //      client ! ChunkedMessageEnd()
    //      context.become(accepting)
    //    case Count(client, remaining) =>
    //      client ! MessageChunk(s"Count $remaining\n")
    //      context.system.scheduler.scheduleOnce(2.seconds, self, Count(client, remaining - 1))
    //    //      (new SBuildRunner).run()
    case HttpRequest(POST, Uri.Path("/run"), _, Empty, _) =>
      sender ! HttpResponse(StatusCodes.BadRequest, "Please provide working directory in the body of the request.")
    case HttpRequest(POST, Uri.Path("/run"), _, entity @ NonEmpty(contentType, data), _) =>

      val result = entity.asString.split("[&]").map { s =>
        URLDecoder.decode(s, "UTF-8")
      }
      println(result.size + " " + result.toSeq)

      // FIXME
      val dir: File = new File(result.head)
      // FIXME
      val args: Array[String] = result.tail

      buildHandler ! BuildReceptionist.BuildRequest(dir, args)

      context.become(waitingForBuildId(sender))
    case HttpRequest(get, Uri.Path("/stream/out"), _, _, _) =>
      import scala.concurrent.ExecutionContext.Implicits.global
      Future {
        while (true) {
          if (math.random > 0.5)
            println("Hello!!!")
          else
            System.err.println("Fooo Bar!!!")

          Thread.sleep(1000)
        }
      }

      sender ! ChunkedResponseStart(HttpResponse())
      streamHandler ! Subscribe(0, None)
      context.become(streaming(sender))
    case a =>
      sender ! HttpResponse(StatusCodes.NotFound)
  }

  def streaming(client: ActorRef): Actor.Receive = generalAction orElse {
    case LogLines(lines) =>
      lines.foreach {
        case OutLine(text, time) => client ! MessageChunk(s"[out] $time - $text\n")
        case ErrLine(text, time) => client ! MessageChunk(s"[err] $time - $text\n")
      }
  }
    // TODO: end stream

  def generalAction: Actor.Receive = {
    case PeerClosed =>
      self ! PoisonPill
  }

  def waitingForBuildId(client: ActorRef): Actor.Receive = generalAction orElse {
    case BuildStarted(id) =>
      client ! HttpResponse(entity = s"${id}")
      self ! PoisonPill
  }
}