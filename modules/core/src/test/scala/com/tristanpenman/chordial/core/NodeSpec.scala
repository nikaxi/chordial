package com.tristanpenman.chordial.core

import akka.actor.{Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import com.tristanpenman.chordial.core.NodeProtocol._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class NodeSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with WordSpecLike with Matchers
with BeforeAndAfterAll {

  def this() = this(ActorSystem("NodeSpec"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "A Node actor" must {

    val ownId = 1L
    val node = system.actorOf(Props(classOf[Node], ownId))

    "respond to a GetId message with a GetIdOk message containing its ID" in {
      node ! GetId()
      expectMsg(GetIdOk(ownId))
    }

    "respond to a GetPredecessor message with a GetPredecessorOkButUnknown message" in {
      node ! GetPredecessor()
      expectMsg(GetPredecessorOkButUnknown())
    }

    "respond to a GetSuccessor message with a GetSuccessorOk message containing its own ID and ActorRef" in {
      node ! GetSuccessor()
      expectMsg(GetSuccessorOk(ownId, node))
    }
  }

}