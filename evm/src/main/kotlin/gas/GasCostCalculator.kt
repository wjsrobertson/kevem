package org.kevm.evm.gas

import org.kevm.evm.EIP
import org.kevm.evm.Opcode
import org.kevm.evm.bytesToBigInteger
import org.kevm.evm.model.Address
import org.kevm.evm.model.Byte
import org.kevm.evm.model.Byte.Companion.padRightToSize
import org.kevm.evm.model.ExecutionContext
import org.kevm.evm.model.Features
import org.kevm.evm.numbers.BigIntMath.max
import org.kevm.evm.ops.CallArguments
import java.math.BigInteger
import kotlin.math.ceil

class GasCostCalculator(
    private val baseGasCostCalculator: BaseGasCostCalculator,
    private val memoryUsageCostCalculator: MemoryUsageGasCostCalculator
) {
    fun calculateCost(opcode: Opcode, executionCtx: ExecutionContext): BigInteger {
        val baseCost = baseGasCostCalculator.baseCost(opcode, executionCtx)
        val memCost = memoryUsageCostCalculator.memoryUsageCost(opcode, executionCtx)

        return baseCost + memCost
    }
}

class MemoryUsageGasCalc {

    /**
     * C_mem in the yellow paper
     */
    fun memoryCost(highestByteIndex: Int): Int {
        val numWords = Math.ceil(highestByteIndex / 32.toDouble()).toInt()

        return (GasCost.Memory.cost * numWords) + ((numWords * numWords) / 512)
    }
}

class CallGasCostCalc {
    fun calcCallCostAndCallGas(
        value: BigInteger,
        to: Address,
        gas: BigInteger,
        executionCtx: ExecutionContext
    ): Pair<BigInteger, BigInteger> {
        val newAccountFee =
            if (value > BigInteger.ZERO && !executionCtx.accounts.accountExists(to)) GasCost.NewAccountEip150.cost
            else 0

        val transferFee =
            if (value > BigInteger.ZERO) GasCost.CallValue.cost
            else 0

        val extraFee = (GasCost.Call.cost + newAccountFee + transferFee).toBigInteger()

        // TODO - gas cap is in the yellow paper but not in parity or ganache - look into this maybe an EIP defines different behvaiour
        /*
        val callerGas = executionCtx.currentCallCtx.gas
        val gasCap =
            if (callerGas > extraFee) BigIntMath.min(allButOne64th(callerGas - extraFee), gas)
            else gas
            */
        val gasCap = gas

        val callGas =
            if (value > BigInteger.ZERO) gasCap + GasCost.CallStipend.cost.toBigInteger()
            else gasCap

        val callCost = gasCap + extraFee

        return Pair(callCost, callGas)
    }

    /**
     * L(n) in the yellow papaer (298)
     */
    private fun allButOne64th(num: BigInteger) = num - num / BigInteger("64")
}

class PredefinedContractGasCostCalc {
    fun calcGasCost(args: CallArguments, executionCtx: ExecutionContext): BigInteger {
        val (data, _) = executionCtx.memory.read(args.inLocation, args.inSize)
        val numBytes = data.size
        val numWords = BigInteger.valueOf(ceil(numBytes / 32.toDouble()).toLong())

        // TODO - move gas cost magic numbers to gas enum
        return when (args.address.value.toInt()) {
            1 -> BigInteger("3000")
            2 -> BigInteger("60") + BigInteger("12") * numWords
            3 -> BigInteger("600") + BigInteger("120") * numWords
            4 -> BigInteger("15") + BigInteger("3") * numWords
            5 -> expModGasCost(data)
            6 ->
                if (executionCtx.features.isEnabled(EIP.EIP1108)) BigInteger("150")
                else BigInteger("500")
            7 ->
                if (executionCtx.features.isEnabled(EIP.EIP1108)) BigInteger("6000")
                else BigInteger("40000")
            8 -> snarkvGasCost(numBytes, executionCtx.features)
            else -> TODO()
        }
    }

    private fun expModGasCost(data: List<Byte>): BigInteger {
        val (bSize, eSize, mSize) = padRightToSize(data, 96).chunked(32).map { bytesToBigInteger(it) }
        val eSizeFactor = max(eSize, BigInteger.ONE)
        val mbSizeFactor = max(mSize, bSize)
        val mbSizeFactorSq = mbSizeFactor * mbSizeFactor

        val fFactor = when {
            mbSizeFactor <= BigInteger("64") ->
                mbSizeFactorSq
            mbSizeFactor <= BigInteger("1024") ->
                (mbSizeFactorSq / BigInteger("4")) + BigInteger("96") * mbSizeFactor - BigInteger("3072")
            else ->
                (mbSizeFactorSq / BigInteger("16")) + BigInteger("480") * mbSizeFactor - BigInteger("199680")
        }

        return (fFactor * eSizeFactor) / GasCost.QuadDivisor.costBigInt
    }

    private fun snarkvGasCost(numBytes: Int, features: Features): BigInteger {
        val baseCost =
            if (features.isEnabled(EIP.EIP1108)) BigInteger("45000")
            else BigInteger("100000")

        val perPairCost =
            if (features.isEnabled(EIP.EIP1108)) BigInteger("34000")
            else BigInteger("80000")

        val numSnarkPairs = (numBytes / 192).toBigInteger()
        return (perPairCost * numSnarkPairs) + baseCost
    }
}
