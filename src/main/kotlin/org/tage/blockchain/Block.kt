package org.tage.blockchain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.security.PublicKey
import java.time.Instant
import java.util.Base64
import java.util.stream.Collectors
import java.util.stream.IntStream


@JsonSerialize
@JsonIgnoreProperties(ignoreUnknown = true)
data class Block<D : Any>(val timestamp: Long, val data: D, val previousHash: String = "",
                          val pow: String = "") {
  val hashcode: String = BlockChain.calculateHash(this::toShortString)

  fun mine(difficulty: Int): Block<D> {
    var block = this
    var i = 0L
    val powString = IntStream.range(0, difficulty).mapToObj { "0" }.collect(Collectors.joining())
    while (block.hashcode.substring(0, difficulty) != powString) {
      block = block.copy(pow = (i++).toString())
    }
    return block
  }

  fun toShortString(): String =
      "${data::class.java.getMethod("toString").invoke(data) as String}$timestamp$previousHash$pow"


  override fun toString(): String = jacksonObjectMapper().also {
    it.registerModule(SimpleModule().also {
      it.addSerializer(PublicKey::class.java, PublicKeySerializer())
    })
  }.writeValueAsString(this)

  companion object {
    fun <D : Any> of(data: D) = Block(Instant.now().toEpochMilli(), data)
  }

  class PublicKeySerializer : StdSerializer<PublicKey>(PublicKey::class.java, true) {

    override fun serialize(value: PublicKey?,
                           gen: JsonGenerator?, provider: SerializerProvider?) {
      gen?.writeString(Base64.getEncoder().encodeToString(value?.encoded))
    }

  }
}
