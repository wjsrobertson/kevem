package org.kevem.ethereumtests

import com.fasterxml.jackson.core.type.TypeReference
import org.assertj.core.api.Assertions.assertThat

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.kevem.evm.Executor
import org.kevem.evm.HardFork
import org.kevem.evm.collections.BigIntegerIndexedList
import org.kevem.evm.crypto.sha256
import org.kevem.evm.gas.*
import org.kevem.evm.model.*
import org.kevem.common.Byte
import org.kevem.common.conversions.toByteList
import java.lang.Exception
import java.math.BigInteger
import java.time.Instant
import org.kevem.common.conversions.*

class VMTestCaseRunnerTest {

    private val executor = createExecutor()

    @DisplayName("ethereum-test VMTest pack")
    @ParameterizedTest(name = "{0}")
    @MethodSource(value = ["testCases"])
    internal fun `ethereum-test VMTest pack`(testCase: VMTestCase): Unit = with(testCase) {

        val executionContext = createExecutionContext(testCase)

        val executed = try {
            executor.executeAll(executionContext)
        } catch (e: Exception) {
            fail("$testCase failed with ${e.message}", e)
        }

        if (post != null) {
            assertPostAccountsMatch(parseAccounts(post), executed)
        }
        if (out != null) {
            assertOutDataMatches(out, executed)
        }
        if (gas != null) {
            assertGasMatches(gas, executionContext, executed)
        }
    }

    private fun assertGasMatches(
        gas: String?,
        executionContext: ExecutionContext,
        executed: ExecutionContext
    ) {
        val gasRemaining = (executionContext.currentCallCtx.gas - executed.gasUsed).toString(16)
        assertThat(gasRemaining).isEqualTo(toBigIntegerOrNull(gas)?.toString(16))
    }

    companion object {
        private const val testsRoot = "ethereum-tests-pack/VMTests"

        private val loader = TestCaseLoader(
            TestCaseParser(object :
                TypeReference<Map<String, VMTestCase>>() {}, testsRoot), testsRoot
        )

        @JvmStatic
        fun testCases() = loader.loadTestCases()
    }

    private fun assertOutDataMatches(out: String?, executed: ExecutionContext) =
        assertThat(executed.lastReturnData).isEqualTo(BigIntegerIndexedList.fromBytes(
            toByteList(
                out
            )
        ))

    private fun assertPostAccountsMatch(accounts: Accounts, executed: ExecutionContext) =
        accounts.list().forEach { a ->
            val account: Account = executed.accounts.list().find { it.address == a.address }
                ?: fail("no account with address ${a.address}")

            assertThat(a).isEqualTo(account)
        }

    private fun createExecutionContext(testCase: VMTestCase): ExecutionContext = with(testCase) {
        val accounts = parseAccounts(pre)

        val nextBlock = Block(
            number = BigInteger.ONE,
            difficulty = toBigInteger(env.currentDifficulty),
            gasLimit = toBigInteger(env.currentGasLimit),
            timestamp = Instant.ofEpochSecond(1)
        )

        val transaction = Transaction(
            origin = Address(exec.origin),
            gasPrice = toBigInteger(exec.gasPrice)
        )

        val callContext = CallContext(
            caller = Address(exec.caller),
            callData = BigIntegerIndexedList.fromByteString(exec.data),
            type = CallType.CALL,
            value = toBigInteger(exec.value),
            code = BigIntegerIndexedList.fromByteString(exec.code),
            gas = toBigInteger(exec.gas),
            storageAddress = Address(exec.address),
            contractAddress = Address(exec.address)
        )

        return ExecutionContext(
            currentBlock = nextBlock,
            currentTransaction = transaction,
            callStack = listOf(callContext),
            accounts = accounts,
            previousBlocks = mapOf(Pair(BigInteger.ONE, Word(sha256(listOf(Byte(1)))))),
            config = EvmConfig(
                coinbase = Address(env.currentCoinbase),
                features = Features(HardFork.Homestead.eips())
            )
        )
    }

    private fun createExecutor(): Executor =
        Executor(
            GasCostCalculator(
                BaseGasCostCalculator(CallGasCostCalc(), PredefinedContractGasCostCalc()),
                MemoryUsageGasCostCalculator(
                    MemoryUsageGasCalc()
                )
            )
        )

    private fun parseAccounts(post: Map<String, VMTestCaseAccount>?): Accounts {
        val accountList = post?.map { entry ->
            val (a, d) = entry

            val contract =
                if (d.code != "0x") Contract(
                    code = BigIntegerIndexedList.fromByteString(d.code),
                    storage = Storage(
                        d.storage.map { e ->
                            val (k, v) = e
                            Pair(toBigInteger(k), Word.coerceFrom(v))
                        }.toMap()
                    )
                ) else null


            Account(
                Address(a),
                toBigInteger(d.balance),
                contract,
                nonce = toBigInteger(d.nonce)
            )
        } ?: emptyList()

        return Accounts(accountList)
    }
}