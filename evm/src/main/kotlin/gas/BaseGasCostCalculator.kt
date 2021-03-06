package org.kevem.evm.gas

import org.kevem.evm.EIP
import org.kevem.evm.Opcode
import org.kevem.evm.model.ExecutionContext
import org.kevem.evm.Opcode.*
import org.kevem.evm.model.Word
import org.kevem.evm.numbers.BigIntMath
import org.kevem.evm.numbers.log256
import java.math.BigInteger
import org.kevem.evm.ops.CallOps
import org.kevem.evm.PrecompiledContractExecutor as Precompiled

class BaseGasCostCalculator(
    private val callGasCostCalc: CallGasCostCalc,
    private val predefinedContractGasCostCalc: PredefinedContractGasCostCalc
) {

    fun baseCost(opcode: Opcode, executionContext: ExecutionContext): BigInteger =
        if (opcode.cost == GasCost.Formula) {
            when (opcode) {
                EXP -> expCost(executionContext)
                SHA3 -> sha3Cost(executionContext)
                CALLDATACOPY -> dataCopyCost(executionContext)
                CODECOPY -> dataCopyCost(executionContext)
                EXTCODECOPY -> extCodeCopyCost(executionContext)
                RETURNDATACOPY -> dataCopyCost(executionContext)
                SSTORE -> sStoreCost(executionContext)
                SLOAD -> sLoadCost(executionContext)
                LOG0 -> logCost(executionContext, 0)
                LOG1 -> logCost(executionContext, 1)
                LOG2 -> logCost(executionContext, 2)
                LOG3 -> logCost(executionContext, 3)
                LOG4 -> logCost(executionContext, 4)
                CALL -> callCost(executionContext, true)
                CALLCODE -> callCost(executionContext, true)
                DELEGATECALL -> callCost(executionContext, false)
                STATICCALL -> callCost(executionContext, false)
                SUICIDE -> suicideCost(executionContext)
                BALANCE -> balanceCost(executionContext)
                EXTCODEHASH -> extCodeHashCost(executionContext)
                else -> throw RuntimeException("Don't now how to compute gas cost for $opcode")
            }
        } else opcode.cost.cost.toBigInteger()

    private fun extCodeHashCost(executionContext: ExecutionContext): BigInteger = when {
        executionContext.config.features.isEnabled(EIP.EIP1884) -> GasCost.ExtCodeHashEip1884.costBigInt
        else -> GasCost.ExtCodeHashEip1052.costBigInt
    }

    private fun balanceCost(executionContext: ExecutionContext): BigInteger = when {
        executionContext.config.features.isEnabled(EIP.EIP1884) -> GasCost.BalanceEip1884.costBigInt
        else -> GasCost.BalanceHomestead.costBigInt
    }

    private fun callCost(executionContext: ExecutionContext, withValue: Boolean): BigInteger {
        val callArgs = CallOps.peekCallArgsFromStack(executionContext.stack, withValue)

        return with(callArgs) {
            if (Precompiled.isPrecompiledContractCall(address)) {
                predefinedContractGasCostCalc.calcGasCost(callArgs, executionContext)
            } else {
                callGasCostCalc.calcCallCostAndCallGas(value, address, gas, executionContext).first
            }
        }
    }

    /**
     * 5000 + ((create_new_account) ? 25000 : 0)
     */
    private fun suicideCost(executionContext: ExecutionContext): BigInteger {
        val (address) = executionContext.currentCallCtx.stack.peekWords(1).map { it.toAddress() }

        val accountExists = executionContext.accounts.accountExists(address)

        val (selfDestructCost, newAccountCost) =
            if (executionContext.config.features.isEnabled(EIP.EIP150))
                Pair(GasCost.SelfDestructEip150, GasCost.NewAccountEip150)
            else
                Pair(GasCost.SelfDestructHomestead, GasCost.NewAccountHomestead)

        val newAccountCharge = if (accountExists) BigInteger.ZERO else newAccountCost.costBigInt
        return selfDestructCost.costBigInt + newAccountCharge
    }

    /**
     * 375 + 8 * (number of bytes in log data)
     */
    private fun logCost(executionContext: ExecutionContext, numTopics: Int): BigInteger {
        val elements = executionContext.currentCallCtx.stack.peekWords(2 + numTopics)
        val size = elements.take(2).last().toInt()

        return (GasCost.Log.cost +
                GasCost.LogTopic.cost * numTopics +
                GasCost.LogData.cost * size).toBigInteger()
    }

    /**
     * ((value != 0) && (storage_location == 0)) ? 20000 : 5000
     */
    private fun sStoreCost(executionContext: ExecutionContext): BigInteger {
        val (location, newValue) = executionContext.currentCallCtx.stack.peekWords(2)

        val storageAddress = executionContext.currentCallCtx.storageAddress
            ?: throw RuntimeException("can't determine contract address")
        val currentValue = executionContext.accounts.storageAt(storageAddress, location.toBigInt())
        val originalValue = executionContext.originalAccounts.storageAt(storageAddress, location.toBigInt())

        return if (executionContext.config.features.isEnabled(EIP.EIP2200)) {
            eip2200SStoreCost(currentValue, newValue, originalValue)
        } else {
            homesteadSStoreCost(newValue, currentValue)
        }
    }

    private fun eip2200SStoreCost(currentValue: Word, newValue: Word, originalValue: Word): BigInteger =
        if (currentValue == newValue)
            GasCost.SLoadEip2200.costBigInt
        else {
            if (originalValue == currentValue) {
                if (originalValue == Word.Zero)
                    GasCost.SStoreSet.costBigInt
                else
                    GasCost.SStoreReset.costBigInt
            } else
                GasCost.SLoadEip2200.costBigInt
        }

    private fun homesteadSStoreCost(newValue: Word, currentValue: Word): BigInteger =
        if (newValue != Word.Zero && currentValue == Word.Zero)
            GasCost.SSet.costBigInt
        else
            GasCost.SReset.costBigInt

    private fun sLoadCost(executionContext: ExecutionContext): BigInteger = when {
        executionContext.config.features.isEnabled(EIP.EIP1884) -> GasCost.SLoadEip1884.costBigInt
        executionContext.config.features.isEnabled(EIP.EIP150) -> GasCost.SLoadEip150.costBigInt
        else -> GasCost.SLoadHomestead.costBigInt
    }

    /**
     * 700 + 3 * (number of words copied, rounded up)
     */
    private fun extCodeCopyCost(executionContext: ExecutionContext): BigInteger {
        val (_, _, _, size) = executionContext.currentCallCtx.stack.peekWords(4)

        return GasCost.ExtCode.costBigInt + GasCost.Copy.costBigInt * numWordsRoundedUp(size.toBigInt())
    }

    private fun dataCopyCost(executionContext: ExecutionContext): BigInteger {
        val (_, _, size) = executionContext.currentCallCtx.stack.peekWords(3)

        return GasCost.VeryLow.costBigInt + GasCost.Copy.costBigInt * numWordsRoundedUp(size.toBigInt())
    }

    /**
     * 30 + 6 * (size of input in words)
     */
    private fun sha3Cost(executionContext: ExecutionContext): BigInteger {
        val (_, size) = executionContext.currentCallCtx.stack.peekWords(2)
        val numWords = numWordsRoundedUp(size.toBigInt())

        return GasCost.Sha3.costBigInt + GasCost.Sha3Word.costBigInt * numWords
    }

    /**
     * (exp == 0) ? 10 : (10 + 50 * (1 + log256(exp)))
     */
    private fun expCost(executionContext: ExecutionContext): BigInteger {
        val elements = executionContext.currentCallCtx.stack.peekWords(2)
        val (_, exp) = elements.map { it.toBigInt() }

        val byteCost =
            if (executionContext.config.features.isEnabled(EIP.EIP160)) GasCost.ExpByteEip160
            else GasCost.ExpByteHomestead

        return if (exp == BigInteger.ZERO) GasCost.Exp.costBigInt
        else GasCost.Exp.costBigInt + byteCost.costBigInt * (BigInteger.ONE + log256(exp))
    }

    private fun numWordsRoundedUp(numBytes: BigInteger) = BigIntMath.divRoundUp(numBytes, BigInteger("32"))
}