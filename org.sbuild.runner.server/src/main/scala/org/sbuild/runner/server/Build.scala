package org.sbuild.runner.server

import java.io.File
import org.sbuild.runner.SBuildRunner
import akka.actor._
import scala.util.control.NonFatal

object BuildReceptionist {
  case class BuildRequest(directory: File, args: Array[String])
  case class BuildFinished(retVal: Int)
  case class BuildFailed(e: Throwable)

  case class BuildSubscribe(subscriber: ActorRef)

  case class CancelBuild(id: Long)
  case object CancelAll

  case class AskHasBuild(id: Long)
  case class HasBuild(id: Long, has: Boolean)

  case object Subscribe
  case class BuildFinishedNotification(id: Long)

  def props(sbuildHomeDir: File): Props = Props(new BuildReceptionist(sbuildHomeDir))
}

class BuildReceptionist(sbuildHomeDir: File) extends Actor {
  require(sbuildHomeDir.exists(), s"SBuild home directory does not exists: ${sbuildHomeDir}")

  import BuildReceptionist._

  private[this] var _nextId = 0L
  def nextId(): Long = {
    _nextId = _nextId + 1
    _nextId
  }

  private[this] var workers: Map[Long, ActorRef] = Map()
  private var subscribers: Vector[ActorRef] = Vector.empty

  def removeWorker(ref: ActorRef, retVal: Int, error: Option[Throwable] = None): Unit = {
    val worker = workers find (_._2 == sender) getOrElse
      (throw new IllegalStateException("Ooops! Can't find worker ID!"))

    workers = workers.filter(worker !=)
    subscribers foreach (s => s ! BuildFinishedNotification(worker._1))

    // TODO: tell somebody
  }

  def receive: Actor.Receive = {
    case BuildRequest(dir, args) =>
      val id = nextId()
      val worker = context.actorOf(BuildWorker.props(self, dir, Array("--sbuild-home", sbuildHomeDir.getAbsolutePath()) ++ args))
      workers += id -> worker

      worker ! BuildWorker.StartBuild
      sender ! RequestProcessor.BuildStarted(id)

    case BuildFailed(exception) =>
      removeWorker(sender, 1, Some(exception))

    case BuildFinished(revVal) =>
      removeWorker(sender, revVal, None)

    case AskHasBuild(id) =>
      sender ! HasBuild(id, workers contains id)

    case Subscribe =>
      context watch sender
      subscribers = subscribers :+ sender

    case Terminated(ref) =>
      subscribers = subscribers.filterNot(ref ==)
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
      try {
        val retVal = SBuildRunner.run(cwd = dir, args = args, rethrowInVerboseMode = false)
        master ! BuildReceptionist.BuildFinished(retVal)
      } catch {
        case NonFatal(e) => master ! BuildReceptionist.BuildFailed(e)
      }
      self ! PoisonPill
  }
}