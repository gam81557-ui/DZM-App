package com.dzm.manager.shizuku

import com.dzm.manager.IFileSource
import java.io.InputStream

/** Lets the user service read a staged file without access to the app's private directory. */
internal class DownloadFileSource(private val input: InputStream) : IFileSource.Stub() {
    @Synchronized
    override fun read(maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes.coerceIn(1, 64 * 1024))
        val count = input.read(buffer)
        return when {
            count <= 0 -> ByteArray(0)
            count < buffer.size -> buffer.copyOf(count)
            else -> buffer
        }
    }
}
