
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.tage.blockchain.TransactionOutput
import org.tage.blockchain.Wallet
import org.tage.fabatka.FabatkaChain
import java.math.BigDecimal
import java.security.Signature

@DisplayName("A Wallet")
class WalletTest {
  @Test
  @DisplayName("has a name and consists of a private/public key pair")
  fun verify_wallet_keypair() {
    val w = Wallet("Test")
    Assertions.assertEquals("Test", w.name)

    Assertions.assertEquals("ECDSA", w.privateKey.algorithm)
    Assertions.assertEquals("PKCS#8", w.privateKey.format)

    Assertions.assertEquals("ECDSA", w.publicKey.algorithm)
    Assertions.assertEquals("X.509", w.publicKey.format)
  }

  @Test
  @DisplayName("can sign arbitrary data")
  fun verify_signature() {
    val w = Wallet("Test")
    val signature = w.sign { "FOO" }

    Assertions.assertNotNull(signature)
    Assertions.assertTrue(Signature.getInstance("ECDSA", "BC")
        .also {
          it.initVerify(w.publicKey)
          it.update("FOO".toByteArray())
        }.verify(signature))
  }

  @Test
  @DisplayName("calculates its balance from the ZeroChain")
  fun verify_balance_calculation() {
  }

  @Test
  @DisplayName("can send funds to an arbitrary recipient")
  fun verify_send_funds() {

    val w = Wallet("Test")
    val t = Wallet("Target")

    FabatkaChain.addUTXOs(listOf(TransactionOutput(w.publicKey, BigDecimal.valueOf(0.4), ""),
        TransactionOutput(w.publicKey, BigDecimal.valueOf(0.5), ""),
        TransactionOutput(w.publicKey, BigDecimal.valueOf(0.6), ""),
        TransactionOutput(w.publicKey, BigDecimal.valueOf(0.3), "")))

    w.sendFunds(t.publicKey, BigDecimal.valueOf(1.0)).also {
      Assertions.assertEquals(1.0, it.value.toDouble())
      Assertions.assertEquals(3, it.inputs.size)
    }

    Assertions.assertEquals(1.0, t.balance().toDouble())
    Assertions.assertEquals(0.8, w.balance().toDouble())
  }

}
