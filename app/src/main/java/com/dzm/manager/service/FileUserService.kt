package com.dzm.manager.service

import android.content.Context
import androidx.annotation.Keep
import com.dzm.manager.IFileService
import com.dzm.manager.IFileSource
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlin.system.exitProcess

class FileUserService : IFileService.Stub {

    @Suppress("unused")
    constructor()

    @Suppress("unused")
    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    override fun fileExists(targetDir: String?, fileName: String?): Boolean {
        if (targetDir.isNullOrBlank() || fileName.isNullOrBlank()) return false
        if (fileName.contains("..") || fileName.contains('/') || fileName.contains('\\')) {
            return false
        }
        return try {
            val target = File(targetDir, fileName)
            target.exists() && target.isFile
        } catch (_: SecurityException) {
            false
        }
    }

    override fun copyFile(source: IFileSource?, targetDir: String?, fileName: String?): String {
        if (source == null) return "ERROR:Missing downloaded file."
        return try {
            openSource(source).use { input ->
                copyFileFromStream(input, targetDir, fileName)
            }
        } catch (t: Throwable) {
            "ERROR:${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun copyFileFromStream(
        input: InputStream,
        targetDir: String?,
        fileName: String?
    ): String {
        if (targetDir.isNullOrBlank() || fileName.isNullOrBlank()) {
            return "ERROR:Invalid target path."
        }
        if (fileName.contains("..") || fileName.contains('/') || fileName.contains('\\')) {
            return "ERROR:Invalid file name."
        }

        val destDir = File(targetDir)
        if (!destDir.exists() && !destDir.mkdirs()) {
            return "ERROR:Failed to create directory $targetDir"
        }

        val destFile = File(destDir, fileName)
        val tempFile = File(destDir, "$fileName.tmp")

        FileOutputStream(tempFile, false).use { output ->
            input.copyTo(output)
            output.fd.sync()
        }

        if (destFile.exists() && !destFile.delete()) {
            tempFile.delete()
            return "ERROR:Unable to replace existing file ${destFile.absolutePath}"
        }

        if (!tempFile.renameTo(destFile)) {
            FileInputStream(tempFile).use { input ->
                FileOutputStream(destFile, false).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            tempFile.delete()
        }

        return "OK"
    }

    override fun zipHasExistingFiles(source: IFileSource?, targetDir: String?): Boolean {
        if (source == null) return false
        return try {
            openZip(source).use { zis ->
                if (targetDir.isNullOrBlank()) return false
                val destRoot = File(targetDir)
                if (!destRoot.exists()) return false
                val canonicalRoot = destRoot.canonicalPath
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val cleanName = entry.name.trimStart('/')
                    if (cleanName.isNotEmpty() && !entry.isDirectory) {
                        val outFile = File(destRoot, cleanName)
                        val canonicalOut = outFile.canonicalPath
                        val insideRoot = canonicalOut == canonicalRoot ||
                            canonicalOut.startsWith(canonicalRoot + File.separator)
                        if (insideRoot && outFile.exists() && outFile.isFile) {
                            zis.closeEntry()
                            return true
                        }
                    }
                    zis.closeEntry()
                }
            }
            false
        } catch (_: Throwable) {
            false
        }
    }

    override fun extractZip(source: IFileSource?, targetDir: String?): String {
        if (source == null) return "ERROR:Missing downloaded archive."
        return try {
            openZip(source).use { zis ->
                if (targetDir.isNullOrBlank()) return "ERROR:Invalid zip target directory."
                val destRoot = File(targetDir)
                if (!destRoot.exists() && !destRoot.mkdirs()) {
                    return "ERROR:Failed to create directory $targetDir"
                }
                val canonicalRoot = destRoot.canonicalPath
                var entryCount = 0
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val entry = zis.nextEntry ?: break
                    val cleanName = entry.name.trimStart('/')
                    if (cleanName.isEmpty()) {
                        zis.closeEntry()
                        continue
                    }

                    val outFile = File(destRoot, cleanName)
                    val canonicalOut = outFile.canonicalPath
                    val insideRoot = canonicalOut == canonicalRoot ||
                        canonicalOut.startsWith(canonicalRoot + File.separator)
                    if (!insideRoot) {
                        zis.closeEntry()
                        return "ERROR:Blocked unsafe zip path: ${entry.name}"
                    }

                    entryCount++
                    if (entry.isDirectory) {
                        // Never delete or replace existing folders; only create missing ones
                        if (!outFile.exists() && !outFile.mkdirs()) {
                            zis.closeEntry()
                            return "ERROR:Failed to create directory ${outFile.absolutePath}"
                        }
                    } else {
                        val parent = outFile.parentFile
                        if (parent != null && !parent.exists() && !parent.mkdirs()) {
                            zis.closeEntry()
                            return "ERROR:Failed to create parent folder ${parent.absolutePath}"
                        }

                        if (outFile.exists()) {
                            // Never replace existing folders or non-.pak files
                            if (outFile.isDirectory || !outFile.name.endsWith(".pak", ignoreCase = true)) {
                                zis.closeEntry()
                                continue
                            }
                        }

                        val tempTarget = File(parent ?: destRoot, "${outFile.name}.tmp")
                        FileOutputStream(tempTarget, false).use { output ->
                            while (true) {
                                val read = zis.read(buffer)
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                            }
                            output.fd.sync()
                        }

                        if (outFile.exists() && !outFile.delete()) {
                            tempTarget.delete()
                            zis.closeEntry()
                            return "ERROR:Unable to replace ${outFile.absolutePath}"
                        }
                        if (!tempTarget.renameTo(outFile)) {
                            FileInputStream(tempTarget).use { input ->
                                FileOutputStream(outFile, false).use { output ->
                                    input.copyTo(output)
                                    output.fd.sync()
                                }
                            }
                            tempTarget.delete()
                        }
                    }
                    zis.closeEntry()
                }
                if (entryCount == 0) "ERROR:Zip archive is empty or invalid." else "OK"
            }
        } catch (t: Throwable) {
            "ERROR:${t.message ?: t.javaClass.simpleName}"
        }
    }

    private fun openSource(source: IFileSource): InputStream = object : InputStream() {
        private val singleByte = ByteArray(1)

        override fun read(): Int =
            if (read(singleByte, 0, 1) == -1) -1 else singleByte[0].toInt() and 0xff

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            val chunk = source.read(length) ?: throw IOException("Missing download data.")
            if (chunk.isEmpty()) return -1
            if (chunk.size > length) throw IOException("Invalid download data.")
            chunk.copyInto(buffer, offset)
            return chunk.size
        }
    }

    private fun openZip(source: IFileSource) =
        ZipInputStream(BufferedInputStream(openSource(source), 64 * 1024))

    override fun destroy() {
        exitProcess(0)
    }
}
