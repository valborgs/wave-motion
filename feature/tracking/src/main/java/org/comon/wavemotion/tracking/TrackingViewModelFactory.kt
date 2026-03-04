package org.comon.wavemotion.tracking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.comon.wavemotion.domain.usecase.ProcessImageUseCase
import org.comon.wavemotion.domain.usecase.StreamHandTrackingUseCase

class TrackingViewModelFactory(
    private val streamHandTrackingUseCase: StreamHandTrackingUseCase,
    private val processImageUseCase: ProcessImageUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackingViewModel(streamHandTrackingUseCase, processImageUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}