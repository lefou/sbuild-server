package org.sbuild.runner.server

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.{Future, Await}
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
import java.io._
import java.util.concurrent.atomic.AtomicBoolean
import java.util.UUID

class JvmStreamProcessor extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  import JvmStreamProcessor._

  private val stopOut = new AtomicBoolean(false)
  private val stopErr = new AtomicBoolean(false)
  private var buffer: Vector[Line] = Vector.empty
  private var subscribers: Vector[ActorRef] = Vector.empty

  def setupStreams() {
    val (outOut, outIn) = getPipedOutStream
    val (errOut, errIn) = getPipedOutStream

    System.setOut(outOut)
    System.setErr(errOut)

    startListener(stopOut, outIn, OutLine(_))
    startListener(stopErr, errIn, ErrLine(_))
  }

  def startListener(stopCond: AtomicBoolean, in: BufferedReader, lineFn: String => Line) {
    Future {
      while (stopCond.get) {
        val line = in.readLine()

        if (line == null) stopCond.set(true)
        else newLine(lineFn(line))
      }
    }
  }

  def newLine(line: Line) =
    self ! NewLine(line)

  def getPipedOutStream =  {
    val out = new PipedOutputStream()
    val in = new PipedInputStream(out)

    (new PrintStream(out), new BufferedReader(new InputStreamReader(in)))
  }

  def receive: Actor.Receive = {
    case Subscribe(skip, tail) =>
      context watch sender

      subscribers = subscribers :+ sender

      val skipped = buffer drop skip
      val requestedLines = tail map (t => skipped takeRight t) getOrElse skipped

      sender ! LogLines(requestedLines)
    case NewLine(line) =>
      buffer = buffer :+ line
      subscribers foreach {sub => sub ! LogLines(Seq(line))}
    case Terminated(ref) =>
      subscribers = subscribers.filterNot(ref ==)
  }
}

object JvmStreamProcessor {
  case class Subscribe(skip: Int = 0, tail: Option[Int] = None)
  case class LogLines(line: Seq[Line])
  case class NewLine(line: Line)
}

trait Line {
  def text: String
  val time: Long
}

case class OutLine(text: String, time: Long = System.currentTimeMillis()) extends Line
case class ErrLine(text: String, time: Long = System.currentTimeMillis()) extends Line