package org.tage.blockchain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.slf4j.LoggerFactory
import org.tage.fabatka.FabatkaChain
import java.math.BigDecimal
import java.security.PublicKey
import java.security.Signature
import java.util.Base64

@JsonSerialize
@JsonIgnoreProperties(value = ["signer"])
data class Transaction(val sender: PublicKey, val recipient: PublicKey, val value: BigDecimal,
                       val inputs: List<TransactionInput>,
                       private val signer: (() -> String) -> ByteArray) {
  private val sequence = SEQ++
  val id: String = BlockChain.calculateHash(this::toShortString)
  val signature: ByteArray = signer(this::toShortString)

  val outputs: List<TransactionOutput>

  @JsonIgnore
  fun toShortString(): String = sequence.toString() +
      Base64.getEncoder().encodeToString(this.sender.encoded) +
      Base64.getEncoder().encodeToString(this.recipient.encoded) +
      this.value

  @JsonIgnore
  fun verify(): Boolean = Signature.getInstance("ECDSA", "BC").also {
    it.initVerify(this.sender)
    it.update(this.toShortString().toByteArray())
  }.verify(this.signature)

  init {
    if (!verify()) {
      log.error("Transaction [{}] has been tampered!", id)
      throw IllegalStateException("Tampered transaction [$id]")
    }

    val iv = inputValue()

    if (iv < BlockChain.minimumTx) {
      log.error("Transaction [{}] input too low!", id)
      throw IllegalStateException("Transactions below ${BlockChain.minimumTx}")
    }

    val leftover = iv - value
    if (leftover < BigDecimal.ZERO) {
      log.error("Insufficient funds $iv < $value!")
      throw IllegalStateException("Insufficient funds!")
    }

    outputs = listOf(TransactionOutput(recipient, value, id), TransactionOutput(sender, leftover, id))

    FabatkaChain.bookkeepingUtxo(inputs, outputs)
  }

  private fun inputValue(): BigDecimal = inputs.sumByDouble { it.UTXO.value.toDouble() }.toBigDecimal()

  companion object {
    private val log = LoggerFactory.getLogger(Transaction::class.java)
    private var SEQ: Long = 0
  }
}

@JsonSerialize
data class TransactionInput(val transactionOutputId: String, val UTXO: TransactionOutput)

@JsonSerialize
data class TransactionOutput(val recipient: PublicKey, val value: BigDecimal, val parentTransactionId: String) {
  val id: String = BlockChain.calculateHash(this::toShortString)

  private fun toShortString(): String = Base64.getEncoder().encodeToString(recipient.encoded) + value + parentTransactionId

  fun isMine(publicKey: PublicKey) = publicKey == recipient
}

data class Transactions(val txs: List<Transaction>) {
  val merkleRoot: String = merkleRoot(txs.map { it.id })[0]

  private fun merkleRoot(hashes: List<String>): List<String> =
      if (hashes.size == 1) hashes
      else {
        val groupedHashes = (if (hashes.size % 2 == 1) (hashes + listOf(hashes.last())) else hashes)
            .mapIndexed { index, s -> index to s }
            .groupBy({ it -> it.first % 2 }, { it -> it.second })
        merkleRoot(groupedHashes[0].orEmpty().mapIndexed { i, hash -> BlockChain.calculateHash { hash + groupedHashes[1].orEmpty()[i] } })
      }

  fun toShortString(): String = txs.map { it.toShortString() }.joinToString(separator = ";")
}
