package org.kevem.rpc

import org.kevem.common.conversions.bytesToString
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.kevem.common.Byte
import org.kevem.common.conversions.toBigInteger

// TODO - remove this use of web3 when possible
class TransactionSigner {

    fun sign(transaction: SendTransactionParamDTO, account: LocalAccount): List<Byte> {
        val rawTransaction = RawTransaction.createTransaction(
            toBigInteger(transaction.nonce ?: "0"),
            toBigInteger(transaction.gasPrice),
            toBigInteger(transaction.gas),
            transaction.to,
            toBigInteger(transaction.value ?: "0"),
            transaction.data
        )

        val signedMessage = TransactionEncoder.signMessage(
            rawTransaction,
            Credentials.create(bytesToString(account.privateKey))
        )

        return signedMessage.map { Byte(it.toInt() and 0xFF) }
    }
}