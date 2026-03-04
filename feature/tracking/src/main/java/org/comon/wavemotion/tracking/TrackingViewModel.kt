package org.comon.wavemotion.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.comon.wavemotion.domain.usecase.ProcessImageUseCase
import org.comon.wavemotion.domain.usecase.StreamHandTrackingUseCase

class TrackingViewModel(
    private val streamHandTrackingUseCase: StreamHandTrackingUseCase,
    private val processImageUseCase: ProcessImageUseCase,
): ViewModel() {

    fun startTracking() {
        viewModelScope.launch {
            streamHandTrackingUseCase().collect {

            }
        }
    }

    fun onCameraFrameReceived(data: ByteArray, width: Int, height: Int) {
        processImageUseCase(data, width, height)
    }
}