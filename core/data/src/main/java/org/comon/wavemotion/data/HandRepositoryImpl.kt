package org.comon.wavemotion.data

import kotlinx.coroutines.flow.Flow
import org.comon.wavemotion.data.datasource.MediaPipeDataSource
import org.comon.wavemotion.domain.entity.HandBone
import org.comon.wavemotion.domain.entity.HandLandmark
import org.comon.wavemotion.domain.repository.HandRepository
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 도메인 레이어의 HandRepository를 실제로 구현하는 클래스입니다.
 * MediaPipe의 데이터를 VMC 규격으로 변환하여 전송합니다.
 */
class HandRepositoryImpl(
    private val vmcSender: VmcSender,
    private val mediaPipeDataSource: MediaPipeDataSource
) : HandRepository {

    override fun getHandTrackingStream(): Flow<HandLandmark> {
        // MediaPipe 소스로부터 실시간 좌표 스트림을 가져옵니다.
        return mediaPipeDataSource.getHandLandmarks()
    }

    override suspend fun sendToVmc(landmark: HandLandmark) {
        withContext(Dispatchers.IO) {
            try {
                vmcSender.sendAvailable()

                val prefix = if (landmark.isRightHand) "Right" else "Left"

                // 💡 보정 계수 설정 (민감도 조절)
                val scale = 0.5f // 움직임 범위를 적당히 줄임
                val xOffset = 0f
                val yOffset = 1.0f // 아바타의 어깨/가슴 높이로 손을 올림
                val zOffset = 0.2f // 몸에서 약간 앞으로 떨어뜨림

                landmark.points.forEachIndexed { index, point ->
                    val bone = HandBone.fromIndex(index) ?: return@forEachIndexed
                    val fullBoneName = if (bone == HandBone.WRIST) "${prefix}Hand" else "$prefix${bone.vmcName}"

                    // 1. 좌표계 변환 (MediaPipe 0~1 -> Warudo 미터 단위)
                    // X: 좌우 반전 (전면 카메라 기준)
                    val posX = (0.5f - point.x) * scale + xOffset
                    // Y: 위아래 반전 (MediaPipe는 상단이 0, Warudo는 바닥이 0)
                    val posY = (0.5f - point.y) * scale + yOffset
                    // Z: 깊이감 보정
                    val posZ = -point.z * scale + zOffset

                    // 2. VMC 패킷 전송
                    vmcSender.sendBoneData(
                        boneName = fullBoneName,
                        posX = posX, posY = posY, posZ = posZ,
                        // ⚠️ 현재 rotW가 1f이므로 손가락이 펴진 채로 유지됩니다.
                        // 손가락을 굽히려면 여기서 실제 회전값(Quaternion)을 계산해 넣어야 합니다.
                        rotX = 0f, rotY = 0f, rotZ = 0f, rotW = 1f
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun processImage(data: ByteArray, width: Int, height: Int, rotationDegrees: Int) {
        val bitmap = createBitmap(width, height)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(data))

        // 사용자의 피드백에 맞춰 이미지를 물리적으로 회전시킵니다.
        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        val rotatedBitmap = android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)

        mediaPipeDataSource.processImage(rotatedBitmap, 0)
    }
}