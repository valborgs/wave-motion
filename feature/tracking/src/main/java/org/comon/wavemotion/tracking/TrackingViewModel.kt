package org.comon.wavemotion.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.comon.wavemotion.domain.entity.HandLandmark
import org.comon.wavemotion.domain.usecase.CloseTrackingUseCase
import org.comon.wavemotion.domain.usecase.ProcessImageUseCase
import org.comon.wavemotion.domain.usecase.StreamHandTrackingUseCase

class TrackingViewModel(
    private val streamHandTrackingUseCase: StreamHandTrackingUseCase,
    private val processImageUseCase: ProcessImageUseCase,
    private val closeTrackingUseCase: CloseTrackingUseCase
): ViewModel() {

    private val _handLandmarks = MutableStateFlow<List<HandLandmark>>(emptyList())
    val handLandmarks: StateFlow<List<HandLandmark>> = _handLandmarks.asStateFlow()

    private val _imageSize = MutableStateFlow(Pair(1, 1))
    val imageSize: StateFlow<Pair<Int, Int>> = _imageSize.asStateFlow()

    fun startTracking() {
        viewModelScope.launch {
            streamHandTrackingUseCase().collect { landmarks ->
                _handLandmarks.value = landmarks
            }
        }
    }

    fun onCameraFrameReceived(data: ByteArray, width: Int, height: Int, rotationDegrees: Int) {
        val isPortrait = rotationDegrees == 90 || rotationDegrees == 270
        val imageW = if (isPortrait) height else width
        val imageH = if (isPortrait) width else height
        _imageSize.value = Pair(imageW, imageH)

        processImageUseCase(data, width, height, rotationDegrees)
    }

    override fun onCleared() {
        super.onCleared()
        closeTrackingUseCase()
    }
}