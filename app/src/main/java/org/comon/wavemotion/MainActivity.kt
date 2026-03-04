package org.comon.wavemotion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import org.comon.wavemotion.data.HandRepositoryImpl
import org.comon.wavemotion.data.VmcSender
import org.comon.wavemotion.domain.usecase.ProcessImageUseCase
import org.comon.wavemotion.domain.usecase.StreamHandTrackingUseCase
import org.comon.wavemotion.mediapipe.MediaPipeDataSourceImpl
import org.comon.wavemotion.tracking.TrackingScreen
import org.comon.wavemotion.tracking.TrackingViewModel
import org.comon.wavemotion.tracking.TrackingViewModelFactory
import org.comon.wavemotion.ui.theme.WaveMotionTheme

class MainActivity : ComponentActivity() {

    // 1. 권한 요청 런처 정의
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // 권한 허용됨 -> 화면 그리기
            setupContent()
        } else {
            // 권한 거부됨 -> 사용자에게 알림 표시 등의 처리
        }
    }

    private val trackingViewModel: TrackingViewModel by viewModels {
        val vmcSender = VmcSender(targetIp = "10.132.23.12")
        val mediaPipeDataSource = MediaPipeDataSourceImpl(this)
        val handRepository = HandRepositoryImpl(vmcSender, mediaPipeDataSource)

        TrackingViewModelFactory(
            StreamHandTrackingUseCase(handRepository),
            ProcessImageUseCase(handRepository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        // 2. 권한 확인 및 요청
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> {
                setupContent()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun setupContent() {
        setContent {
            WaveMotionTheme {
                TrackingScreen(viewModel = trackingViewModel)
            }
        }
    }
}

