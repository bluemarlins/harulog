# 안드로이드 SW 아키텍처 사양서

본 프로젝트는 AI 기반 코드 생성 환경에서 일관되고 예측 가능한 구조를 유지하기 위해 **Modern Android Architecture(공식 권장 가이드)** 및 **단방향 데이터 흐름(UDF)**을 엄격히 준수한다.

## 1. 단일 모듈 및 패키지 구조 (Single Module Structure)
그라들(Gradle) 오버헤드를 줄이고 AI 프롬프트를 단순화하기 위해 `app` 단일 모듈 구조를 채택하며, 기능 및 레이어별로 패키지를 분리한다.

```text
com.example.harulog
├── data
│   ├── local
│   │   ├── dao          # Room 데이터 접근 객체
│   │   ├── entity       # 데이터베이스 엔티티
│   │   └── AppDatabase.kt
│   └── repository       # 데이터 소스 중재 및 비즈니스 로직 캡슐화
├── di                   # Hilt 의존성 주입 모듈
├── ui
│   ├── theme            # Design System (Color, Type, Theme)
│   ├── calendar         # 캘린더 화면 컴포저블 및 ViewModel
│   ├── todo             # 모아보기 화면 컴포저블 및 ViewModel
│   ├── diary            # 다이어리 화면 컴포저블 및 ViewModel
│   └── common           # 공통 UI 컴포넌트 (카드, 칩 등)
└── utils                # 백업/복원, 날짜 관련 확장 함수
```

## 2. 아키텍처 레이어 지침

### 2.1 UI 레이어 (UI Layer)
* **Jetpack Compose + Jetpack ViewModel** 조합 사용.
* **단방향 데이터 흐름(UDF) 규칙**:
  * ViewModel은 상태를 StateFlow 형태의 `UiState` 단 하나로 묶어 화면에 단방향으로 전달한다.
  * Screen 컴포저블은 Stateless하게 유지하며, 사용자 이벤트는 ViewModel의 메서드를 호출하는 단방향 액션으로만 전달한다.
  * 데이터 구독 시 수명 주기를 안전하게 관리하기 위해 `collectAsStateWithLifecycle()`을 사용한다.
* **Edge-to-Edge 및 시스템 인셋 대응**:
  * 화면이 시스템 바(상태 바, 내비게이션 바) 영역 뒤까지 넓게 채워지도록 Edge-to-Edge 구조를 채택한다.
  * 최상위 Scaffold의 `paddingValues`를 하위 스크린의 리스트(LazyColumn, LazyVerticalGrid 등)에 전달하여 `contentPadding`으로 처리하고, 입력 UI에는 `imePadding()`을 부모 컨테이너에 지정하여 시스템 바 및 키보드와의 겹침을 방지한다.

### 2.2 데이터 레이어 (Data Layer)
* **Repository 패턴**: UI 레이어(ViewModel)는 오직 Repository 인터페이스만 참조하며, 데이터 소스의 구체적인 구현(Room DB)은 숨긴다.
* **리액티브 스트림**: 데이터베이스 변경 시 UI가 리액티브하게 반응하도록 데이터 전송 스트림은 Kotlin Flow를 활용한다.

### 2.3 의존성 주입 (Dependency Injection)
* 객체 간의 결합도를 낮추고 테스트 및 확장성을 확보하기 위해 **Hilt**를 필수적으로 적용한다.
