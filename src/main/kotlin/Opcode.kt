package com.gammadex.kevin

enum class GasPriceTier(val cost: Int) {
    Zero(0),
    Base(2),
    VeryLow(3),
    Low(5),
    Mid(8),
    High(10),
    Ext(20),
    Special(0)
}

enum class Opcode(val code: Byte, val numArgs: Int, val numReturn: Int) {
    STOP(0x00, 0, 0),
    ADD(0x01, 2, 1),
    MUL(0x02, 2, 1),
    SUB(0x03, 2, 1),
    DIV(0x04, 2, 1),
    SDIV(0x05, 2, 1),
    MOD(0x06, 2, 1),
    SMOD(0x07, 2, 1),
    ADDMOD(0x08, 3, 1),
    MULMOD(0x09, 3, 1),
    EXP(0x0A, 2, 1),
    SIGNEXTEND(0x0B, 2, 1),
    LT(0x10, 2, 1),
    GT(0x11, 2, 1),
    SLT(0x12, 2, 1),
    SGT(0x13, 2, 1),
    EQ(0x14, 2, 1),
    ISZERO(0x15, 1, 1),
    AND(0x16, 2, 1),
    OR(0x17, 2, 1),
    XOR(0x18, 2, 1),
    NOT(0x19, 1, 1),
    BYTE(0x1A, 2, 1),
    SHL(0x1B, 2, 1),
    SHR(0x1C, 2, 1),
    SAR(0x1D, 2, 1),
    SHA3(0x20, 2, 1),
    ADDRESS(0x30, 0, 1),
    BALANCE(0x31, 1, 1),
    ORIGIN(0x32, 0, 1),
    CALLER(0x33, 0, 1),
    CALLVALUE(0x34, 0, 1),
    CALLDATALOAD(0x35, 1, 1),
    CALLDATASIZE(0x36, 0, 1),
    CALLDATACOPY(0x37, 3, 0),
    CODESIZE(0x38, 0, 1),
    CODECOPY(0x39, 3, 0),
    GASPRICE(0x3A, 0, 1),
    EXTCODESIZE(0x3B, 1, 1),
    EXTCODECOPY(0x3C, 4, 0),
    RETURNDATASIZE(0x3D, 0, 1),
    RETURNDATACOPY(0x3E, 3, 0),
    BLOCKHASH(0x40, 1, 1),
    COINBASE(0x41, 0, 1),
    TIMESTAMP(0x42, 0, 1),
    NUMBER(0x43, 0, 1),
    DIFFICULTY(0x44, 0, 1),
    GASLIMIT(0x45, 0, 1),
    POP(0x50, 1, 0),
    MLOAD(0x51, 1, 1),
    MSTORE(0x52, 2, 0),
    MSTORE8(0x53, 2, 0),
    SLOAD(0x54, 1, 1),
    SSTORE(0x55, 2, 0),
    JUMP(0x56, 1, 0),
    JUMPI(0x57, 2, 0),
    PC(0x58, 0, 1),
    MSIZE(0x59, 0, 1),
    GAS(0x5A, 0, 1),
    JUMPDEST(0x5B, 0, 0),
    PUSH1(0x60, 0, 1),
    PUSH2(0x61, 0, 1),
    PUSH3(0x62, 0, 1),
    PUSH4(0x63, 0, 1),
    PUSH5(0x64, 0, 1),
    PUSH6(0x65, 0, 1),
    PUSH7(0x66, 0, 1),
    PUSH8(0x67, 0, 1),
    PUSH9(0x68, 0, 1),
    PUSH10(0x69, 0, 1),
    PUSH11(0x6A, 0, 1),
    PUSH12(0x6B, 0, 1),
    PUSH13(0x6C, 0, 1),
    PUSH14(0x6D, 0, 1),
    PUSH15(0x6E, 0, 1),
    PUSH16(0x6F, 0, 1),
    PUSH17(0x70, 0, 1),
    PUSH18(0x71, 0, 1),
    PUSH19(0x72, 0, 1),
    PUSH20(0x73, 0, 1),
    PUSH21(0x74, 0, 1),
    PUSH22(0x75, 0, 1),
    PUSH23(0x76, 0, 1),
    PUSH24(0x77, 0, 1),
    PUSH25(0x78, 0, 1),
    PUSH26(0x79, 0, 1),
    PUSH27(0x7A, 0, 1),
    PUSH28(0x7B, 0, 1),
    PUSH29(0x7C, 0, 1),
    PUSH30(0x7D, 0, 1),
    PUSH31(0x7E, 0, 1),
    PUSH32(0x7F, 0, 1),
    DUP1(0x80, 1, 2),
    DUP2(0x81, 2, 3),
    DUP3(0x82, 3, 4),
    DUP4(0x83, 4, 5),
    DUP5(0x84, 5, 6),
    DUP6(0x85, 6, 7),
    DUP7(0x86, 7, 8),
    DUP8(0x87, 8, 9),
    DUP9(0x88, 9, 10),
    DUP10(0x89, 10, 11),
    DUP11(0x8A, 11, 12),
    DUP12(0x8B, 12, 13),
    DUP13(0x8C, 13, 14),
    DUP14(0x8D, 14, 15),
    DUP15(0x8E, 15, 16),
    DUP16(0x8F, 16, 17),
    SWAP1(0x90, 2, 2),
    SWAP2(0x91, 3, 3),
    SWAP3(0x92, 4, 4),
    SWAP4(0x93, 5, 5),
    SWAP5(0x94, 6, 6),
    SWAP6(0x95, 7, 7),
    SWAP7(0x96, 8, 8),
    SWAP8(0x97, 9, 9),
    SWAP9(0x98, 10, 10),
    SWAP10(0x99, 11, 11),
    SWAP11(0x9A, 12, 12),
    SWAP12(0x9B, 13, 13),
    SWAP13(0x9C, 14, 14),
    SWAP14(0x9D, 15, 15),
    SWAP15(0x9E, 16, 16),
    SWAP16(0x9F, 17, 17),
    LOG0(0xA0, 2, 0),
    LOG1(0xA1, 3, 0),
    LOG2(0xA2, 4, 0),
    LOG3(0xA3, 5, 0),
    LOG4(0xA4, 6, 0),
    PUSHC(0xAC, 0, 1),
    JUMPC(0xAD, 1, 0),
    JUMPCI(0xAE, 2, 0),
    JUMPTO(0xB0, 1, 0),
    JUMPIF(0xB1, 2, 0),
    JUMPSUB(0xB2, 1, 0),
    JUMPV(0xB3, 1, 0),
    JUMPSUBV(0xB4, 1, 0),
    BEGINSUB(0xB5, 0, 0),
    BEGINDATA(0xB6, 0, 0),
    RETURNSUB(0xB7, 1, 0),
    PUTLOCAL(0xB8, 1, 0),
    GETLOCAL(0xB9, 0, 1),
    XADD(0xC1, 0, 0),
    XMUL(0xC2, 2, 1),
    XSUB(0xC3, 2, 1),
    XDIV(0xC4, 2, 1),
    XSDIV(0xC5, 2, 1),
    XMOD(0xC6, 2, 1),
    XSMOD(0xC7, 2, 1),
    XLT(0xD0, 2, 1),
    XGT(0xD1, 2, 1),
    XSLT(0xD2, 2, 1),
    XSGT(0xD3, 2, 1),
    XEQ(0xD4, 2, 1),
    XISZERO(0xD5, 2, 1),
    XAND(0xD6, 1, 1),
    XOR2(0xD7, 2, 1),
    XXOR(0xD8, 2, 1),
    XNOT(0xD9, 2, 1),
    XSHL(0xDB, 2, 1),
    XSHR(0xDC, 2, 1),
    XSAR(0xDD, 2, 1),
    XROL(0xDE, 2, 1),
    XROR(0xDF, 2, 1),
    XPUSH(0xE0, 1, 1),
    XMLOAD(0xE1, 1, 1),
    XMSTORE(0xE2, 2, 0),
    XSLOAD(0xE4, 1, 1),
    XSSTORE(0xE5, 2, 0),
    XVTOWIDE(0xE6, 1, 1),
    XWIDETOV(0xE7, 1, 1),
    XGET(0xE8, 3, 1),
    XPUT(0xE9, 2, 1),
    XSWIZZLE(0xEA, 2, 1),
    XSHUFFLE(0xEF, 3, 1),
    CREATE(0xF0, 3, 1),
    CALL(0xF1, 7, 1),
    CALLCODE(0xF2, 7, 1),
    RETURN(0xF3, 2, 0),
    DELEGATECALL(0xF4, 6, 1),
    STATICCALL(0xFA, 6, 1),
    CREATE2(0xFB, 3, 1),
    REVERT(0xFD, 2, 0),
    INVALID(0xFE, 0, 0),
    SUICIDE(0xFF, 1, 0);

    constructor(code: Int, numArgs: Int, numReturn: Int) : this(Byte(code), numArgs, numReturn)

    companion object {
        val byCode = values().map { it.code to it }.toMap()
    }
}
