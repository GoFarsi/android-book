package com.book.gofarsi

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.net.URLConnection
import java.security.MessageDigest

class CacheInterceptor(context: Context) {
    private val cacheDir = File(context.cacheDir, "web_cache")
    private val maxCacheSize = 50 * 1024 * 1024 // 50 MB

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    fun getResponse(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        val cacheFile = File(cacheDir, url.toMD5())

        if (cacheFile.exists()) {
            return createResponse(cacheFile, URLConnection.guessContentTypeFromName(url))
        }
        return null
    }

    fun saveResponse(url: String, response: WebResourceResponse) {
        val cacheFile = File(cacheDir, url.toMD5())
        response.data?.let { data ->
            try {
                FileOutputStream(cacheFile).use { output ->
                    data.copyTo(output)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        manageCacheSize()
    }

    private fun manageCacheSize() {
        var totalSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0
        if (totalSize > maxCacheSize) {
            cacheDir.listFiles()
                ?.sortedBy { it.lastModified() }
                ?.forEach {
                    if (totalSize > maxCacheSize) {
                        totalSize -= it.length()
                        it.delete()
                    }
                }
        }
    }

    private fun createResponse(file: File, mimeType: String?): WebResourceResponse {
        return WebResourceResponse(
            mimeType,
            "UTF-8",
            FileInputStream(file)
        )
    }

    private fun String.toMD5(): String {
        val md = MessageDigest.getInstance("MD5")
        return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
    }
}