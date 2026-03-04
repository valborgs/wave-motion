package org.comon.wavemotion.data

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * VMC(Virtual Motion Capture) 프로토콜을 사용하여 UDP 패킷을 전송하는 클래스입니다.
 */
class VmcSender(
    private val targetIp: String,
    private val targetPort: Int = 39539 // VSeeFace 기본 포트
) {
    private val socket = DatagramSocket()
    private val address = InetAddress.getByName(targetIp)

    /**
     * 특정 뼈대의 좌표 및 회전 데이터를 전송합니다.
     * VSeeFace 규격: /VMC/Ext/Bone/Pos (String, Float*7)
     */
    fun sendBoneData(
        boneName: String,
        posX: Float, posY: Float, posZ: Float,
        rotX: Float, rotY: Float, rotZ: Float, rotW: Float
    ) {
        val oscAddress = "/VMC/Ext/Bone/Pos"
        val typeTags = ",sfffffff" // String 1개 + Float 7개

        // OSC 규격에 따른 버퍼 크기 계산 및 할당
        val bufferSize = getPadSize(oscAddress) + getPadSize(typeTags) +
                getPadSize(boneName) + (4 * 7)
        val buffer = ByteBuffer.allocate(bufferSize).apply {
            order(ByteOrder.BIG_ENDIAN) // OSC는 Big Endian 사용
            putOscString(oscAddress)
            putOscString(typeTags)
            putOscString(boneName)
            putFloat(posX)
            putFloat(posY)
            putFloat(posZ)
            putFloat(rotX)
            putFloat(rotY)
            putFloat(rotZ)
            putFloat(rotW) // 7번째 f값 (중요)
        }

        sendUdp(buffer.array())
    }

    /**
     * VSeeFace에 장치가 활성화되었음을 알리는 하트비트 신호를 보냅니다.
     */
    fun sendAvailable() {
        val oscAddress = "/VMC/Ext/OK"
        val typeTags = "," // 파라미터 없음

        val buffer = ByteBuffer.allocate(getPadSize(oscAddress) + getPadSize(typeTags)).apply {
            order(ByteOrder.BIG_ENDIAN)
            putOscString(oscAddress)
            putOscString(typeTags)
        }
        sendUdp(buffer.array())
    }

    // --- OSC Helper 메서드 ---

    private fun ByteBuffer.putOscString(s: String) {
        val bytes = s.toByteArray(Charsets.UTF_8)
        put(bytes)
        put(0.toByte()) // Null terminator
        val pad = 4 - (bytes.size + 1) % 4
        if (pad < 4) repeat(pad) { put(0.toByte()) }
    }

    private fun getPadSize(s: String): Int {
        val size = s.toByteArray(Charsets.UTF_8).size + 1
        return size + (4 - size % 4) % 4
    }

    private fun sendUdp(data: ByteArray) {
        try {
            val packet = DatagramPacket(data, data.size, address, targetPort)
            socket.send(packet)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun close() {
        socket.close()
    }
}