# 📜 Google Music Development History (Phase 1 ~ 6)

## Phase 1: UI 스캐폴딩 및 기초 (완료)
- Jetpack Compose 및 Material 3 기반 기본 레이아웃 구축.
- Home, Library, Settings 3개 화면 구성 및 Navigation 설정.
- 다크 모드/라이트 모드 테마 적용.

## Phase 2: 설정 및 데이터 저장 (완료)
- Preferences DataStore를 통한 서버 IP 주소 영구 저장 로직 구현.
- SettingsViewModel을 통해 UI와 데이터 저장소 연결.

## Phase 3: 서버-앱 통신 기초 (완료)
- FastAPI(Python) 기반 백엔드 서버 구축.
- `yt-dlp` 연동을 통한 유튜브 URL 정보(제목, 썸네일 등) 분석 엔드포인트 구현.
- Retrofit을 이용한 안드로이드-서버 간 API 통신 기초 확립.

## Phase 4 & 4.5: 다운로드 및 자동 저장 (완료)
- 서버 측 실시간 다운로드 진행률 추적 및 상태 API(`task_id` 기반) 구현.
- **하이브리드 저장**: 서버 다운로드 완료 시 앱이 자동으로 파일을 스트리밍 받아 휴대폰 `Downloads` 폴더에 저장.
- 작업 격리(`task_id` 폴더) 및 Video ID 기반 파일명 관리로 특수문자 문제 해결.

## Phase 5 & 5.5: 구글 드라이브 및 세션 유지 (완료)
- **클라우드 연동**: 구글 로그인(OAuth 2.0) 및 `DRIVE_FILE` 권한 획득.
- **하이브리드 업로드**: 
    1. 서버에서 바로 구글 드라이브로 업로드 (Server-to-Drive)
    2. 휴대폰 저장 파일을 구글 드라이브로 직접 업로드 (Device-to-Drive)
- **세션 유지**: MainActivity 수준에서 ViewModel을 관리하여 탭 이동 시 데이터 보존.
- **UI 개선**: URL Clear 버튼, 완료 상태에 따른 버튼 조건부 노출(`AnimatedVisibility`).

## Phase 6: 라이브러리 고도화 및 플레이어 엔진 (완료)
- Library 탭 분리: 음악(MP3)과 동영상(MP4)을 TabRow로 구분 및 필터링.
- **Media3 (ExoPlayer)** 도입: 기본적인 재생/일시정지 및 탐색 기능 구현.
- 플레이어 화면(`PlayerScreen`) 및 전용 ViewModel 구현.

### Phase 6 - Step 2: 플레이어 UX 고도화 (진행 중/완료)
- **제스처 컨트롤**: YouTube 스타일의 좌측(밝기), 우측(볼륨) 수직 드래그 및 더블 탭(5초 탐색) 구현.
- **HUD 오버레이**: 제스처 조절 시 수치를 직관적으로 보여주는 상단 프로그레스 바 HUD 추가.
- **자막 동기화**: 재생 시간 기반 자막 추출 엔진 기초 구현 및 하단 패널(Collapsed) 표시.
- **재생 목록 연동**: BottomSheet을 통한 접이식 리스트 구현, 현재 목록 확인 및 즉시 전환 기능.
- **상태 최적화**: 200ms 주기의 정밀한 위치 추적 및 화면 전환 시 재생 상태 유지 강화.
