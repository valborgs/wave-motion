package org.comon.wavemotion.domain.entity

enum class HandBone(val index: Int, val vmcName: String) {
    // 손목
    WRIST(0, "Wrist"),

    // 엄지
    THUMB_CMC(1, "ThumbProximal"),
    THUMB_MCP(2, "ThumbIntermediate"),
    THUMB_IP(3, "ThumbDistal"),
    THUMB_TIP(4, "ThumbTip"),

    // 검지
    INDEX_MCP(5, "IndexProximal"),
    INDEX_PIP(6, "IndexIntermediate"),
    INDEX_DIP(7, "IndexDistal"),
    INDEX_TIP(8, "IndexTip"),

    // 중지
    MIDDLE_MCP(9, "MiddleProximal"),
    MIDDLE_PIP(10, "MiddleIntermediate"),
    MIDDLE_DIP(11, "MiddleDistal"),
    MIDDLE_TIP(12, "MiddleTip"),

    // 약지
    RING_MCP(13, "RingProximal"),
    RING_PIP(14, "RingIntermediate"),
    RING_DIP(15, "RingDistal"),
    RING_TIP(16, "RingTip"),

    // 새끼
    PINKY_MCP(17, "LittleProximal"),
    PINKY_PIP(18, "LittleIntermediate"),
    PINKY_DIP(19, "LittleDistal"),
    PINKY_TIP(20, "LittleTip");

    companion object {
        fun fromIndex(index: Int): HandBone? = HandBone.entries.find { it.index == index }
    }
}
