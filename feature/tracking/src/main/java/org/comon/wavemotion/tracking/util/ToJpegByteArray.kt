package org.comon.wavemotion.tracking.util

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * ImageProxy를 JPEG 형태의 ByteArray로 변환합니다.
 */
fun ImageProxy.toJpegByteArray(): ByteArray {
    val buffer: ByteBuffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}

fun ImageProxy.toDirectBufferByteArray(): ByteArray {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes
}