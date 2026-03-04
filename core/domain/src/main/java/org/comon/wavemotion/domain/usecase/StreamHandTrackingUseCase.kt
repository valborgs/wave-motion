package org.comon.wavemotion.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import org.comon.wavemotion.domain.entity.HandLandmark
import org.comon.wavemotion.domain.repository.HandRepository

/**
 * 실시간 손 트래킹 데이터를 스트리밍하고, 동시에 VMC 서버로 전송하는 비즈니스 로직을 수행합니다.
 * 이 UseCase는 Presentation 레이어(ViewModel)에서 호출되어 트래킹 파이프라인을 실행합니다.
 */
class StreamHandTrackingUseCase(
    private val handRepository: HandRepository
) {
    /**
     * 트래킹 스트림을 시작합니다.
     * 반환되는 Flow를 collect하면 실시간 좌표 데이터를 얻을 수 있으며,
     * 각 데이터가 발생할 때마다 자동으로 VMC 전송 로직이 실행됩니다.
     *
     * @return 손 관절 데이터 스트림 (HandLandmark Flow)
     */
    operator fun invoke(): Flow<HandLandmark> = handRepository
        .getHandTrackingStream()
        .onEach { landmark ->
            // 데이터가 들어올 때마다 VMC 서버(VSeeFace)로 좌표를 전송합니다.
            handRepository.sendToVmc(landmark)
        }
}