package com.cesar.elasticsearch.index.utils

import com.fasterxml.jackson.databind.node.ObjectNode
import com.nimbusds.jose.shaded.gson.{Gson, GsonBuilder}

import scala.reflect.ClassTag
object ElasticJsonTransform {

  private val gson: Gson = new GsonBuilder().serializeNulls().create()

  implicit class DTOTransform[T](source: T) {
    final def toElasticJson: String = {
      gson.toJson(source)
    }
  }

  implicit class ElasticTransform[From<: ObjectNode](json: From) {
    final def toDTO[To](implicit classTag: ClassTag[To]): To = {
      gson.fromJson(json.toString, classTag.runtimeClass.asInstanceOf[Class[To]])
    }

  }

}
