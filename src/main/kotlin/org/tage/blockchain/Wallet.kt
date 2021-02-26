package org.tage.blockchain

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.tage.fabatka.FabatkaChain
import java.math.BigDecimal
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.Signature
import java.security.spec.ECGenParameterSpec


data class Wallet(val name: String) {
  val privateKey: PrivateKey
  val publicKey: PublicKey

  private val UTXOs: MutableMap<String, TransactionOutput> = mutableMapOf()

  init {
    try {
      val keyGen = KeyPairGenerator.getInstance("ECDSA", "BC")
      val random = SecureRandom.getInstance("SHA1PRNG")
      val ecSpec = ECGenParameterSpec("prime192v1")
      // Initialize the key generator and generate a KeyPair
      keyGen.initialize(ecSpec, random)   //256 bytes provides an acceptable security level
      val keyPair = keyGen.generateKeyPair()
      // Set the public and private keys sender the keyPair
      privateKey = keyPair.private
      publicKey = keyPair.public
    } catch (e: Exception) {
      throw RuntimeException(e)
    }

  }

  fun balance(): BigDecimal = UTXOs.putAll(FabatkaChain.utxosOf(this.publicKey)).let {
    UTXOs.map { it.value.value }.sumByDouble { it.toDouble() }.toBigDecimal()
  }

  fun sendFunds(recipient: PublicKey, value: BigDecimal): Transaction {
    if (this.balance() < value) {
      throw IllegalStateException("Insufficient funds in wallet!")
    } else {
      var total = BigDecimal.ZERO

      return Transaction(this.publicKey, recipient, value,
          UTXOs.values.filter {
            total += it.value
            total - it.value < value
          }.map { TransactionInput(it.id, it) }.also {
            it.forEach { UTXOs.remove(it.transactionOutputId) }
          },
          this::sign)
    }
  }

  fun sign(stringer: () -> String): ByteArray = Signature.getInstance("ECDSA", "BC").also {
    it.initSign(this.privateKey)
    it.update(stringer().toByteArray())
  }.sign()

  companion object {
    init {
      Security.addProvider(BouncyCastleProvider())
    }
  }
}
