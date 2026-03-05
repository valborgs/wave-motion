# 개발 로그 (Development Log)

프로젝트 개발 과정 중 발생한 요구사항, 구현내용, 이슈 및 해결 과정을 스터디 및 히스토리 파악 목적으로 최신 일자순으로 기록합니다.

---

## 2026-03-05
**제목:** ViewModel 생명주기에 맞춘 MediaPipe 리소스 해제 (Memory Leak 방지)

**요구사항:**
- `TrackingViewModel`이 소멸(`onCleared()`)될 때, 백그라운드에서 동작 중인 `HandLandmarker`의 리소스가 정상적으로 해제되어야 함.
- Clean Architecture 원칙(ViewModel -> UseCase -> Repository -> DataSource)을 준수하면서 계층을 거쳐 리소스 해제가 전파되도록 구성.

**구현내용:**
- **인터페이스 및 DataSource/Repository 확장**: `MediaPipeDataSource` 및 `HandRepository` 인터페이스에 `close()` 메서드를 각각 정의하고, `HandRepositoryImpl`에서 `mediaPipeDataSource.close()`를 위임하여 호출하도록 구현.
- **UseCase 추가**: ViewModel이 Repository 구현을 직접 참조하지 않도록 `CloseTrackingUseCase`를 신규 생성하여 도메인 레이어에서 정리 작업을 처리하도록 캡슐화.
- **ViewModel 리소스 해제**: `TrackingViewModel` 내부에 `onCleared()`를 오버라이드하고, DI를 통해 주입받은 `closeTrackingUseCase()`를 호출하여 자원 해제.
- **의존성 주입 수정**: `TrackingViewModelFactory`와 `MainActivity`의 초기화 로직에 `CloseTrackingUseCase` 인스턴스를 생성하여 넘겨주도록 수정.

---

## 2026-03-05
**제목:** 단일 손 트래킹에서 양손 트래킹으로 확장 연동

**요구사항:**
- MediaPipe에서 한 손만 추적되던 기존 로직을 변경하여 양손을 모두 추적하도록 수정.
- 양손의 랜드마크 데이터를 각각 분리하여 VMC 규격(Warudo 등)으로 전송하고, 화면 CameraX Canvas 위에도 두 스켈레톤 모두 시각적으로 렌더링.

**구현내용:**
- **MediaPipe 옵션 조정**: `MediaPipeDataSourceImpl.kt` 의 `HandLandmarkerOptions` 설정에서 추적 감지 최대 손의 개수(`setNumHands(2)`)를 2개로 상향.
- **도메인 및 데이터 레이어 흐름 변경**: `MediaPipeDataSource`와 `HandRepository`의 인터페이스 반환 타입을 기존 단일 `HandLandmark` 파라미터에서 여러 개의 손을 담을 수 있는 `List<HandLandmark>` Flow 형태로 전체 구조 리팩토링.
- **VMC 다중 손 송신 처리**: `StreamHandTrackingUseCase`에서 `List`로 받아온 랜드마크들을 순회(forEach)하며, 데이터 내의 `isRightHand` 속성(MediaPipe `handedness()[0]` 기반)에 따라 `sendToVmc()` 함수가 Left/Right 접두어를 붙여 각각 별도 처리 및 전송을 하도록 구현.
- **UI 렌더링 수정**: `TrackingViewModel`에서도 List 형태의 StateFlow를 유지하게 하고, `TrackingScreen` UI의 Canvas 컴포넌트에서 `handLandmarks` 리스트를 반복문으로 돌면서 감지된 모든 손에 대해 초록색 연결선과 빨간색 관절 포인트를 각각 분리하여 독립적으로 그리도록 코드 수정.

---

## 2026-03-05
**제목:** 카메라 뷰 내 손 스켈레톤 UI 오버레이 구현 및 좌표/비율 왜곡 문제 해결

**요구사항:**
- 전면 카메라 프리뷰 뷰 위에 MediaPipe를 통해 감지된 사용자의 손 랜드마크(21개 관절)를 뼈대(Skeleton) 오버레이 형태로 실시간 렌더링.

**구현내용:**
- `TrackingViewModel`에 카메라 프레임 해상도(`imageSize`)와 MediaPipe 검출 결과(`handLandmark`)를 전달받아 보관하는 `StateFlow` 추가.
- `TrackingScreen` UI 컴포저블 내에 CameraX `PreviewView`를 배치하고 그 위에 Compose `Canvas`를 오버레이로 겹쳐서(`Box` 사용) 배치.
- MediaPipe에서 넘어온 `HandLandmark.points` 21개 랜드마크 데이터에 대해 관절 연결 정보(`HAND_CONNECTIONS`)를 정의하고, `Canvas`의 `drawLine`과 `drawCircle`을 이용해 녹색 뼈대(연결선)와 빨간색 점(관절 포인트)을 실시간으로 그리도록 구현.

**이슈:**
1. **스켈레톤 렌더링 방향 및 좌표 뒤틀림 현상:** 실제 손의 방향과 일치하지 않고 90도 또는 좌우 반전되어 나타남. CameraX 원본 이미지는 센서 물리 장착 방향(가로)대로 들어오나, MediaPipe가 이를 화면의 세로 방향으로 인지하지 못함.
2. **화면 스케일링에 따른 오프셋(위치) 왜곡 현상:** CameraX가 기본적으로 화면 비율을 맞추기 위해 4:3 해상도를 꽉 채워 자르는 `FILL_CENTER`로 동작했으나, 스켈레톤을 그리는 `Canvas`는 스마트폰 화면 전체 비율대로 계산을 시도하여 렌더링 좌표가 실제 손가락 위치에서 붕 뜨는 현상.

**수정내용:**
1. 카메라 프레임 방향 보정: `HandRepositoryImpl`에서 `Bitmap` 객체를 뷰어 쪽에 전달하기 전에 `android.graphics.Matrix`의 `postRotate()`를 사용해 `rotationDegrees` 만큼 물리적으로 이미지를 정방향으로 90도 회전시켜 넘김으로써 MediaPipe가 방향을 온전히 처리할 수 있게 해결.
2. 렌더링 뷰 스케일링 설정 변경: 스켈레톤 좌표가 왜곡되지 않고 일치할 수 있도록 CameraX `PreviewView`의 `ScaleType`을 전체 화면 영역 안에 여백을 두고 포함되는 형태인 `FIT_CENTER`로 변경.
3. 캔버스 오프셋 수식 보정: `Canvas` 내 좌표계 변환 시 화면 너비/높이 대비 카메라 버퍼 원본 해상도 비율(`size` vs `imageSize`)을 `minOf`로 계산해 스케일(`scale`) 계수를 구하고, 화면 중앙 배치에 맞춰 떨어지는 여백을 `startX`, `startY` 오프셋으로 추출하여 스켈레톤의 `Offset` 계산식에 반영. 전면 카메라 좌우 반전은 보정 오프셋 안에서 `(1f - x)` 형태로 변경.
