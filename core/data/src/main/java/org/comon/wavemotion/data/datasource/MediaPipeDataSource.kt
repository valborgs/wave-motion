package org.comon.wavemotion.data.datasource

import android.graphics.Bitmap
import kotlinx.coroutines.flow.Flow
import org.comon.wavemotion.domain.entity.HandLandmark

interface MediaPipeDataSource {
    fun getHandLandmarks(): Flow<HandLandmark>
    fun processImage(bitmap: Bitmap, rotationDegrees: Int)
}