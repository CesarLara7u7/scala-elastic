package com.cesar.elasticsearch.index.message

import co.elastic.clients.elasticsearch._types.query_dsl.{BoolQuery, MatchQuery, Query}
import co.elastic.clients.elasticsearch.core.search.Hit
import co.elastic.clients.elasticsearch.core.{GetRequest, IndexRequest, SearchRequest, SearchResponse}
import co.elastic.clients.elasticsearch.indices.{CreateIndexRequest, ExistsRequest}
import co.elastic.clients.util.ObjectBuilder
import com.cesar.elasticsearch.index.utils.ElasticJsonTransform.{DTOTransform, ElasticTransform}
import com.cesar.elasticsearch.index.utils.ElasticSearchConfigurations
import com.fasterxml.jackson.databind.node.ObjectNode
import org.slf4j.LoggerFactory

import java.io.StringReader

class MessageIndexer(elasticSearchConfigurations: ElasticSearchConfigurations) {
  private val logger = LoggerFactory.getLogger(getClass)
  private val client = elasticSearchConfigurations.esClient
  private val INDEX = "messages"
  private var MAX_VALUE = 10
  verifyIndex()
  verifyIfMessagesAreIndexed()

  private def verifyIndex(): Unit = {
    if (client.indices().exists(ExistsRequest.of(_.index(INDEX))).value()) {
    } else {
      client.indices().create(createIndexFunction)
    }
  }

  private def verifyIfMessagesAreIndexed(): Unit = {
    val messages: Seq[MessageIndexed] = Nil // Recibe message from database
    MAX_VALUE = messages.size
    messages foreach { message => {
      val response = getDocument(message)
      if (response.isEmpty) {
        client.index(
          saveDocumentFunction(message)
        )
      }
    }
    }
  }


  private def createIndexFunction: java.util.function.Function[CreateIndexRequest.Builder, ObjectBuilder[CreateIndexRequest]] =
    builder =>
      builder.index(INDEX)


  private def saveDocumentFunction(message: MessageIndexed): java.util.function.Function[IndexRequest.Builder[MessageIndexed], ObjectBuilder[IndexRequest[MessageIndexed]]] =
    index =>
      index
        .index(INDEX)
        .id(message.key.toString)
        .withJson(new StringReader(message.toElasticJson))


  private def getDocument(message: MessageIndexed): Option[MessageIndexed] = {
    def getIndex: java.util.function.Function[GetRequest.Builder, ObjectBuilder[GetRequest]] = {
      request => {
        request
          .index(INDEX)
          .id(message.key)
      }
    }

    val response = client.get(getIndex, classOf[ObjectNode])
    if (response.found()) {
      Some(response.source().toDTO[MessageIndexed])
    } else {
      None
    }
  }

  private def filterDocuments(filter: String, user: String): java.util.function.Function[SearchRequest.Builder, ObjectBuilder[SearchRequest]] = {
    (search: SearchRequest.Builder) => {
      val builder = search.index(INDEX).size(MAX_VALUE).allowPartialSearchResults(true)

      def byRoomKeys(bool: BoolQuery.Builder) = {
        bool.must(MatchQuery.of(_.field("user").query(user))._toQuery())
      }

      if (filter.nonEmpty) {
        val byContent: Query = MatchQuery.of(_.field("content").query(filter).fuzziness("2"))._toQuery()

        def boolFunction = BoolQuery.of {
          bool => {
            byRoomKeys(bool)
            bool.must(byContent)
          }
        }

        def functionQuery = Query.of {
          (query: Query.Builder) => query.bool(boolFunction)
        }

        builder.query(functionQuery)
      } else {


        def functionQuery = Query.of {
          (query: Query.Builder) => query.bool(bool => byRoomKeys(bool))
        }

        builder.query(functionQuery)
      }
    }
  }

  def saveMessage(message: MessageIndexed): Unit = {
    if (getDocument(message).isEmpty) {
      client.index(saveDocumentFunction(message))
    }
  }

  private def searchResponseHitToSeqMessage(response: SearchResponse[ObjectNode]): Seq[MessageIndexed] = {
    val hits: java.util.List[Hit[ObjectNode]] = response.hits().hits()
    var messages: scala.collection.mutable.Seq[MessageIndexed] = scala.collection.mutable.Seq.empty
    hits.forEach(hit => {
      messages = messages ++ Seq(hit.source().toDTO[MessageIndexed])
    })

    val messageResponse: Seq[MessageIndexed] = messages.toSeq
    messageResponse
  }

  def findMessagesByFilter(filter: String, user: String): Seq[MessageIndexed] = {

    logger.info("Getting document  by filters: {}", filter)
    val response: SearchResponse[ObjectNode] = client.search(filterDocuments(filter, user), classOf[ObjectNode])
    searchResponseHitToSeqMessage(response)
  }

  def getMessages(user: String): Seq[MessageIndexed] = {
    val response: SearchResponse[ObjectNode] = client.search(filterDocuments("", user), classOf[ObjectNode])
    searchResponseHitToSeqMessage(response)
  }

}
