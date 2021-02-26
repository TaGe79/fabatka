package org.tage.blockchain

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.Collections
import java.util.LinkedList
import java.util.stream.Collectors
import java.util.stream.IntStream

abstract class BlockChain<B : Block<*>> {
  private var blockchain: List<B> = LinkedList()

  fun add(block: B): B {
    synchronized(blockchainSynchronizer) {
      val element = block.copy(previousHash = if (blockchain.isEmpty()) "" else blockchain.last().hashcode).mine(BlockChain.difficulty) as B
      blockchain += element
      return blockchain.last().also { log.debug("Block added -> [$it]") }
    }
  }

  fun getBlockchain(): List<B> = synchronized(blockchainSynchronizer) { Collections.unmodifiableList(blockchain) }

  fun verify(): Boolean = blockchain.foldRightIndexed(true,
      { idx, block, acc ->
        acc && (
            block.hashcode.substring(0, difficulty) == powPrefix &&
                block.hashcode == calculateHash(block::toShortString) &&
                (idx == 0 || blockchain[idx - 1].hashcode == block.previousHash))
            .also {
              if (!it) log.error("The ${idx}th block is corrupt in the chain!")
            }
      })

  companion object {

    val log: Logger = LoggerFactory.getLogger(BlockChain::class.java)

    private val blockchainSynchronizer = Object()

    const val difficulty = 3
    val minimumTx = BigDecimal(0.000001)

    val powPrefix: String = IntStream.range(0, difficulty).mapToObj { "0" }.collect(Collectors.joining())

    fun calculateHash(stringer: () -> String): String = MessageDigest.getInstance("SHA-512")
        .digest(stringer().toByteArray()).joinToString(separator = "") {
          String.format("%02x", 0xff and it.toInt())
        }
  }
}
