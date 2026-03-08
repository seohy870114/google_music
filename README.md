# 🎵 Google Music (YouTube Downloader & Player)

YouTube 영상을 고화질 MP4(자막 포함) 및 고음질 MP3로 변환하여 로컬 디바이스 또는 구글 드라이브에 저장하고 즐길 수 있는 안드로이드 애플리케이션 프로젝트입니다.

## 🚀 주요 기능
- **스마트 다운로드**: 유튜브 URL을 입력하여 MP4(영상) 또는 MP3(오디오)로 변환 및 저장.
- **자막 인베딩**: 설정에 따라 한국어/영어 자막을 영상 파일 내부에 포함.
- **로컬 재생**: 휴대폰 내부 저장 공간을 활용한 오프라인 미디어 재생.
- **클라우드 동기화**: 본인의 구글 계정으로 로그인하여 **Google Drive**에 직접 파일을 업로드하고 관리.
- **멀티태스킹**: 백그라운드 다운로드 및 백그라운드 오디오 재생 지원.

## 🛠 기술 스택 (기술적 제안)
- **개발 도구**: Android Studio (Android 12+ 최적화)
- **언어**: Kotlin (Jetpack Compose UI)
- **핵심 엔진**: `yt-dlp` (Python 기반 엔진을 안드로이드에서 실행하거나 백엔드 API 서버와 연동)
- **클라우드 API**: Google Drive API (v3)
- **인증**: Google Sign-In (OAuth 2.0)

## 🏗 시스템 아키텍처 (제안)
사용자의 휴대폰 성능과 유튜브의 차단 정책을 고려할 때 두 가지 방식 중 선택이 필요합니다.

1. **Client-Server 방식 (권장)**:
   - **Android App**: UI 및 재생 역할.
   - **Ubuntu Server (개인 서버)**: `yt-dlp`를 실행하여 영상을 변환하고 사용자 구글 드라이브로 직접 쏘거나 앱으로 전달. (서버 IP가 차단되지 않도록 관리 용이)
2. **On-Device 방식**:
   - **Android App** 내부에서 `python-for-android` 등을 이용해 직접 `yt-dlp` 실행. (기기 성능에 따라 속도 차이가 있고 유튜브 차단에 더 민감함)

## ⚠️ Play Store 출시 주의사항
- **정책**: YouTube 다운로드 기능은 구글 플레이 스토어 정책(YouTube 약관 위반)으로 인해 **정식 출시가 거절될 가능성이 매우 높습니다.**
- **대안**: 
  - 개인용 `.apk` 파일 배포 (Sideloading)
  - '오픈소스 미디어 플레이어' 컨셉으로 기능을 분리하여 관리
  - 깃허브(GitHub)를 통한 배포

## 📅 향후 계획 (Action Items)
- [ ] Android Studio 프로젝트 스캐폴딩 생성
- [ ] Kotlin 기반의 구글 계정 로그인 (Auth) 구현
- [ ] Google Drive API 연동 및 파일 업로드 테스트
- [ ] Ubuntu 서버의 `yt-dlp` 로직을 REST API로 변환하여 앱과 통신 구현
