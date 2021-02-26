import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.tage.blockchain.Block
import org.tage.blockchain.Transaction
import org.tage.blockchain.TransactionInput
import org.tage.blockchain.TransactionOutput
import org.tage.blockchain.Transactions
import org.tage.blockchain.Wallet
import org.tage.fabatka.FabatkaChain
import java.math.BigDecimal

/**
 *
 */
class FabatkaTest {
  @Test
  fun verify_blockchain() {
    val coinbase = Wallet("coinbase")
    val w1 = Wallet("W1")
    val w2 = Wallet("W2")
    val w3 = Wallet("W3")
    val w4 = Wallet("W4")
    val genesisUTXO = TransactionOutput(coinbase.publicKey, BigDecimal.valueOf(1000000.0), "0")
    val genesisTransaction = Transaction(coinbase.publicKey, w1.publicKey, BigDecimal.valueOf(1000000.0),
        listOf(TransactionInput(genesisUTXO.id, genesisUTXO)), coinbase::sign)

    FabatkaChain.addUTXOs(listOf(genesisUTXO))

    val fabatkaChain = FabatkaChain()

    fabatkaChain.add(Block.of(Transactions(listOf(genesisTransaction))))

    val b1 = Block.of(Transactions(listOf(
        w1.sendFunds(w2.publicKey, BigDecimal.valueOf(10.0)),
        w1.sendFunds(w3.publicKey, BigDecimal.valueOf(10.0)),
        w1.sendFunds(w4.publicKey, BigDecimal.valueOf(10.0)),
        w2.sendFunds(w4.publicKey, BigDecimal.valueOf(5.0)),
        w3.sendFunds(w4.publicKey, BigDecimal.valueOf(5.0)),
        w4.sendFunds(w1.publicKey, BigDecimal.valueOf(10.0)))))

    fabatkaChain.add(b1)

    Assertions.assertEquals(BigDecimal.valueOf(999980.0), w1.balance())
    Assertions.assertEquals(BigDecimal.valueOf(5.0), w2.balance())
    Assertions.assertEquals(BigDecimal.valueOf(5.0), w3.balance())
    Assertions.assertEquals(BigDecimal.valueOf(10.0), w4.balance())

    val b2 = Block.of(Transactions(listOf(
        w4.sendFunds(w2.publicKey, BigDecimal.valueOf(2.5))
    )))

    fabatkaChain.add(b2)

    Assertions.assertEquals(BigDecimal.valueOf(7.5), w4.balance())
    Assertions.assertEquals(BigDecimal.valueOf(7.5), w2.balance())

    Assertions.assertTrue(fabatkaChain.verify())
  }
}
