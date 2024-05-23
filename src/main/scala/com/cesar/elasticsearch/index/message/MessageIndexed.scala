package com.cesar.elasticsearch.index.message

import java.util.Date

case class MessageIndexed(
                           key: String,
                           content: String,
                           user: String,
                           inserted: Date
                         )
