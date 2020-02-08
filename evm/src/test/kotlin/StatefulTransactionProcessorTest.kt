import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.assertj.core.api.Assertions.assertThat
import org.kevm.evm.Executor
import org.kevm.evm.StatefulTransactionProcessor
import org.kevm.evm.TransactionProcessor
import org.kevm.evm.gas.*
import org.kevm.evm.model.Address
import org.kevm.evm.model.Byte
import org.kevm.evm.model.TransactionMessage
import test.TestObjects
import java.math.BigInteger

private val Zero = BigInteger.ZERO
private val One = BigInteger.ONE

class StatefulTransactionProcessorTest {

    var underTest = createStp()

    @BeforeEach
    internal fun setUp() {
        underTest = createStp()
        underTest.setAutoMine(false)
    }

    @Test
    internal fun `check two pending transactions get mined`() {
        val messages = initForTwoTransfers()

        mine(messages)

        val lastBlock = underTest.getWorldState().blocks.last()
        assertThat(lastBlock.transactions).hasSize(2)
    }

    @Test
    internal fun `check only one block added when two pending transactions get mined`() {
        val messages = initForTwoTransfers()

        val numBlocksBefore = underTest.getWorldState().blocks.size
        mine(messages)
        val numBlocksAfter = underTest.getWorldState().blocks.size

        assertThat(numBlocksAfter - numBlocksBefore).isEqualTo(1)
    }

    @Test
    internal fun `check process mines when automine true`() {
        underTest.setAutoMine(true)
        val message = initForTransfer()

        val receipt = underTest.process(message)

        assertTxWasMined(receipt.hash)
    }

    @Test
    internal fun `check process does not mine when automine false`() {
        underTest.setAutoMine(false)
        val message = initForTransfer()

        val receipt = underTest.process(message)

        assertTxWasNotMined(receipt.hash)
    }

    private fun assertTxWasMined(hash: List<Byte>) {
        val hashes = underTest.getWorldState().blocks.flatMap { it.transactions }.map { it.message.hash }
        assertThat(hashes).contains(hash)
    }

    private fun assertTxWasNotMined(hash: List<Byte>) {
        val hashes = underTest.getWorldState().blocks.flatMap { it.transactions }.map { it.message.hash }
        assertThat(hashes).doesNotContain(hash)
    }

    private fun createStp(): StatefulTransactionProcessor {
        val executor = Executor(
            GasCostCalculator(
                BaseGasCostCalculator(CallGasCostCalc(), PredefinedContractGasCostCalc()),
                MemoryUsageGasCostCalculator(MemoryUsageGasCalc())
            )
        )

        val tp = TransactionProcessor(executor)
        return StatefulTransactionProcessor(tp, TestObjects.clock, TestObjects.worldState)
    }

    private fun addBalance(address: Address, amount: BigInteger) {
        val worldState = underTest.getWorldState()

        val balance = worldState.accounts.balanceOf(address)
        val accounts = worldState.accounts.updateBalance(address, balance + amount)

        val newWorldState = worldState.copy(accounts = accounts)

        underTest.setWorldState(newWorldState)
    }

    private fun initForTransfer(): TransactionMessage {
        addBalance(Address("0x1"), BigInteger("21000"))

        return TransactionMessage(Address("0x1"), Address("0x2"), Zero, One, BigInteger("21000"), emptyList(), Zero)
    }

    private fun initForTwoTransfers(): List<TransactionMessage> {
        addBalance(Address("0x1"), BigInteger("21000"))
        addBalance(Address("0x3"), BigInteger("21000"))

        val messages = listOf(
            TransactionMessage(Address("0x1"), Address("0x2"), Zero, One, BigInteger("21000"), emptyList(), Zero),
            TransactionMessage(Address("0x3"), Address("0x4"), Zero, One, BigInteger("21000"), emptyList(), Zero)
        )
        return messages
    }

    private fun mine(messages: List<TransactionMessage>) {
        messages.forEach {
            underTest.enqueTransaction(it)
        }
        underTest.mine()
    }
}