package com.tristanpenman.chordial.demo.service

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.TestActorRef
import com.tristanpenman.chordial.demo.Governor._
import com.tristanpenman.chordial.demo.WebService
import org.scalatest.{ShouldMatchers, WordSpec}
import spray.json._
import spray.testkit.ScalatestRouteTest

class WebServiceSpec extends WordSpec with ShouldMatchers with WebService with ScalatestRouteTest {

  import WebService._

  def actorRefFactory: ActorSystem = system

  private def dummyActor: ActorRef = TestActorRef(new Actor {
    def receive: Receive = {
      case message =>
        fail(s"Dummy actor should not receive any messages, but just received: $message")
    }
  })

  "The web service" when {

    "backed by a Governor with no nodes" should {
      val governor: ActorRef = TestActorRef(new Actor {
        def receive: Receive = {
          case CreateNode() =>
            sender() ! CreateNodeOk(1L, dummyActor)
          case CreateNodeWithSeed(seedId) =>
            sender() ! CreateNodeWithSeedOk(2L, dummyActor)
          case GetNodeIdSet() =>
            sender() ! GetNodeIdSetOk(Set.empty)
        }
      })

      "respond to a GET request on the /nodes endpoint with an empty JSON array" in {
        Get("/nodes") ~> routes(governor) ~> check {
          val jsonAst = responseAs[String].parseJson
          val jsonAsNodeAttrArray = jsonAst.convertTo[Iterable[NodeAttributes]]
          assert(jsonAsNodeAttrArray.isEmpty)
        }
      }

      "respond to a POST request on the /nodes endpoint with the correct JSON object" in {
        Post("/nodes") ~> routes(governor) ~> check {
          val jsonAst = responseAs[String].parseJson
          val jsonAsNodeAttr = jsonAst.convertTo[NodeAttributes]
          assert(jsonAsNodeAttr.nodeId == 1L)
          assert(jsonAsNodeAttr.successorId.contains(1L))
          assert(jsonAsNodeAttr.active)
        }
      }

      "respond to a POST request on the /nodes endpoint (including a seed ID) with the correct JSON object" in {
        Post("/nodes?seed_id=1") ~> routes(governor) ~> check {
          val jsonAst = responseAs[String].parseJson
          val jsonAsNodeAttr = jsonAst.convertTo[NodeAttributes]
          assert(jsonAsNodeAttr.nodeId == 2L)
          assert(jsonAsNodeAttr.successorId.contains(1L))
          assert(jsonAsNodeAttr.active)
        }
      }
    }

    "backed by a Governor with existing nodes" should {
      val governor: ActorRef = TestActorRef(new Actor {
        def receive: Receive = {
          case GetNodeIdSet() =>
            sender() ! GetNodeIdSetOk(Set(0L, 1L, 2L))
          case GetNodeState(nodeId) =>
            sender() ! GetNodeStateOk(nodeId == 0L || nodeId == 1L)
          case GetNodeSuccessorId(nodeId) =>
            if (nodeId == 2L) {
              fail("Web service attempted to query successor ID of an inactive node")
            } else {
              sender() ! GetNodeSuccessorIdOk((nodeId + 1L) % 2L)
            }
        }
      })

      "respond to a GET request on the /nodes endpoint with a JSON array of correct JSON objects" in {
        Get("/nodes") ~> routes(governor) ~> check {
          val jsonAst = responseAs[String].parseJson
          val jsonAsNodeAttrArray = jsonAst.convertTo[Array[NodeAttributes]]
          assert(jsonAsNodeAttrArray.length == 3)
          assert(jsonAsNodeAttrArray.contains(NodeAttributes(0L, Some(1L), active = true)))
          assert(jsonAsNodeAttrArray.contains(NodeAttributes(1L, Some(0L), active = true)))
          assert(jsonAsNodeAttrArray.contains(NodeAttributes(2L, None, active = false)))
        }
      }
    }
  }
}