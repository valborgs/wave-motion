package org.comon.wavemotion.mediapipe

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.comon.wavemotion.data.datasource.MediaPipeDataSource
import org.comon.wavemotion.domain.entity.HandLandmark
import org.comon.wavemotion.domain.entity.HandPoint

/**
 * MediaPipe SDK를 사용하여 실제 손 관절 데이터를 추출하는 구현체입니다.
 */
class MediaPipeDataSourceImpl(
    private val context: Context
) : MediaPipeDataSource {

    // 실시간 스트림 데이터를 담을 Flow (최신 데이터 1개만 유지)
    private val _handLandmarks = MutableSharedFlow<List<HandLandmark>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private var handLandmarker: HandLandmarker? = null

    init {
        setupHandLandmarker()
    }

    /**
     * MediaPipe Hand Landmarker 초기화 설정
     */
    private fun setupHandLandmarker() {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("hand_landmarker.task") // assets 폴더 내 모델 파일
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setNumHands(2) // 양손 추적 활성화
            .setMinHandDetectionConfidence(0.5f) // 검출 임계값
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setRunningMode(RunningMode.LIVE_STREAM) // 실시간 스트림 모드 필수
            .setResultListener { result, _ -> processResult(result) }
            .build()

        handLandmarker = HandLandmarker.createFromOptions(context, options)
    }

    /**
     * 카메라 모듈로부터 받은 비트맵 이미지를 MediaPipe에 전달합니다.
     */
    override fun processImage(bitmap: Bitmap, rotationDegrees: Int) {
        val mpImage = BitmapImageBuilder(bitmap).build()
        val imageProcessingOptions = ImageProcessingOptions.builder()
            .setRotationDegrees(rotationDegrees) // 앞선 레이어에서 이미 회전되었으므로 0 전달
            .build()
        // 실시간 스트림 모드에서는 타임스탬프 전달이 필수입니다.
        handLandmarker?.detectAsync(mpImage, imageProcessingOptions, SystemClock.uptimeMillis())
    }

    /**
     * MediaPipe의 결과 데이터를 도메인 모델인 HandLandmark로 변환합니다.
     */
    private fun processResult(result: HandLandmarkerResult) {
        if (result.landmarks().isEmpty()) return

        val landmarksList = mutableListOf<HandLandmark>()

        for (i in result.landmarks().indices) {
            val landmarks = result.landmarks()[i]
            val handedness = result.handedness()[i][0] // 오른손/왼손 정보

            // 1. NormalizedLandmark -> HandPoint 변환
            val points = landmarks.map { landmark ->
                HandPoint(
                    x = landmark.x(),
                    y = landmark.y(),
                    z = landmark.z()
                )
            }

            // 2. HandLandmark 엔티티 생성
            val handLandmark = HandLandmark(
                points = points,
                // MediaPipe의 전면 카메라 분류 특성상 좌우가 반전되어 인식될 수 있으나 기본값 사용
                isRightHand = handedness.categoryName() == "Right",
                timestamp = System.currentTimeMillis()
            )
            landmarksList.add(handLandmark)
        }

        // 전체 손 리스트 Flow 방출
        _handLandmarks.tryEmit(landmarksList)
    }

    override fun getHandLandmarks(): Flow<List<HandLandmark>> = _handLandmarks.asSharedFlow()

    override fun close() {
        handLandmarker?.close()
    }
}