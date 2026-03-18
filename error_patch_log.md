# 🛠 Error & Patch Log

### 1. Gradle Sync 및 DSL 에러
- **문제**: Groovy(`build.gradle`)와 Kotlin DSL(`build.gradle.kts`) 문법 혼용으로 `isMinifyEnabled` 등을 인식하지 못함.
- **해결**: 모든 설정 파일을 `.kts`로 통일하고 `include(":app")` 등 괄호 문법 적용.

### 2. Cleartext (HTTP) 통신 에러
- **문제**: Android 9 이상에서 `http://` 로컬 IP 서버와 통신 불가 (`CLEARTEXT_NOT_PERMITTED`).
- **해결**: `AndroidManifest.xml`에 `android:usesCleartextTraffic="true"` 설정 추가.

### 3. Google Drive 라이브러리 참조 에러
- **문제**: `Unresolved reference: api` 및 특정 버전의 구글 드라이브 라이브러리를 찾지 못함.
- **해결**: 메이븐 저장소에서 검증된 `v3-rev197-1.25.0` 버전으로 고정하고, `META-INF` 중복 리소스 제외(`excludes`) 설정 추가.

### 4. FFmpeg 변환 중 연결 끊김 (IOException)
- **문제**: 서버에서 무거운 FFmpeg 작업을 할 때 앱의 타임아웃이 발생하거나 연결이 끊겨 무한 대기.
- **해결**: `OkHttpClient` 타임아웃을 30초로 연장하고, `HomeViewModel`에 연결 실패 시 최대 5회 **지능적 재시도(Retry)** 로직 탑재.

### 5. Compose 상태 관리 및 Navigation 문법 에러
- **문제**: `remember` 참조 에러, `popUpTo` 오타, `saveState`를 private 블록 밖에서 사용하는 문법 에러 발생.
- **해결**: 표준 Navigation DSL 문법으로 수정 및 Composable 범위 내로 `remember` 로직 이동.

### 7. Material 3 & Compose 최신 문법 빌드 에러
- **문제**: `HorizontalDivider`, `ListItem`의 `text` 파라미터, `LinearProgressIndicator`의 람다 `progress` 등이 특정 라이브러리 버전에서 `Unresolved reference` 발생.
- **해결**: 
    - `HorizontalDivider` 대신 안정적인 `Divider()` 사용.
    - `ListItem`은 `headlineContent = { Text(...) }` 형식을 강제하여 최신 M3 표준 준수.
    - `LinearProgressIndicator`는 직접 값 대입 형식(`progress = float`)으로 호환성 확보.

### 8. OptIn 어노테이션 임포트 충돌
- **문제**: `androidx.annotation.OptIn` 사용 시 실험적 API가 제대로 활성화되지 않거나 충돌 발생.
- **해결**: 모든 파일에서 `androidx.annotation.OptIn`을 제거하고 **`kotlin.OptIn`**으로 통일하여 해결.

### 9. State Delegate (`by`) 및 Runtime State 에러
- **문제**: `mutableIntStateOf`, `mutableLongStateOf`를 사용할 때 `getValue`, `setValue` 임포트 누락으로 `by` 키워드 작동 불능.
- **해결**: `androidx.compose.runtime.*` (특히 `getValue`, `setValue`) 임포트를 명시적으로 추가하여 해결.
