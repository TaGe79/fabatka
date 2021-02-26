package org.tage.fabatka

import org.tage.blockchain.Block
import org.tage.blockchain.BlockChain
import org.tage.blockchain.TransactionInput
import org.tage.blockchain.TransactionOutput
import org.tage.blockchain.Transactions
import java.security.PublicKey

class FabatkaChain : BlockChain<Block<Transactions>>() {
  companion object {
    private val UTXOs: MutableMap<String, TransactionOutput> = mutableMapOf()
    private val utxoSynchronizer = Object()

    fun addUTXOs(utxos: List<TransactionOutput>) {
      UTXOs.putAll(utxos.map { it.id to it })
    }

    fun bookkeepingUtxo(inputs: List<TransactionInput>, outputs: List<TransactionOutput>) {
      synchronized(utxoSynchronizer) {
        outputs.forEach { UTXOs[it.id] = it }
        inputs.forEach { UTXOs.remove(it.UTXO.id) }
      }
    }

    fun utxosOf(publicKey: PublicKey): Map<String, TransactionOutput> = UTXOs.filter { it.value.isMine(publicKey) }
  }
}
