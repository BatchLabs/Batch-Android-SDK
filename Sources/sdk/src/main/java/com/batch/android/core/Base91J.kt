package com.batch.android.core

import kotlin.math.ceil

/**
 * Base91J encoder/decoder.
 *
 * Base91J is a format based on Base91, but JSON safe. This implementation is based on the Base91
 * Java library.
 */
@OptIn(ExperimentalUnsignedTypes::class)
class Base91J {

    private val alphabet =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!#$%&()*+,./:;<=>?@[]^_`{|}~'"
    private val encodingMap = UByteArray(91)
    private val decodingMap = UByteArray(256) { 0xFF.toUByte() }

    class CorruptedInputException : Exception()

    init {
        prepareMaps()
    }

    /** Encodes a string to a Base91J string. */
    fun encodeString(input: String): String {
        return encode(input.encodeToByteArray()).decodeToString()
    }

    /** Decodes a Base91J string to a string. */
    @Throws(CorruptedInputException::class)
    fun decodeString(input: String): ByteArray {
        return decode(input.encodeToByteArray())
    }

    /** Encode to Base91J byte array. */
    fun encode(input: ByteArray): ByteArray {
        var queue: UInt = 0u
        var numBits: UInt = 0u
        var n = 0

        val output = UByteArray(ceil(input.size * 16.0 / 13.0).toInt())

        for (byte in input.toUByteArray()) {
            queue = queue or (byte.toUInt() shl numBits.toInt())
            numBits += 8u
            if (numBits > 13u) {
                var v: UInt = queue and 8191u

                if (v > 88u) {
                    queue = queue shr 13
                    numBits -= 13u
                } else {
                    v = queue and 16383u
                    queue = queue shr 14
                    numBits -= 14u
                }
                output[n++] = encodingMap[(v % 91u).toInt()]
                output[n++] = encodingMap[(v / 91u).toInt()]
            }
        }

        if (numBits > 0u) {
            output[n++] = encodingMap[(queue % 91u).toInt()]

            if (numBits > 7u || queue > 90u) {
                output[n++] = encodingMap[(queue / 91u).toInt()]
            }
        }

        return output.copyOf(n).toByteArray()
    }

    /** Decodes a Base91J byte array. */
    @Throws(CorruptedInputException::class)
    fun decode(input: ByteArray): ByteArray {
        var queue: UInt = 0u
        var numBits: UInt = 0u
        var v = -1
        var n = 0
        val output = UByteArray(ceil(input.size * 14.0 / 16.0).toInt())

        for (byte in input.toUByteArray()) {
            if (decodingMap[byte.toInt()] == 0xFF.toUByte()) {
                throw CorruptedInputException()
            }

            if (v == -1) {
                v = decodingMap[byte.toInt()].toInt()
            } else {
                v += decodingMap[byte.toInt()].toInt() * 91
                queue = queue or (v.toUInt() shl numBits.toInt())

                if (v and 8191 > 88) {
                    numBits += 13u
                } else {
                    numBits += 14u
                }

                while (numBits > 7u) {
                    output[n++] = queue.toUByte()
                    queue = queue shr 8
                    numBits -= 8u
                }

                v = -1
            }
        }

        if (v != -1) {
            output[n++] = (queue or (v.toUInt() shl numBits.toInt())).toUByte()
        }

        return output.copyOf(n).toByteArray()
    }

    private fun prepareMaps() {
        for ((index, char) in alphabet.encodeToByteArray().withIndex()) {
            encodingMap[index] = char.toUByte()
            decodingMap[char.toInt()] = index.toUByte()
        }
    }
}
