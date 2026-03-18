# 📍 Current Project Status & Architecture

## 🏛 App Architecture
- **Shared ViewModel Structure**: `MainActivity`에서 ViewModels를 생성하고 모든 화면(`HomeScreen`, `LibraryScreen`, `SettingsScreen`)에 공유.
    - 장점: 탭 전환 시 데이터가 휘발되지 않고 유지됨 (검색 결과, 진행률 등).
- **Hybrid Acquisition Flow**:
    1. 서버 다운로드 (`task_id` 기반)
    2. 서버 완료 즉시 자동 휴대폰 저장 (`Downloads` 폴더)
    3. 필요 시 구글 드라이브 업로드 (서버 기반 혹은 모바일 직접 기반)

## 🌐 Server & Connectivity
- **Backend**: FastAPI (Python 3.10+)
- **Storage**: `server/downloads/{task_id}/` 경로에 작업 격리 저장.
- **Cleanup**: 생성 후 30분 뒤 자동 삭제 시스템 가동.
- **Connection**: `10.0.2.2` (에뮬레이터) 혹은 Tailscale 기반 로컬 IP 통신.

## 🔜 Next Action Items (Phase 7 & Beyond)
1. **백그라운드 재생 및 미디어 서비스**:
    - `MediaSessionService` 연동을 통한 알림창 컨트롤(Notification) 및 앱 종료 후에도 오디오 유지.
    - 블루투스 이어폰 제어 및 잠금화면 컨트롤 지원.
2. **자막 기능 심화**:
    - 실제 유튜브 자막 파일(VTT/SRT) 자동 다운로드 및 파일 기반 동기화 엔진 완성.
3. **재생 설정 및 편의 기능**:
    - 재생 속도 조절 (0.5x ~ 2.0x).
    - 화면 회전(Landscape) 대응 및 자동 가로 모드 전환 옵션.
4. **Library 관리 고도화**:
    - 파일 삭제 기능 구현 및 로컬 파일 메타데이터(ID3 Tag) 편집 기초.

## 🏆 Completed Progress (Recent)
- [x] Phase 6-2: 플레이어 UX 고도화 (제스처, HUD, BottomSheet 재생목록)
- [x] Phase 6-1: Media3 ExoPlayer 기초 엔진 탑재
