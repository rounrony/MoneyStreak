# 머니스트릭 (MoneyStreak)

소프트웨어 시스템 설계 과목 Checkpoint 4 (Beta release) 구현체.

## 실행 방법 (외부 라이브러리/빌드 도구 불필요, JDK만 있으면 됨)

프로젝트 루트(이 README가 있는 폴더)에서:

**macOS / Linux (bash):**
```bash
javac -d out $(find src -name "*.java")
java -cp out moneystreak.Main
```

**Windows 명령 프롬프트(cmd):**
```cmd
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
java -cp out moneystreak.Main
```

**Windows PowerShell:**
```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java -Path src).FullName
java -cp out moneystreak.Main
```

실행하면 `data/` 폴더가 자동으로 생성되고, 그 안에 거래/예산/스트릭/카테고리/그룹랭킹이
JSON 파일로 저장됩니다 (앱을 껐다 켜도 데이터는 유지됩니다).

## 구현된 기능 (베타, 7개)

| 기능 | 패키지/클래스 |
|---|---|
| F1 거래 CRUD | `manager.TransactionManager`, `ui.TransactionPanel` |
| F2 자동 분류 | `manager.AutoClassifier` |
| F3 캘린더 조회 | `manager.StatisticsEngine`, `ui.CalendarPanel` |
| F4 통계 및 시각화 | `manager.StatisticsEngine`, `ui.StatisticsPanel` |
| F5 예산 경고 | `manager.BudgetManager`, `ui.StatisticsPanel`(예산 설정 다이얼로그) |
| F7 스트릭 + 티어 | `manager.StreakManager`, `model.Tier` |
| F9 그룹 경쟁 + 공유 카드 | `manager.ShareManager`, `ui.GroupPanel` |

향후 구현 예정(이번 베타 범위 아님): F6 반복거래, F8 목표저축예측, F10 검색/필터/즐겨찾기.

## 패키지 구조

```
src/moneystreak/
├── model/      데이터 클래스 (Transaction, Category, Budget, StreakStatus, Tier, GroupRanking, StatEntry)
├── manager/    비즈니스 로직 (TransactionManager, AutoClassifier, BudgetManager, StreakManager,
│               StatisticsEngine, ShareManager)
├── store/      JSON 기반 영속성 (SimpleJson: 자체 구현 JSON 파서/라이터, DataStore: 파일 읽기/쓰기)
├── ui/         Swing GUI (MainFrame, TransactionPanel, CalendarPanel, StatisticsPanel, GroupPanel)
└── Main.java   실행 진입점
```

## 티어/점수 시스템 요약

- 티어: 브론즈 → 실버 → 골드 → 플래티넘 → 에메랄드 → 다이아몬드 → 마스터 → 그랜드마스터 → 챌린저
- 하루 거래를 기록하면: 예산 미설정 시 +100점, 예산 설정 시 스트릭 +60점 + (이번 달 한도를 아직 안 넘겼으면) 예산 +40점
- 결석(프리즈 없음) 시: 연속일수 0으로 리셋, -100점
- 이번 달 중 예산 한도를 처음 초과하는 순간: -100점(1회성)
- 매월 말, 그 달에 모은 점수가 현재 티어 유지 요구치(해당 티어로 승급할 때 필요했던 점수) 미달이면 한 단계 강등
- 예산을 한 번도 안 넘긴 달이 끝나면 스트릭 프리즈 1개 지급 (결석일을 자동으로 보호)
