package org.sbuild.runner.server

import java.io.File
import org.sbuild.runner.SBuildRunner
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.actor.PoisonPill

object BuildReceptionist {
  case class BuildRequest(directory: File, args: Array[String])
  case object BuildFinished

  case class BuildSubscribe(subscriber: ActorRef)

  case class CancelBuild(id: Long)
  case object CancelAll

  case class AskHasBuild(id: Long)
  case class HasBuild(id: Long)

  def props(sbuildHomeDir: File): Props = Props(new BuildReceptionist(sbuildHomeDir))
}

class BuildReceptionist(sbuildHomeDir: File) extends Actor {
  require(sbuildHomeDir.exists(), s"SBuild home directory does not exists: ${sbuildHomeDir}")
  import BuildReceptionist._

  private[this] var _nextId = 0L
  def nextId(): Long = {
    _nextId = nextId + 1
    _nextId
  }

  private[this] var workers: Map[Long, ActorRef] = Map()

  def receive: Actor.Receive = {
    case BuildRequest(dir, args) =>
      val id = nextId()
      val worker = context.actorOf(BuildWorker.props(self, dir, args))
      workers += id -> worker

      sender ! RequestProcessor.BuildStarted(id)

    case BuildFinished =>
      workers.filter(_._2 == sender)

    case AskHasBuild(id) =>
      workers.contains(id)
      sender ! HasBuild(id)

    // TODO: suport subscription and unsubscription
    // TODO: cretae a deathwatch for our workers
  }
}

object BuildWorker {

  case object StartBuild

  def props(buildReceptionist: ActorRef, dir: File, args: Array[String]): Props =
    Props(new BuildWorker(buildReceptionist, dir, args))
}

class BuildWorker(master: ActorRef, dir: File, args: Array[String]) extends Actor {
  import BuildWorker._

  def receive: Actor.Receive = {
    case StartBuild =>
      val retVal = SBuildRunner.run(cwd = dir, args = args, rethrowInVerboseMode = false)
      master ! BuildReceptionist.BuildFinished
      self ! PoisonPill
  }
}