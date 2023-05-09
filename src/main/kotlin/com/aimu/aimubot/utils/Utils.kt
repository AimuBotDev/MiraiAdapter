package com.aimu.aimubot.utils

import java.io.File
import java.net.URL
import java.net.URLDecoder
import java.util.zip.GZIPInputStream

fun String.getSandwichedText(left: String, right: String): String {
    if (left == "") {
        if (this.contains(right))
            return this.substringBefore(right)
    } else if (right == "") {
        if (this.contains(left))
            return this.substringAfter(left)
    } else {
        if (this.contains(left) && this.contains(right))
            return this.substringAfter(left).substringBefore(right)
    }
    return ""
}

fun downloadBytes(url: String) : ByteArray {
    println("Downloading Bytes [$url]")
    val openConnection = URL(url).openConnection()
    openConnection.addRequestProperty(
        "user-agent",
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
    )
    val bytes = if (openConnection.contentEncoding == "gzip") {
        GZIPInputStream(openConnection.getInputStream()).readBytes()
    } else {
        openConnection.getInputStream().readBytes()
    }
    return bytes
}

fun downloadImage(url: String, file: File): File {
    println("Downloading Image [$url]")
    val openConnection = URL(url).openConnection()
    
    openConnection.addRequestProperty(
        "user-agent",
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
    )
    
    val bytes = if (openConnection.contentEncoding == "gzip") {
        GZIPInputStream(openConnection.getInputStream()).readBytes()
    } else {
        openConnection.getInputStream().readBytes()
    }
    file.writeBytes(bytes)
    return file
}

fun downloadImage(url: String, path: String, overwrite: Boolean) {
    print("Downloading Image [$url]")

    val fileName = URLDecoder.decode(url, "utf-8").substringAfterLast("/")
    val file = File("$path//$fileName")
    if (!overwrite && file.exists()) {
        println(" skipped")
        return
    }

    println()

    val openConnection = URL(url).openConnection()
    
    openConnection.addRequestProperty(
        "user-agent",
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.169 Safari/537.36"
    )
    
    val bytes = if (openConnection.contentEncoding == "gzip") {
        GZIPInputStream(openConnection.getInputStream()).readBytes()
    } else {
        openConnection.getInputStream().readBytes()
    }
    file.writeBytes(bytes)
}
