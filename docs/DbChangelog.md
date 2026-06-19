# 데이터베이스 스키마 변경 이력 (DB Changelog)

본 문서는 `harulog` 애플리케이션의 Room 로컬 데이터베이스 스키마 변경 사항과 마이그레이션 적용 역사를 기록한다. 스펙 변경으로 인해 엔티티 구조가 바뀔 때마다 변경 로그가 반드시 추가되어야 한다.

---

## [V2.0] - 2026-06-17
데이터베이스 통합 및 날짜/시간 표준화 리팩토링을 수행했다.

### 1. 테이블 통합 및 신규 엔티티 선언
* **`todo_schedule` 테이블 통합**:
  * 기존 개별 `todos` 및 `schedules` 테이블을 단일 `todo_schedule` 테이블로 통합했다.
  * 엔티티 클래스: `TodoScheduleEntity`
  * 추가 필드: `category` (WORK, PERSONAL 카테고리 구분용 Enum), `isMonthlyScope` (월간 단위 여부 플래그), `startTime`, `endTime`, `isCompleted` 등 포함.
* **`diary` 테이블 선언**:
  * 1일 1기록을 보장하도록 기본 키(PrimaryKey)를 `LocalDate`(`eventDate`)로 지정한 다이어리 기록 테이블을 선언했다.
  * 엔티티 클래스: `DiaryEntity`
* **`exercise_sticker` 테이블 선언**:
  * 날짜별 운동 완료 여부(`isExercised`)를 토글 및 보관하기 위한 전용 스티커 테이블을 선언했다.
  * 엔티티 클래스: `ExerciseStickerEntity`

### 2. 데이터 타입 표준화
* 기존의 `String` 기반 날짜/시간 포맷을 Java 표준인 `LocalDate` 및 `LocalTime` 타입으로 전면 마이그레이션했다.
* 이를 데이터베이스가 읽고 쓸 수 있는 문자열 형식으로 직렬화하기 위해 `Converters` 클래스를 연동하고 `@TypeConverters(Converters::class)`를 활성화했다.

### 3. 마이그레이션 정책
* 개발 및 구조 설계 고도화 단계의 유연성을 위해, 스키마 불일치 발생 시 즉시 리셋 후 재 생성하는 Destructive Migration 정책(`.fallbackToDestructiveMigration()`)을 임시 적용했다.

---

## [V1.0] - 2026-06-17 (이전 레거시 버전)
초기 프로토타입 릴리즈 버전의 데이터 모델 구조.
* **`todos` 테이블**: 문자열 ID 및 할 일 제목 저장.
* **`schedules` 테이블**: 시간 정보 및 이벤트 텍스트 저장.
