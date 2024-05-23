package com.cesar.elasticsearch.index.utils

import co.elastic.clients.elasticsearch.ElasticsearchClient

trait ElasticSearchConfigurations {
  def esClient: ElasticsearchClient
}
