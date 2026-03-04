package org.comon.wavemotion.domain.entity

data class HandLandmark(
    val points: List<HandPoint>, // 0~20번까지의 관절 데이터
    val isRightHand: Boolean,    // 왼손/오른손 구분
    val timestamp: Long          // 데이터의 신선도 확인용
) {
    init {
        require(points.size == 21) { "손 관절 데이터는 반드시 21개여야 합니다." }
    }
}
