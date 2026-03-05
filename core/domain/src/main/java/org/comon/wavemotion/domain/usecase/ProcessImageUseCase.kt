package org.comon.wavemotion.domain.usecase

import org.comon.wavemotion.domain.repository.HandRepository

class ProcessImageUseCase(private val repository: HandRepository) {
    operator fun invoke(data: ByteArray, width: Int, height: Int, rotationDegrees: Int) {
        repository.processImage(data, width, height, rotationDegrees)
    }
}