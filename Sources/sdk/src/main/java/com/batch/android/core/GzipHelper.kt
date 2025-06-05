package com.batch.android.core

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.text.Charsets.UTF_8

/** Helper class for gzip compression. */
object GzipHelper {

    /** Gzip a string to a byte array. */
    @JvmStatic
    @Throws(IOException::class)
    fun gzip(content: String): ByteArray {
        val buffer = content.toByteArray(UTF_8)
        ByteArrayOutputStream(buffer.size).use { bos ->
            GZIPOutputStream(bos).use { gzipOS -> gzipOS.write(buffer) }
            return bos.toByteArray()
        }
    }

    /** Ungzip a byte array to a string */
    @JvmStatic
    fun ungzip(content: ByteArray): String =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).use { it.readText() }
}
