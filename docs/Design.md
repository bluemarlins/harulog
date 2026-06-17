# UI/UX 및 디자인 시스템 가이드 (Material 3 기반)

모든 UI 컴포넌트는 Jetpack Compose의 `MaterialTheme`를 확장하여 정의하며, 시각적 일관성과 반응형 레이아웃을 보장한다.

## 1. 컬러 시스템 (Color Palette)
* **Primary:** Deep Blue / Indigo 계열 (일정 관리의 신뢰감과 깔끔함 부여)
* **Secondary / Success:** Emerald Green (운동 달성 체크 및 스티커 아이콘용)
* **Category Colors:**
  * **업무 (Work):** Blue / Navy Chip
  * **개인 (Personal):** Coral / Purple Chip
* **Surface / Background:** Light 모드 기준 Light Gray 배경과 Pure White 카드를 조합하여 가독성 확보 (다크모드 대응 구조 설계 고려).

## 2. 타이포그래피 (Typography)
* `Typography.titleLarge`: 연/월 타이틀, 주요 대형 헤더 (22sp, Bold)
* `Typography.bodyLarge`: 다이어리 본문 내용, 할 일 세부 텍스트 (16sp, Regular)
* `Typography.labelMedium`: 카테고리 태그 칩, 캘린더 날짜 숫자 (12sp, Medium)

## 3. 공통 UI 컴포넌트 정책
* **Card 컴포넌트:** 일정, 할 일, 다이어리는 모두 일관된 사각 라운드 카드 형태로 배치한다. (`shape = RoundedCornerShape(12.dp)`)
* **Sticker 컴포넌트:** 운동 체크 스티커는 캘린더의 각 날짜 셀 내부에 컴팩트한 배지(Badge) 또는 미니 아이콘 형태로 오버레이한다.

## 4. 반응형 레이아웃 정책 (Adaptive Layout)
* `WindowWidthSizeClass`를 기준으로 화면 크기를 감지한다.
* **Compact (일반 스마트폰 세로):**
  * 하단 네비게이션 바(Bottom Navigation) 적용.
  * 상단 캘린더 그리드 + 하단 상세 일정 리스트의 **세로 적층형 구조**.
* **Medium / Expanded (태블릿, 폴더블, 가로 모드):**
  * 좌측 고정 네비게이션 레일(Navigation Rail) 적용.
  * **Master-Detail 분할 뷰(Split View):** 좌측 영역에는 넓은 캘린더 그리드를 배치하고, 우측 영역에는 선택한 날짜의 할 일 목록 및 다이어리 작성란을 동시에 노출하여 화면 왜곡을 방지한다.
