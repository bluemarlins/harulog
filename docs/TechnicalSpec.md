# 기술 스택 및 데이터 모델 구현 사양

## 1. 기술 스택 (Tech Stack)
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Database:** Room DB (로컬 데이터 저장)
* **Asynchronous:** Kotlin Coroutines & Flow
* **DI:** Hilt

---

## 2. 로컬 데이터베이스 스키마 정의 (Room Entities)

### 2.1 카테고리 정의 (Enum)
```kotlin
enum class CategoryType { WORK, PERSONAL }
```

### 2.2 일정 및 할 일 통합 테이블 (todo_schedule)
```kotlin
@Entity(tableName = "todo_schedule")
data class TodoScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String?,
    val isTodo: Boolean,             // true: 할 일, false: 일정
    val eventDate: LocalDate,        // 일정 날짜 또는 할 일 수행일
    val startTime: LocalTime?,       // 일정 시작 시간 (할 일은 null 가능)
    val endTime: LocalTime?,         // 일정 종료 시간 (할 일은 null 가능)
    val category: CategoryType,      // WORK 또는 PERSONAL
    val isCompleted: Boolean = false, // 할 일 완료 여부
    val isMonthlyScope: Boolean = false // true: 이번 달 안에 해야 하는 것 (날짜 지정 무관)
)
```

### 2.3 다이어리 테이블 (diary)
```kotlin
@Entity(tableName = "diary")
data class DiaryEntity(
    @PrimaryKey val date: LocalDate, // 1일 1기록 원칙으로 날짜를 기본키로 지정
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 2.4 운동 스티커 테이블 (exercise_sticker)
```kotlin
@Entity(tableName = "exercise_sticker")
data class ExerciseStickerEntity(
    @PrimaryKey val date: LocalDate,
    val isExercised: Boolean
)
```

---

## 3. 백업 및 복원 구현 기술 사양
* **메커니즘**: Android Storage Access Framework(SAF)를 이용한 로컬 JSON 파일 Export/Import 시스템.
* **백업 과정**: `TodoScheduleEntity`, `DiaryEntity`, `ExerciseStickerEntity` 전체 데이터를 조회하여 하나의 통합된 JSON 문자열로 직렬화(Gson 또는 Kotlinx Serialization 사용) 후 사용자가 지정한 저장소에 `.json` 파일로 저장.
* **복원 과정**: 사용자가 선택한 JSON 파일을 읽어와 각 엔티티 리스트로 역직렬화한 뒤, Room DB에 `OnConflictStrategy.REPLACE` 전략으로 일괄 삽입(Upsert).
