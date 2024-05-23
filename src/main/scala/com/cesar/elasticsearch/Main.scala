package com.cesar.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.{ElasticsearchTransport, TransportUtils}
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.cesar.elasticsearch.index.message.{MessageIndexed, MessageIndexer}
import com.cesar.elasticsearch.index.utils.ElasticSearchConfigurations
import org.apache.http.HttpHost
import org.apache.http.auth.{AuthScope, UsernamePasswordCredentials}
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.client.RestClient

import java.util.Date
import javax.net.ssl.SSLContext

class Main {
  private val messageIndexer = new MessageIndexer(elasticConfigs)
  messageIndexer.getMessages("user1")
  messageIndexer.saveMessage(MessageIndexed(
    key = "message1",
    content = "hola world",
    user = "user1",
    inserted = new Date()
  ))
  messageIndexer.findMessagesByFilter("hola", "user1")

  def elasticConfigs: ElasticSearchConfigurations = new ElasticSearchConfigurations {
    def fingerprint: String = "elasticSearchFingerprint"

    def username: String = "elasticSearchUsername"

    def password: String = "elasticSearchPassword"

    def SSLContext: SSLContext = TransportUtils.sslContextFromCaFingerprint(fingerprint)

    def credentials: BasicCredentialsProvider = {
      val basicCredentialsProvider = new BasicCredentialsProvider()
      basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password))
      basicCredentialsProvider
    }

    def getHost: String = "appHost"

    def clientRest: RestClient = RestClient
      .builder(
        new HttpHost(getHost, 9200, "https")
      ).setHttpClientConfigCallback(
        _.setSSLContext(SSLContext)
          .setDefaultCredentialsProvider(credentials))
      .build

    def transport: ElasticsearchTransport = new RestClientTransport(
      clientRest, new JacksonJsonpMapper()
    )

    override def esClient: ElasticsearchClient = new ElasticsearchClient(transport)
  }
}
