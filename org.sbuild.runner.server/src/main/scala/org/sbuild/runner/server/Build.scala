package org.sbuild.runner.server

import java.io.File

import org.sbuild.runner.SBuildRunner

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala

object BuildReceptionist {
  case class BuildRequest(directory: File, args: Array[String])
  case class BuildSubscribe(subscriber: ActorRef)
  case class CancelBuild(id: Long)
  case object CancelAll
  case class HasBuild(id: Long)
}

class BuildReceptionist extends Actor {
  import BuildReceptionist._

  private[this] var _nextId = 0L
  def nextId(): Long = {
    _nextId = nextId + 1
    _nextId
  }

  def receive: Actor.Receive = {
    case BuildRequest(dir, args) =>
      val id = nextId()
      context.actorOf(BuildWorker.props(self, id, dir, args))
      sender ! RequestProcessor.BuildStarted(id)

    // TODO: suport subscription and unsubscription
    // TODO: cretae a deathwatch for our workers
  }
}

object BuildWorker {
  def props(buildReceptionist: ActorRef, id: Long, dir: File, args: Array[String]): Props =
    Props(new BuildWorker(buildReceptionist, id, dir, args))
}

class BuildWorker(buildReceptionist: ActorRef, id: Long, dir: File, args: Array[String]) extends Actor {

  SBuildRunner.run(cwd = dir, args = args, rethrowInVerboseMode = false)

  def receive: Actor.Receive = {
    case e =>
      println(e)
  }
}