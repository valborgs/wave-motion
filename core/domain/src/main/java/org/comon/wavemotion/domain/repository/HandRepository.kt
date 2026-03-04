package org.comon.wavemotion.domain.repository

import kotlinx.coroutines.flow.Flow
import org.comon.wavemotion.domain.entity.HandLandmark

/**
 * 손 트래킹 데이터를 관리하는 리포지토리 인터페이스입니다.
 */
interface HandRepository {
    /**
     * 실시간으로 추출되는 손 관절 데이터 스트림을 반환합니다.
     */
    fun getHandTrackingStream(): Flow<HandLandmark>

    /**
     * 추출된 데이터를 외부(VSeeFace)로 전송합니다.
     */
    suspend fun sendToVmc(landmark: HandLandmark)

    /**
     * Bitmap 대신 픽셀 데이터인 ByteArray와 이미지 정보를 전달합니다.
     */
    fun processImage(data: ByteArray, width: Int, height: Int)
}