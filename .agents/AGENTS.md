# AI Agent 개발 및 추적성(Traceability) 가이드라인

본 문서는 `harulog` 프로젝트를 AI Agent가 자율적이고 일관되게 개발하기 위한 기본 원칙과 개발 이력 추적성(Traceability) 규격을 정의한다. 에이전트는 코드 및 구조 변경을 수행할 때 본 가이드라인을 최우선 원칙으로 준수해야 한다.

---

## 1. 핵심 개발 원칙 (Core Development Principles)

### 1.1 개발 환경 및 플랫폼 스펙 (Environment Specs)
* **Kotlin & Jetpack Compose**: 모든 UI 개발은 선언형 UI 프레임워크인 Jetpack Compose와 최신 Kotlin Idiom(Coroutines, Flow, Serialization 등)을 사용하여 개발한다.
* **최소 지원 SDK (minSDK 33)**: 앱의 최소 지원 API 레벨은 **33 (Android 13)**으로 제한하며, 코드 생성 및 API 사용 시 API level 33 이상에서 호환되는 현대적인 Android API를 우선적으로 사용한다. 하위 호환 분기는 불필요하다.

### 1.2 UI와 비즈니스 로직의 엄격한 분리 (UDF)
* **Stateless UI**: 모든 컴포저블(Composable)은 가급적 상태를 직접 가지지 않고, 외부에서 전달된 `UiState`와 이벤트 콜백만 바인딩해야 한다.
* **ViewModel 중심의 상태 관리**: 화면의 모든 비즈니스 로직, 상태 전이 및 Repository 연동은 해당 화면의 `ViewModel`에서 단방향 데이터 흐름(Unidirectional Data Flow)으로 처리한다.
* **상호 영향 최소화**: UI 레이아웃의 디자인 변경이 데이터 도메인 레이어에 영향을 미쳐서는 안 되며, 반대로 DB 스키마나 비즈니스 규칙 변경이 UI 컴포저블의 구조를 깨뜨리지 않도록 중간 데이터 전송 객체(State DTO)를 철저히 캡슐화한다.
* **생명주기 안전 상태 수집**: Compose 화면 단에서 Flow를 수집하여 상태로 바인딩할 때는 항상 리소스 누수 방지를 위해 `collectAsStateWithLifecycle()`을 사용한다.

### 1.3 UI 변경 시 디자인 시스템 및 모바일 디자인 원칙 준수
* UI 구성 요소를 추가하거나 변경할 때는 항상 [Design.md](file:///e:/Workspace/Source/harulog/docs/Design.md) 문서의 컬러 파레트 토큰(`LightPrimary`, `SuccessWorkoutColor` 등), 여백 시스템(Spacing & Grid), 타이포그래피 사양을 사전 참조하여 설계 규격을 100% 일치시켜야 한다.
* 임의의 하드코딩된 색상값이나 폰트 크기, 마진 패딩의 개별 사용을 금지한다.
* 또한, 모바일 기기의 터치 감각과 가독성을 보장하기 위해 [Design.md](file:///e:/Workspace/Source/harulog/docs/Design.md)에 정의된 **'7. 모바일 UI 디자인 원칙'**을 엄격히 적용한다.
  * **터치 영역**: 모든 clickable 요소는 최소 `48.dp x 48.dp` 크기의 터치 타겟을 확보한다.
  * **시각 대비**: 조도 변화에 대응하도록 명도 대비 비율(최소 4.5:1 이상)을 충족하고 다크 모드에 맞는 눈 피로 완화 색상 체계를 적용한다.
  * **피드백**: 대화형 컴포넌트는 Ripple Effect 등의 터치 피드백을 반드시 바인딩한다.
  * **레이아웃**: 빈번한 조작 액션은 하단(Thumb Zone) 배치를 지향한다.

### 1.4 코드 빌드 무오류 원칙 (Build & Safety Rule)
* **빌드 오류 발생 엄금**: 에이전트가 코드를 작성하거나 수정한 후, 최종 코드는 어떠한 컴파일 에러나 빌드 에러도 없어야 한다.
* **명확한 임포트(Import) 준수**: 코드 내 사용되는 모든 클래스 및 함수는 명확히 임포트되어 있어야 하며, Placeholder 임포트나 와일드카드(`.*`)는 지양한다.
* **사전 빌드 테스트 검증**: 에이전트는 코드 수정을 마친 후, 직접 터미널을 통해 `./gradlew assembleDebug` 또는 `./gradlew test` 등의 빌드 검사 커맨드를 가동하여 코드 변경으로 인한 빌드 크래시가 없음을 검증해야 한다.

---

## 2. 추적성 및 히스토리 관리 규칙 (Traceability Rules)

개발 과정이 중단되거나 중간에 LLM 모델이 변경되더라도, 최소한의 콘텍스트 토큰만 소비하여 신속하게 이전 맥락을 파악(Catch-up)할 수 있도록 변경 사양을 실시간으로 문서화한다.

### 2.1 데이터베이스 구조 변경 (DB Changelog)
* Room Entity 추가/삭제, 컬럼 변경, TypeConverter 적용 등 DB 스키마의 변경이 발생하면, 반드시 [DbChangelog.md](file:///e:/Workspace/Source/harulog/docs/DbChangelog.md) 파일에 변경 일자, 변경 내용, 마이그레이션 적용 여부를 기록한다.

### 2.2 소프트웨어 아키텍처 큰 변경 (Architecture Update)
* 프로젝트 패키지 구조 재배치, 새로운 아키텍처 컴포넌트(Hilt DI 모듈 도입, 새로운 데이터 플로우 패턴 등)의 도입 등 설계 프레임워크에 큰 변화가 있을 시, 즉시 [ArchitectureGuide.md](file:///e:/Workspace/Source/harulog/docs/ArchitectureGuide.md) 사양서의 텍스트 및 다이어그램(Mermaid)을 최신화한다.

### 2.3 기능 및 사용자 요구사항 변경 (PRD Update)
* 대화를 통해 새로운 기능 요구사항이 합의되거나, 기존 요구사항의 범위가 변경될 경우 코드 수정 전에 우선적으로 [PRD.md](file:///e:/Workspace/Source/harulog/docs/PRD.md) 파일의 기능 명세 및 사용자 시나리오 영역을 업데이트한다.
* **하네스 엔지니어링 및 중요 변경**: 하네스 엔지니어링을 위해 중요한 코드 및 기능 변경이 발생할 때에는 반드시 [PRD.md](file:///e:/Workspace/Source/harulog/docs/PRD.md) 업데이트도 병행하여 처리한다.

---

## 3. 사용자 시나리오 기반 골든 테스트 (Golden Test) 도입 원칙

에이전트가 코드를 안전하게 수정하고 UI의 레이아웃 깨짐(Clipping) 현상을 사전에 완벽히 자동 차단하기 위해 **골든 테스트(Visual Regression Test)**를 도입 및 실행한다.

### 3.1 골든 테스트 아키텍처
* **도구**: JVM 상에서 에뮬레이터 없이 렌더링 검증이 가능한 **Roborazzi** 또는 **Paparazzi** 프레임워크를 적용한다.
* **테스트 대상 시나리오**:
  1. **캘린더 월간 뷰 (Month View)**: 스티커 배지 및 카테고리 닷이 포함된 그리드가 온전히 렌더링되는지 확인.
  2. **캘린더 일간 완료 동작 (Day View Toggle)**: 성취 토글 시 녹색 원형 성취 스티커 렌더링 검증.
  3. **대시보드 일정/할 일 목록 (Dashboard Pane)**: Work(딥블루), Personal(코랄) 카드가 명세대로 잘 보이고 텍스트가 생략되지 않는지 검증.
* **하네스 실행**: 코드 수정이 가해질 때마다 `.\gradlew recordRoborazziDebug` 또는 `.\gradlew verifyRoborazziDebug`를 수행하여 기존 골든 스크린샷 이미지와 픽셀 단위 비교 검증을 수행한다.

### 3.2 실기기 화면 검증 제한 원칙
* **스크린샷 검증 제약**: 연결된 디바이스(실제 기기 또는 에뮬레이터)에서 스크린샷을 캡처하여 화면 구성을 검증하는 작업은 사용자가 프롬프트(Prompt)를 통해 명백하게 이를 요청했을 때만 수행한다.
* **명시적 요청이 없는 경우**: 프롬프트에 명시적인 스크린샷 검증 지시가 포함되어 있지 않은 경우에는 디바이스 스크린샷을 통한 화면 구성 검증 작업을 수행하지 않는다.

---

## 4. 커뮤니케이션 및 토큰 최적화 원칙 (Communication & Token Optimization Rules)

* **한국어 답변**: 사용자와의 모든 대화는 한국어로 답변한다.
* **토큰 최적화**: 상세한 중간 개발 과정이나 장황한 분석 과정의 보고는 생략하거나 최소화한다.
* **의사 결정 및 요약 집중**:
  * 변경에 대한 의사 결정 포인트(이유와 근거)를 간략히 명시한다.
  * 수정되거나 추가된 작업에 대한 핵심 요약만 작성하여 제공한다.
