package org.comon.wavemotion.tracking

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.comon.wavemotion.tracking.util.toDirectBufferByteArray
import java.util.concurrent.Executors

private val HAND_CONNECTIONS = listOf(
    Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4), // 엄지
    Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8), // 검지
    Pair(5, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12), // 중지
    Pair(9, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16), // 약지
    Pair(13, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20), // 새끼
    Pair(0, 17) // 손바닥 (손목 ~ 새끼 손가락 밑)
)

@Composable
fun TrackingScreen(
    viewModel: TrackingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    val handLandmarks by viewModel.handLandmarks.collectAsStateWithLifecycle()
    val imageSize by viewModel.imageSize.collectAsStateWithLifecycle()

    LaunchedEffect(Unit){
        viewModel.startTracking()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 1. Preview 설정
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // 2. ImageAnalysis 설정 (프레임 캡처 및 전달)
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            // ByteArray로 변환하여 ViewModel에 전달
                            val data = imageProxy.toDirectBufferByteArray()
                            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                            viewModel.onCameraFrameReceived(data, imageProxy.width, imageProxy.height, rotationDegrees)
                            imageProxy.close()
                        }
                    }

                try {
                    cameraProvider.unbindAll()
                    // 전면 카메라 사용
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        imageAnalyzer
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val imageW = imageSize.first.toFloat()
            val imageH = imageSize.second.toFloat()

            // PreviewView의 FIT_CENTER 스케일링 및 오프셋 계산 (minOf 사용)
            val scale = minOf(size.width / imageW, size.height / imageH)
            val scaledW = imageW * scale
            val scaledH = imageH * scale
            val startX = (size.width - scaledW) / 2f
            val startY = (size.height - scaledH) / 2f

            handLandmarks.forEach { landmark ->
                val points = landmark.points
                if (points.size >= 21) {
                    // 전면 카메라인 경우 좌우 반전 필요: scaledW 안에서 반전 후 오프셋 더하기
                    val offsets = points.map { 
                        val px = startX + (1f - it.x) * scaledW
                        val py = startY + it.y * scaledH
                        Offset(
                            x = px,
                            y = py
                        )
                    }

                    // 1. 관절 연결선 그리기
                    HAND_CONNECTIONS.forEach { (start, end) ->
                        drawLine(
                            color = Color.Green,
                            start = offsets[start],
                            end = offsets[end],
                            strokeWidth = 5f
                        )
                    }

                    // 2. 관절 포인트 그리기
                    offsets.forEach { offset ->
                        drawCircle(
                            color = Color.Red,
                            radius = 8f,
                            center = offset
                        )
                    }
                }
            }
        }
    }

    // 컴포저블 제거 시 Executor 정리
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
}