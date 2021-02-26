import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.tage.blockchain.Block
import org.tage.blockchain.BlockChain
import java.time.Instant

/**
 *
 */
@DisplayName("The Blockchain")
class BlockchainTest {
  class TestChain : BlockChain<Block<String>>()

  @Test
  @DisplayName("shall not have the genesis block when it is instantiated")
  fun verify_zero_chain_has_genesis_block_upon_creation() {
    val blockchain = TestChain().getBlockchain()
    Assertions.assertTrue(blockchain.isEmpty())
  }

  @Nested
  @DisplayName("consist of blocks which")
  class BlockTest {
    @Test
    @DisplayName("shall receive a calculated hash upon creation")
    fun verify_creating_first_block() {
      val block = Block(Instant.now().epochSecond, "{\"data\":\"test data\"}", "0")

      Assertions.assertNotNull(block.hashcode)
      Assertions.assertEquals(128, block.hashcode.length)
    }

    @Test
    @DisplayName("shall have a mining method")
    fun verify_mining() {
      val block = Block(Instant.now().epochSecond, "FOO").mine(3)
      Assertions.assertEquals("000", block.hashcode.substring(0, 3))
    }

    @Test
    @DisplayName("shall contain the hash of its predecessor block")
    fun verify_chain_of_blocks() {
      val genesisBlock = Block(Instant.now().epochSecond, "{\"data\":\"D0\"}")
      val firstBlock = Block(Instant.now().epochSecond, "{\"data\":\"D1\"}", genesisBlock.hashcode)
      val secondBlock = Block(Instant.now().epochSecond, "{\"data\":\"D2\"}", firstBlock.hashcode)

      System.out.println(listOf(genesisBlock, firstBlock, secondBlock).map { it ->
        it.toString().plus(" - ").plus(it.hashcode)
      }.joinToString(separator = "\n"))

      Assertions.assertEquals(firstBlock.previousHash, genesisBlock.hashcode)
      Assertions.assertEquals(secondBlock.previousHash, firstBlock.hashcode)
    }
  }

  @Test
  @DisplayName("shall provide a method to add new blocks to the chain")
  fun verify_zero_chain_add_method() {
    val zc = TestChain()
    val b1 = zc.add(Block.of("\"data\":\"D1\""))
    val b2 = zc.add(Block.of("\"data\":\"D2\""))

    Assertions.assertEquals(b1.previousHash, "")
    Assertions.assertEquals(b2.previousHash, b1.hashcode)
  }

  @Test
  @DisplayName("shall provide a validate function which returns true if the blockchain is valid")
  fun verify_chain_validation() {
    val zc = TestChain()
    zc.add(Block.of("\"data\":\"D1\""))
    zc.add(Block.of("\"data\":\"D2\""))

    Assertions.assertTrue(zc.verify())
  }

  @Test
  @DisplayName("shall provide a validate function which returns false if the blockchain has a manipulated block")
  fun verify_chain_validation_fail_if_a_block_gets_manipulated() {
    val zc = TestChain()
    zc.add(Block.of("\"data\":\"D1\""))
    zc.add(Block.of("\"data\":\"D2\""))

    val block = zc.getBlockchain()[1]
    val dataField = block.javaClass.getDeclaredField("data")
    dataField.isAccessible = true
    dataField.set(block, "FOO")

    Assertions.assertNotEquals(block.hashcode, BlockChain.calculateHash(block::toShortString))
    Assertions.assertFalse(zc.verify())
  }
}
