package com.gammadex.kevin.evm

import com.gammadex.kevin.evm.model.*
import com.gammadex.kevin.evm.lang.*

fun uniStackOp(executionContext: ExecutionContext, op: (w1: Word) -> Word): ExecutionContext {
    val (a, newStack) = executionContext.stack.popWord()
    val finalStack = newStack.pushWord(op(a))

    return executionContext.updateCurrentCallContext(stack = finalStack)
}

fun biStackOp(executionContext: ExecutionContext, op: (w1: Word, w2: Word) -> Word): ExecutionContext {
    val (elements, newStack) = executionContext.stack.popWords(2)
    val (a, b) = elements
    val finalStack = newStack.pushWord(op(a, b))

    return executionContext.updateCurrentCallContext(stack = finalStack)
}

fun triStackOp(executionContext: ExecutionContext, op: (w1: Word, w2: Word, w3: Word) -> Word): ExecutionContext {
    val (elements, newStack) = executionContext.stack.popWords(3)
    val (a, b, c) = elements
    val finalStack = newStack.pushWord(op(a, b, c))

    return executionContext.updateCurrentCallContext(stack = finalStack)
}

class Executor {
    fun execute(currentContext: ExecutionContext, startContext: ExecutionContext): ExecutionContext =
        with(currentContext) {
            // TODO - handle end of contract
            val opcode = Opcode.byCode[currentCallContext.contract[currentLocation]]

            // TODO increment contract pointer
            // TODO deduct gas
            // TODO - maximum stack size

            when (opcode) {
                Opcode.STOP -> currentContext.copy(completed = true)
                Opcode.ADD -> biStackOp(currentContext, VmMath::add)
                Opcode.MUL -> biStackOp(currentContext, VmMath::mul)
                Opcode.SUB -> biStackOp(currentContext, VmMath::sub)
                Opcode.DIV -> biStackOp(currentContext, VmMath::div)
                Opcode.SDIV -> biStackOp(currentContext, VmMath::sdiv)
                Opcode.MOD -> biStackOp(currentContext, VmMath::mod)
                Opcode.SMOD -> biStackOp(currentContext, VmMath::smod)
                Opcode.ADDMOD -> triStackOp(currentContext, VmMath::addMod)
                Opcode.MULMOD -> triStackOp(currentContext, VmMath::mulMod)
                Opcode.EXP -> biStackOp(currentContext, VmMath::exp)
                Opcode.SIGNEXTEND -> biStackOp(currentContext, VmMath::signExtend)
                Opcode.LT -> biStackOp(currentContext, VmMath::lt)
                Opcode.GT -> biStackOp(currentContext, VmMath::gt)
                Opcode.SLT -> biStackOp(currentContext, VmMath::slt)
                Opcode.SGT -> biStackOp(currentContext, VmMath::sgt)
                Opcode.EQ -> biStackOp(currentContext, VmMath::eq)
                Opcode.ISZERO -> uniStackOp(currentContext, VmMath::isZero)
                Opcode.AND -> biStackOp(currentContext, VmMath::and)
                Opcode.OR -> biStackOp(currentContext, VmMath::or)
                Opcode.XOR -> biStackOp(currentContext, VmMath::xor)
                Opcode.NOT -> uniStackOp(currentContext, VmMath::not)
                Opcode.BYTE -> biStackOp(currentContext, VmMath::byte)
                Opcode.SHL -> biStackOp(currentContext, VmMath::shl)
                Opcode.SHR -> biStackOp(currentContext, VmMath::shr)
                Opcode.SAR -> biStackOp(currentContext, VmMath::sar)
                Opcode.SHA3 -> biStackOp(currentContext) { a, b ->
                    val bytes = memory.get(a.toInt(), b.toInt())
                    keccak256(bytes)
                }
                Opcode.ADDRESS -> {
                    val call = callStack.last()
                    val newStack = stack.pushWord(Word.coerceFrom(call.contract.address.value))

                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.BALANCE -> {
                    val (popped, newStack) = stack.popWord()
                    val balance = evmState.balanceOf(popped.toAddress())
                    val finalStack = newStack.pushWord(Word.coerceFrom(balance))

                    currentContext.updateCurrentCallContext(stack = finalStack)
                }
                Opcode.ORIGIN -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentTransaction.origin.value))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.CALLER -> {
                    val call = callStack.last { it.type != CallType.DELEGATECALL }
                    val newStack = stack.pushWord(call.caller.toWord())

                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.CALLVALUE -> {
                    val call = callStack.last { it.type != CallType.DELEGATECALL }
                    val newStack = stack.pushWord(Word.coerceFrom(call.value))

                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.CALLDATALOAD -> {
                    val call = callStack.last()

                    val (position, newStack) = stack.popWord()
                    val start = position.toInt().coerceIn(0, call.callData.size)
                    val end = (position.toInt() + 32).coerceIn(call.callData.size, 32)

                    val data = call.callData.subList(start, end)
                    val finalStack = newStack.pushWord(Word.coerceFrom(data))

                    currentContext.updateCurrentCallContext(stack = finalStack)
                }
                Opcode.CALLDATASIZE -> {
                    val size = callStack.last().callData.size
                    val newStack = stack.pushWord(Word.coerceFrom(size))

                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.CALLDATACOPY -> {
                    // TODO - how should it handle out of range ?
                    val (elements, newStack) = stack.popWords(3)
                    val (to, from, size) = elements.map { it.toInt() }
                    val call = callStack.last()
                    val data = call.callData.subList(from, from + size)
                    val newMemory = memory.set(to, data)

                    currentContext.updateCurrentCallContext(stack = newStack, memory = newMemory)
                }
                Opcode.CODESIZE -> {
                    // TODO - what should hapen if it is a DELEGATECALL?
                    val call = callStack.last()
                    val newStack = stack.pushWord(Word.coerceFrom(call.contract.code.size))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.CODECOPY -> {
                    // TODO - how should it handle out of range ?
                    val (elements, newStack) = stack.popWords(3)
                    val (to, from, size) = elements.map { it.toInt() }
                    val call = callStack.last()
                    val data = call.contract.code.subList(from, from + size)
                    val newMemory = memory.set(to, data)

                    currentContext.updateCurrentCallContext(stack = newStack, memory = newMemory)
                }
                Opcode.GASPRICE -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentTransaction.gasPrice))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.EXTCODESIZE -> {
                    val (popped, newStack) = stack.popWord()
                    val code = evmState.codeAt(popped.toAddress())
                    val finalStack = newStack.pushWord(Word.coerceFrom(code.size))

                    currentContext.updateCurrentCallContext(stack = finalStack)
                }
                Opcode.EXTCODECOPY -> {
                    // TODO - how should it handle out of range ?
                    val (elements, newStack) = stack.popWords(4)
                    val (address, to, from, size) = elements

                    val code = evmState.codeAt(address.toAddress())
                    val data = code.subList(from.toInt(), from.toInt() + size.toInt())
                    val newMemory = memory.set(to.toInt(), data)

                    currentContext.updateCurrentCallContext(stack = newStack, memory = newMemory)
                }
                Opcode.RETURNDATASIZE -> {
                    val newStack = stack.pushWord(Word.coerceFrom(lastReturnData.size))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.RETURNDATACOPY -> {
                    // TODO - how should it handle out of range ?
                    val (elements, newStack) = stack.popWords(3)
                    val (to, from, size) = elements.map { it.toInt() }
                    val data = lastReturnData.subList(from, from + size)
                    val newMemory = memory.set(to, data)

                    currentContext.updateCurrentCallContext(stack = newStack, memory = newMemory)
                }
                Opcode.BLOCKHASH -> uniStackOp(currentContext) {
                    previousBlocks.getOrDefault(it.toBigInt(), Word.Zero)
                }
                Opcode.COINBASE -> {
                    val newStack = stack.pushWord(coinBase.toWord())
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.TIMESTAMP -> {
                    val epoch = clock.instant().epochSecond
                    val newStack = stack.pushWord(Word.coerceFrom(epoch))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.NUMBER -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentBlock.number))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.DIFFICULTY -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentBlock.difficulty))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.GASLIMIT -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentBlock.gasLimit))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.POP -> {
                    // TODO - what if stack is empty
                    val (_, newStack) = stack.pop()
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.MLOAD -> {
                    val (word, newStack) = stack.popWord()
                    val data = memory.get(word.toInt(), 32)
                    val finalStack = newStack.pushWord(Word(data))

                    currentContext.updateCurrentCallContext(stack = finalStack)
                }
                Opcode.MSTORE -> {
                    val (elements, newStack) = stack.popWords(2)
                    val (p, v) = elements
                    val newMemory = memory.set(p.toInt(), v.data)

                    currentContext.updateCurrentCallContext(stack = newStack, memory = newMemory)
                }
                Opcode.MSTORE8 -> {
                    val (v, newStack) = stack.pop()
                    val (p, newStack2) = newStack.popWord()
                    val newMemory = memory.set(p.toInt(), v.take(1))

                    currentContext.updateCurrentCallContext(stack = newStack2, memory = newMemory)
                }
                Opcode.SLOAD -> {
                    val (word, newStack) = stack.popWord()
                    val index = word.toInt()
                    val finalStack = newStack.pushWord(storage[index])

                    currentContext.updateCurrentCallContext(stack = finalStack)
                }
                Opcode.SSTORE -> {
                    val (elements, newStack) = stack.popWords(2)
                    val (a, v) = elements
                    val newStorage = storage.set(a.toInt(), v)

                    currentContext.updateCurrentCallContext(stack = newStack, storage = newStorage)
                }
                Opcode.JUMP -> {
                    val (to, newStack) = stack.popWord()
                    val toLocation = to.toInt()
                    val call = callStack.last()
                    val nextOpCode = Opcode.byCode[call.contract[toLocation]]

                    if (nextOpCode == Opcode.JUMPDEST) {
                        currentContext.updateCurrentCallContext(stack = newStack, currentLocation = toLocation)
                    } else {
                        TODO("handle invalid jump destination")
                    }
                }
                Opcode.JUMPI -> {
                    val (elements, newStack) = stack.popWords(2)
                    val (to, condition) = elements

                    if (condition.toBoolean()) {
                        val call = callStack.last()
                        val toLocation = to.toInt()
                        val nextOpCode = Opcode.byCode[call.contract[toLocation]]

                        if (nextOpCode == Opcode.JUMPDEST) {
                            currentContext.updateCurrentCallContext(stack = newStack, currentLocation = toLocation)
                        } else {
                            TODO("handle invalid jump destination")
                        }
                    } else {
                        currentContext.updateCurrentCallContext(stack = newStack)
                    }
                }
                Opcode.PC -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentLocation))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.MSIZE -> {
                    val size = memory.maxIndex()?.plus(1) ?: 0
                    val newStack = stack.pushWord(Word.coerceFrom(size))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.GAS -> {
                    val newStack = stack.pushWord(Word.coerceFrom(currentCallContext.gasRemaining))
                    currentContext.updateCurrentCallContext(stack = newStack)
                }
                Opcode.JUMPDEST -> {
                    currentContext
                }
                Opcode.PUSH1 -> push(currentContext, 1)
                Opcode.PUSH2 -> push(currentContext, 2)
                Opcode.PUSH3 -> push(currentContext, 3)
                Opcode.PUSH4 -> push(currentContext, 4)
                Opcode.PUSH5 -> push(currentContext, 5)
                Opcode.PUSH6 -> push(currentContext, 6)
                Opcode.PUSH7 -> push(currentContext, 7)
                Opcode.PUSH8 -> push(currentContext, 8)
                Opcode.PUSH9 -> push(currentContext, 9)
                Opcode.PUSH10 -> push(currentContext, 10)
                Opcode.PUSH11 -> push(currentContext, 11)
                Opcode.PUSH12 -> push(currentContext, 12)
                Opcode.PUSH13 -> push(currentContext, 13)
                Opcode.PUSH14 -> push(currentContext, 14)
                Opcode.PUSH15 -> push(currentContext, 15)
                Opcode.PUSH16 -> push(currentContext, 16)
                Opcode.PUSH17 -> push(currentContext, 17)
                Opcode.PUSH18 -> push(currentContext, 18)
                Opcode.PUSH19 -> push(currentContext, 19)
                Opcode.PUSH20 -> push(currentContext, 20)
                Opcode.PUSH21 -> push(currentContext, 21)
                Opcode.PUSH22 -> push(currentContext, 22)
                Opcode.PUSH23 -> push(currentContext, 23)
                Opcode.PUSH24 -> push(currentContext, 24)
                Opcode.PUSH25 -> push(currentContext, 25)
                Opcode.PUSH26 -> push(currentContext, 26)
                Opcode.PUSH27 -> push(currentContext, 27)
                Opcode.PUSH28 -> push(currentContext, 28)
                Opcode.PUSH29 -> push(currentContext, 29)
                Opcode.PUSH30 -> push(currentContext, 30)
                Opcode.PUSH31 -> push(currentContext, 31)
                Opcode.PUSH32 -> push(currentContext, 32)
                Opcode.DUP1 -> dup(currentContext, 0)
                Opcode.DUP2 -> dup(currentContext, 1)
                Opcode.DUP3 -> dup(currentContext, 2)
                Opcode.DUP4 -> dup(currentContext, 3)
                Opcode.DUP5 -> dup(currentContext, 4)
                Opcode.DUP6 -> dup(currentContext, 5)
                Opcode.DUP7 -> dup(currentContext, 6)
                Opcode.DUP8 -> dup(currentContext, 7)
                Opcode.DUP9 -> dup(currentContext, 8)
                Opcode.DUP10 -> dup(currentContext, 9)
                Opcode.DUP11 -> dup(currentContext, 10)
                Opcode.DUP12 -> dup(currentContext, 11)
                Opcode.DUP13 -> dup(currentContext, 12)
                Opcode.DUP14 -> dup(currentContext, 13)
                Opcode.DUP15 -> dup(currentContext, 14)
                Opcode.DUP16 -> dup(currentContext, 15)
                Opcode.SWAP1 -> swap(currentContext, 0)
                Opcode.SWAP2 -> swap(currentContext, 1)
                Opcode.SWAP3 -> swap(currentContext, 2)
                Opcode.SWAP4 -> swap(currentContext, 3)
                Opcode.SWAP5 -> swap(currentContext, 4)
                Opcode.SWAP6 -> swap(currentContext, 5)
                Opcode.SWAP7 -> swap(currentContext, 6)
                Opcode.SWAP8 -> swap(currentContext, 7)
                Opcode.SWAP9 -> swap(currentContext, 8)
                Opcode.SWAP10 -> swap(currentContext, 9)
                Opcode.SWAP11 -> swap(currentContext, 10)
                Opcode.SWAP12 -> swap(currentContext, 11)
                Opcode.SWAP13 -> swap(currentContext, 12)
                Opcode.SWAP14 -> swap(currentContext, 13)
                Opcode.SWAP15 -> swap(currentContext, 14)
                Opcode.SWAP16 -> swap(currentContext, 15)
                Opcode.LOG0 -> log(currentContext, 0)
                Opcode.LOG1 -> log(currentContext, 1)
                Opcode.LOG2 -> log(currentContext, 2)
                Opcode.LOG3 -> log(currentContext, 3)
                Opcode.LOG4 -> log(currentContext, 4)
                Opcode.CREATE -> {
                    // TODO - what if current contract doesn't have enough wei to send
                    // TODO - what if the generated address already exists
                    // TODO - subtract gas
                    val (elements, newStack) = stack.popWords(3)
                    val (v, p, s) = elements

                    val newContractCode = memory.get(p.toInt(), s.toInt())
                    val newContractAddress = addressGenerator.nextAddress()
                    val contract = Contract(newContractCode, newContractAddress)
                    val balance = v.toBigInt()
                    val currentAddress = currentCallContext.contract.address
                    val newEvmState = evmState
                        .updateBalanceAndContract(newContractAddress, balance, contract)
                        .updateBalance(currentAddress, evmState.balanceOf(currentAddress).subtract(balance))

                    val newStack2 = newStack.pushWord(newContractAddress.toWord())

                    currentContext
                        .copy(evmState = newEvmState)
                        .updateCurrentCallContext(stack = newStack2)
                }
                Opcode.CALL -> {
                    val (elements, newStack) = stack.popWords(7)
                    val (g, a, v, inLocation, inSize, outLocation, outSize) = elements

                    val value = v.toBigInt()
                    val gas = g.toBigInt()
                    val address = a.toAddress()

                    val callerBalance = evmState.balanceOf(currentCallContext.contract.address)
                    if (callerBalance < value) {
                        TODO("handle case where contract doesn't have enough funds")
                    }

                    if (currentCallContext.gasRemaining < gas) {
                        TODO("handle case where not enugh gas remaining")
                    }

                    val (destBalance, destContract) = evmState.balanceAndContractAt(address)
                    val newEvmState = evmState
                        .updateBalance(address, destBalance + value)

                    val startBalance = newEvmState.balanceOf(currentCallContext.contract.address)
                    val newEvmState2 = newEvmState
                        .updateBalance(currentCallContext.contract.address, startBalance - value)

                    val newCall = CallContext(
                        currentCallContext.contract.address,
                        memory.get(inLocation.toInt(), inSize.toInt()),
                        destContract ?: EmptyContract(address),
                        CallType.CALL,
                        value,
                        gas,
                        outLocation.toInt(),
                        outSize.toInt()
                    )

                    val updatedCtx = currentContext
                        .updateCurrentCallContext(
                            stack = newStack,
                            gasRemaining = currentCallContext.gasRemaining - gas
                        )

                    updatedCtx.copy(callStack = updatedCtx.callStack + newCall,
                            evmState = newEvmState2)
                }
                Opcode.CALLCODE -> {
                    val (elements, newStack) = stack.popWords(7)
                    val (g, a, v, inLocation, inSize, outLocation, outSize) = elements

                    val address = a.toAddress()
                    val nextContract = evmState.contractAt(address) ?: TODO()

                    val newCall = CallContext(
                        currentCallContext.contract.address,
                        memory.get(inLocation.toInt(), inSize.toInt()),
                        nextContract,
                        CallType.CALLCODE,
                        v.toBigInt(),
                        g.toBigInt(),
                        outLocation.toInt(),
                        outSize.toInt(),
                        storage = currentCallContext.storage
                    )

                    currentContext
                        .updateCurrentCallContext(
                            stack = newStack,
                            gasRemaining = currentCallContext.gasRemaining - g.toBigInt()
                        )
                        .copy(callStack = callStack + newCall)
                }
                Opcode.RETURN -> {
                    val (elements, _) = stack.popWords(2)
                    val (p, s) = elements.map { it.toInt() }

                    val remainingCalls = callStack.dropLast(1)

                    val returnData = memory.get(p, s)

                    val (newMemory, newStorage) =
                        if (currentCallContext.type == CallType.DELEGATECALL) // TODO
                            Pair(currentCallContext.memory, currentCallContext.storage)
                        else
                            Pair(remainingCalls.last().memory, remainingCalls.last().storage)

                    currentContext
                        .copy(lastReturnData = returnData, callStack = remainingCalls)
                        .updateCurrentCallContext(memory = newMemory, storage = newStorage)
                }
                Opcode.DELEGATECALL -> {
                    val (elements, newStack) = stack.popWords(7)
                    val (g, a, inLocation, inSize, outLocation, outSize) = elements

                    val address = a.toAddress()
                    val newContract = evmState.contractAt(address) ?: TODO()

                    val newCall = CallContext(
                        currentCallContext.caller,
                        memory.get(inLocation.toInt(), inSize.toInt()),
                        newContract,
                        CallType.DELEGATECALL,
                        currentCallContext.value,
                        g.toBigInt(),
                        outLocation.toInt(),
                        outSize.toInt(),
                        storage = currentCallContext.storage
                    )

                    currentContext
                        .updateCurrentCallContext(
                            stack = newStack,
                            gasRemaining = currentCallContext.gasRemaining - g.toBigInt()
                        )
                        .copy(callStack = callStack + newCall)
                }
                Opcode.STATICCALL -> {
                    val (elements, newStack) = stack.popWords(7)
                    val (g, a, v, inLocation, inSize, outLocation, outSize) = elements

                    val address = a.toAddress()
                    val newContract = evmState.contractAt(address) ?: TODO()

                    val newCall = CallContext(
                        currentCallContext.caller,
                        memory.get(inLocation.toInt(), inSize.toInt()),
                        newContract,
                        CallType.STATICCALL,
                        v.toBigInt(),
                        g.toBigInt(),
                        outLocation.toInt(),
                        outSize.toInt()
                    )

                    currentContext
                        .updateCurrentCallContext(
                            stack = newStack,
                            gasRemaining = currentCallContext.gasRemaining - g.toBigInt()
                        )
                        .copy(callStack = callStack + newCall)
                }
                Opcode.CREATE2 -> TODO()
                Opcode.REVERT -> {
                    val (elements, _) = stack.popWords(2)
                    val (p, s) = elements.map { it.toInt() }
                    val returnData = memory.get(p, s)

                    // TODO - caller balance loses used gas

                    startContext.copy(
                        lastReturnData = returnData,
                        completed = true,
                        logs = emptyList(),
                        callStack = callStack.take(1)
                    )
                }
                Opcode.INVALID -> {
                    // TODO - caller balance loses gas limit

                    startContext.copy(
                        lastReturnData = emptyList(),
                        completed = true,
                        logs = emptyList(),
                        callStack = callStack.take(1)
                    )
                }
                Opcode.SUICIDE -> TODO()


                /*
                lastReturnData

                evmState
                 */


                else -> TODO("$opcode is not implemented")
            }
        }
}
