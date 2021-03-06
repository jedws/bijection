/*
 * Copyright 2010 Twitter Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twitter.bijection.json

import com.twitter.bijection.Bijection.asMethod

import com.twitter.bijection.{ BaseProperties, Bijection }
import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Arbitrary

import org.codehaus.jackson.JsonNode
import com.twitter.bijection.json.JsonNodeBijection.{fromJsonNode, toJsonNode}

object JsonBijectionLaws extends Properties("JsonBijection") with BaseProperties {
  def roundTripJson[T: JsonNodeBijection : Arbitrary] = {
    implicit val bij: Bijection[T,String] = JsonBijection.toString[T]
    roundTrips[T,String]()
  }
  def roundTripJsonEq[T: JsonNodeBijection : Arbitrary](eq: (T,T) => Boolean) = {
    implicit val bij: Bijection[T,String] = JsonBijection.toString[T]
    roundTrips[T,String](eq)
  }

  property("Short") = roundTripJson[Short]
  property("Int") = roundTripJson[Int]
  property("Long") = roundTripJson[Long]
  property("Double") = roundTripJson[Double]
  property("String") = roundTripJson[String]
  property("Array[Byte]") = roundTripJsonEq[Array[Byte]]({(l,r) => (l.toList == r.toList)})
  // Collections
  property("Either[String,Int]") = roundTripJson[Either[String,Int]]
  property("Map[String, Either[String,Int]]") = roundTripJson[Map[String, Either[String,Int]]]
  property("Map[String,Int]") = roundTripJson[Map[String,Int]]
  property("Map[String,Long]") = roundTripJson[Map[String,Long]]
  property("Map[String,String]") = roundTripJson[Map[String,String]]
  property("Map[String,Map[String,Int]]") = roundTripJson[Map[String,Map[String,Int]]]
  property("List[String]") = roundTripJson[List[String]]
  property("List[Int]") = roundTripJson[List[Int]]
  property("List[List[String]]") = roundTripJson[List[List[String]]]
  property("Set[Int]") = roundTripJson[Set[Int]]

  //property("Seq[Int]") = roundTripJson[Seq[Int]]
  //property("Vector[Int]") = roundTripJson[Vector[Int]]
  //property("IndexedSeq[Int]") = roundTripJson[IndexedSeq[Int]]

  // Handle Mixed values:
  property("Mixed values") = forAll { (kv: List[(String, Int, List[String])]) =>
    val mixedMap = kv.map { case (key, intv, lv) =>
      if (scala.math.random < 0.5) { (key + "i", toJsonNode(intv)) }
      else { (key + "l", toJsonNode(lv)) }
    }.toMap

    val jsonMixed = mixedMap.as[UnparsedJson]

    jsonMixed.as[Map[String, JsonNode]].map { kup : (String, JsonNode) =>
      val (k, up) = kup
      if (k.endsWith("i")) {
        fromJsonNode[Int](up) == fromJsonNode[Int](mixedMap(k))
      }
      else {
        fromJsonNode[Int](up) == fromJsonNode[Int](mixedMap(k))
      }
    }.forall { x => x}
  }

}
