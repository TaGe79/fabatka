import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.tage.blockchain.Transaction
import org.tage.blockchain.TransactionInput
import org.tage.blockchain.TransactionOutput
import org.tage.blockchain.Transactions
import org.tage.blockchain.Wallet
import java.math.BigDecimal

/**
 *
 */
val wFrom = Wallet("Test Wallet1")
val wTo = Wallet("Test Wallet2")

@DisplayName("A transaction")
class TransactionTest {

  @Test
  @DisplayName("shall have an id which represents its hash")
  fun verify_hash() {
    val t = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)

    Assertions.assertNotNull(t.id)
    Assertions.assertEquals(128, t.id.length)
  }

  @Test
  @DisplayName("shall have a signature calculated")
  fun verify_signature() {
    val t = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)

    System.out.println(t.signature.joinToString(separator = "") { String.format("%02x", 0xff and it.toInt()) })
    Assertions.assertNotNull(t.signature)
  }

  @Test
  @DisplayName("shall have a valid signature if no data has been tampered")
  fun verify_valid_signature() {
    val t = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)
    Assertions.assertTrue(t.verify())
  }

  @Test
  @DisplayName("shall report signature verification failure if it has been tampered")
  fun verify_invalid_signature_when_transaction_data_has_been_tamnpered() {
    val t = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)

    t.javaClass.getDeclaredField("value")
        .apply {
          this.trySetAccessible()
          this.set(t, BigDecimal.valueOf(1000.0))
        }

    Assertions.assertFalse(t.verify())
  }
}

@DisplayName("A container of transactions")
class TransactionContainerTest {
  @Test
  @DisplayName("returns the tx hash for a single tx ass merkle root")
  fun verify_tx_hash_as_merkle_root_for_signle_item() {
    val t1 = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)

    Assertions.assertEquals(Transactions(listOf(t1)).merkleRoot, t1.id)
  }

  @Test
  @DisplayName("calculates Merkle root for 2 transactions")
  fun verify_merkle_root_of_two() {
    val t1 = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)
    val t2 = Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(100.0),
        listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
        wFrom::sign)
    val TOT = Transactions(listOf(t1, t2))

    System.out.println(TOT.merkleRoot)

    Assertions.assertNotNull(TOT.merkleRoot)
    Assertions.assertEquals(128, TOT.merkleRoot.length)
  }

  @Test
  @DisplayName("calculates Merkle root for odd transactions")
  fun verify_merkle_root_of_five() {
    val merkleRoot = Transactions(listOf(10.0, 20.0, 30.0, 40.0, 50.0)
        .map {
          Transaction(wFrom.publicKey, wTo.publicKey, BigDecimal.valueOf(it),
              listOf(TransactionInput("", TransactionOutput(wFrom.publicKey, BigDecimal.valueOf(1000.0), ""))),
              wFrom::sign)
        }).merkleRoot

    System.out.println(merkleRoot)
    Assertions.assertNotNull(merkleRoot)
    Assertions.assertEquals(128, merkleRoot.length)

  }
}
