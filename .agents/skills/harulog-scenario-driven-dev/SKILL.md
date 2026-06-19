---
name: harulog-scenario-driven-dev
description: >-
  사용자의 요구사항을 받아 시나리오 정의, 충돌 분석, 테스트 케이스 작성(TDD), 코드 구현 및 3회 자동 디버깅, 추적성 문서(PRD, DB Changelog, Architecture) 동기화, Git 커밋/푸시까지의 개발 전체 주기를 제어합니다.
---

# Harulog Scenario-Driven Development (시나리오 기반 개발 및 추적성 제어 스킬)

## Overview
본 스킬은 사용자가 자연어로 UI, 새로운 기능 요구사항 혹은 예외 처리 사양을 묘사했을 때, 에이전트가 이를 안정적으로 수렴하여 테스트 기반으로 점진 개발하고 문서 정합성을 100% 동기화하며 소스코드를 배포하는 일관된 자율 개발 흐름(TDD + Traceability)을 보장하기 위한 가이드라인 스킬입니다.

## Dependencies
* 본 스킬은 추가 의존성 없이 표준 파일 및 명령어 도구(`run_command`, `replace_file_content`, `write_to_file`)를 사용하여 동작합니다.

## Quick Start
사용자가 다음과 같은 프롬프트를 보냈을 때 본 스킬이 트리거됩니다:
> *"캘린더 화면에서 이전/다음 달로 넘겼을 때, 해당 달의 1일이 자동 선택되도록 시나리오를 만들고 기능을 구현해줘."*

이 경우 본 스킬의 **Workflow** 섹션에 기술된 6단계 동작 규격을 순서대로 충동 없이 실행해야 합니다.

## Workflow (가이드라인 수칙)

### 1. 요구사항 분석 및 신규 시나리오 정의
* 사용자의 입력을 분석하여 독립적인 **사용자 시나리오(User Scenario)** 및 기대 결과(Expected Outcomes)를 도출하고 구조화합니다.

### 2. 시나리오 충돌 감지 및 가이드 (Conflict Detection)
* 새로 정의한 시나리오가 기존 문서([docs/PRD.md](file:///e:/Workspace/Source/harulog/docs/PRD.md))에 기술된 규칙이나 기존 테스트([app/src/test/.../UserScenarioTest.kt](file:///e:/Workspace/Source/harulog/app/src/test/java/com/example/harulog/ui/main/UserScenarioTest.kt))와 충돌하는지 사전 분석합니다.
* **충돌 발견 시**: 즉시 개발 프로세스를 **중단(Block)**하고, 모순 상황을 사용자에게 명확히 리포트한 뒤 해결 방향을 협의합니다.

### 3. 테스트 케이스 선제 구축 (Test-Driven Development)
* 충돌이 없음을 확인하면, [UserScenarioTest.kt](file:///e:/Workspace/Source/harulog/app/src/test/java/com/example/harulog/ui/main/UserScenarioTest.kt) 테스트 스위트에 신규 시나리오를 충실히 모사한 JVM 로컬 테스트 함수를 먼저 작성합니다.
* 작성 후 `.\gradlew test`를 실행하여 새 테스트가 실패하는 것을 확인합니다.

### 4. 기능 구현 및 자동 디버깅 (Implementation & Debugging)
* 테스트를 통과시키기 위해 필요한 프로덕션 코드(ViewModel, Composable Screen 등)를 작성하거나 수정합니다.
* `.\gradlew test`를 실행하여 정합성을 검증합니다.
  * **빌드/테스트 실패 시**: 에러 로그를 분석하여 코드를 자동 수정하는 디버깅(Self-correction) 루프를 **최대 3회** 수행합니다.
  * **3회 시도 후에도 실패 시**: 작업을 중단하고 실패 로그와 분석 결과를 정리하여 사용자에게 가이드를 요청합니다.

### 5. 개발 추적성 동기화 (Traceability Sync)
기능 구현이 완료되면 반드시 아래 문서들을 순차적으로 검토하고 갱신하여 문서의 최신성을 보존합니다.
* **기능 요건**: [docs/PRD.md](file:///e:/Workspace/Source/harulog/docs/PRD.md)에 신규 사용자 시나리오 명세 및 히스토리 업데이트.
* **데이터 모델**: DB 구조 변경이 동반되었을 경우 [docs/DbChangelog.md](file:///e:/Workspace/Source/harulog/docs/DbChangelog.md)에 버전 기록 갱신.
* **아키텍처**: 시스템 설계 변경 시 [docs/ArchitectureGuide.md](file:///e:/Workspace/Source/harulog/docs/ArchitectureGuide.md) 업데이트.
* **스타일/컨벤션**: UI 컴포넌트 마진, 폰트, 컬러 등 정의 시 [docs/Design.md](file:///e:/Workspace/Source/harulog/docs/Design.md) 및 [docs/AgentGuidelines.md](file:///e:/Workspace/Source/harulog/docs/AgentGuidelines.md) 준수 여부 크로스 체크.

### 6. Git 커밋 및 원격 저장소 푸시
* 작업이 정상 검증되면 의미 있는 커밋 메시지(예: `feat: implement month-nav auto-selection scenario and tests`)를 작성하여 로컬 커밋을 생성하고 `develop` 브랜치로 푸시를 수행합니다.

## Common Mistakes
* **시나리오 충돌 검토 생략**: 기존의 동작 방식(예: 항상 월 단위로 보이는 캘린더 등)을 훼손하는 시나리오를 검증 없이 즉시 구현해 시스템 오동작을 초래하는 행동.
* **추적성 문서 무시**: 코드는 수정했으나 `PRD.md`나 `DbChangelog.md` 등을 방치하여, 다음 작업 시 에이전트의 컨텍스트 파악 실패로 인한 불필요한 토큰 낭비를 유발하는 경우.
* **테스트 생략 혹은 무시**: 빌드가 성공했으나 `.\gradlew test` 검증을 건너뛰어 회귀 버그(Regression Bug)를 인지하지 못하고 푸시하는 동작.
