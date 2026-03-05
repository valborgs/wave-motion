package org.comon.wavemotion.domain.usecase

import org.comon.wavemotion.domain.repository.HandRepository

class CloseTrackingUseCase(private val repository: HandRepository) {
    operator fun invoke() {
        repository.close()
    }
}
