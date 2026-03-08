# 🛠 Project Implementation Guide: Google Music (YouTube Downloader & Player)

이 문서는 `README.md`의 비전을 실행하기 위한 구체적인 개발 가이드라인 및 기술적 의사결정 사항을 담고 있습니다.

## 1. 개발 환경 및 아키텍처
- **Client**: Android (Kotlin, Jetpack Compose, Material 3)
- **Server**: Ubuntu Desktop (ASUS X502C 구형 노트북 활용)
- **Backend Stack**: Python (FastAPI), yt-dlp, FFmpeg
- **Storage**: Local Phone Storage + Google Drive API (v3)
- **Distribution**: APK 직접 배포 (Sideloading) - Play Store 정책 회피

## 2. UI/UX 디자인 원칙 (Jetpack Compose)
우선적으로 UI를 구축하며, 다음 화면들을 순차적으로 구현함:

### A. Home Screen (다운로드 및 분석)
- 유튜브 URL 입력 필드 (TextField) 및 '분석/다운로드' 버튼.
- 클립보드 자동 감지 기능: 앱 포커스 시 유튜브 링크 여부 확인 후 자동 붙여넣기 제안.
- 현재 진행 중인 다운로드 목록 (LinearProgressIndicator 활용).

### B. Library Screen (미디어 관리)
- 저장된 파일 리스트 (MP3/MP4 구분).
- 로컬 저장소와 구글 드라이브 아이콘으로 저장 위치 표시.
- 스와이프를 통한 파일 삭제 또는 드라이브 업로드 트리거.

### C. Player Screen (미디어 재생)
- **ExoPlayer (Media3)** 기반 고성능 재생기.
- 하단 미니 플레이어 (앱 어디서나 접근 가능).
- 전체 화면 플레이어: 앨범 아트, 가사/자막 표시, 재생 컨트롤.

### D. Settings Screen
- **서버 설정**: 우분투 서버의 고정 IP 주소 및 포트 설정.
- **다운로드 품질**: 비디오(1080p, 720p 등) 및 오디오 품질 선택.
- **인증**: 구글 계정 로그인/로그아웃 관리.

## 3. 서버(Ubuntu) 구성 가이드 (ASUS X502C)
- **접근**: SSH를 통한 원격 관리 (`openssh-server`).
- **지속성**: 노트북 덮개를 닫아도 서버가 유지되도록 `logind.conf` 수정 (`HandleLidSwitch=ignore`).
- **고정 IP**: 공유기에서 DHCP 고정 할당을 통해 내부 IP 고정.
- **API 서버**: FastAPI를 사용하여 `yt-dlp` 실행 결과를 JSON으로 앱에 전달.

## 4. 단계별 구현 로드맵 (Immediate Action Items)

### Phase 1: UI 스캐폴딩 (완료)
- [x] Jetpack Compose 프로젝트 초기화 및 Navigation Compose 설정.
- [x] 홈, 라이브러리, 설정 화면의 기본 레이아웃 작성.
- [x] Material 3 테마 및 다크 모드 적용.

### Phase 2: 서버 통신 및 인증
- [ ] 구글 로그인 (OAuth 2.0) 연동.
- [ ] 서버 IP 설정을 위한 DataStore(또는 SharedPreferences) 연동.
- [ ] FastAPI 기초 엔드포인트와 앱 간의 Retrofit/Ktor 통신 테스트.

### Phase 3: 미디어 엔진 및 다운로드
- [ ] Media3(ExoPlayer) 연동 및 백그라운드 재생 서비스.
- [ ] 서버단 `yt-dlp` 로직 구현 및 파일 전송 스트림 구축.
- [ ] 구글 드라이브 API를 통한 파일 업로드 구현.

## 5. 특이사항
- **APK 배포**: 앱 내부에 자체 업데이트 체크 로직을 포함하여, 새로운 APK가 서버에 올라오면 사용자에게 알림.
- **보안**: 서버는 개인 공유기 내부망에서 동작하므로 포트 포워딩 또는 VPN(Tailscale 등) 고려.