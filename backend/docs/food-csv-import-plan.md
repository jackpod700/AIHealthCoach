# 음식 CSV 데이터 전처리 및 적재 플랜

## 목표

`data/foods/raw`에 있는 전국통합식품영양성분정보 CSV 3종을 전처리하여 PostgreSQL `foods` 테이블에 적재한다.

대상 파일:

- `data/foods/raw/전국통합식품영양성분정보_가공식품_표준데이터.csv`
- `data/foods/raw/전국통합식품영양성분정보_원재료성식품_표준데이터.csv`
- `data/foods/raw/전국통합식품영양성분정보_음식_표준데이터.csv`

## 폴더 구조

음식 데이터 적재는 백엔드 애플리케이션 로직이 아니라 데이터 운영 작업에 가깝기 때문에 루트의 `data/foods` 아래에서 관리한다.

```text
data/
└─ foods/
   ├─ raw/
   │  ├─ 전국통합식품영양성분정보_가공식품_표준데이터.csv
   │  ├─ 전국통합식품영양성분정보_원재료성식품_표준데이터.csv
   │  └─ 전국통합식품영양성분정보_음식_표준데이터.csv
   ├─ scripts/
   │  ├─ prepare_foods.py
   │  ├─ import-foods.sql
   │  └─ import_foods.ps1
   └─ build/
      ├─ processed-foods.csv
      └─ food-import-errors.csv
```

`data/foods/build`는 전처리 산출물 위치이며 Git 추적 대상에서 제외한다.

## 선택한 적재 방식

`정제 CSV 생성 -> PostgreSQL staging 테이블 COPY -> foods upsert` 방식으로 진행한다.

이 방식을 선택한 이유:

- CSV 원본 데이터가 많아 `INSERT` SQL을 직접 생성하는 방식보다 빠르다.
- `foods.code`를 기준으로 재실행 가능한 upsert가 가능하다.
- 원본 CSV, 정제 CSV, DB 적재 SQL의 관심사가 분리된다.
- Spring Boot 애플리케이션 리소스와 대량 데이터 적재 책임을 분리할 수 있다.

## 실행 방법

Docker Compose로 전체 프로젝트를 실행하면 음식 데이터도 자동 적재된다.

```powershell
docker compose up -d --build
```

Compose 실행 흐름:

1. `postgres`가 healthcheck를 통과한다.
2. `food-importer`가 CSV 전처리와 `foods` upsert를 수행한다.
3. `food-importer`가 성공하면 `backend`가 실행된다.
4. `backend` 실행 후 `frontend`가 실행된다.

전처리와 DB 적재를 한 번에 실행한다.

```powershell
.\data\foods\scripts\import_foods.ps1
```

전처리만 실행하려면 아래 명령을 사용한다.

```powershell
python data/foods/scripts/prepare_foods.py
```

생성 파일:

- `data/foods/build/processed-foods.csv`
- `data/foods/build/food-import-errors.csv`

## 컬럼 매핑

| CSV 컬럼 | foods 컬럼 | 처리 방식 |
| --- | --- | --- |
| `식품코드` | `code` | 필수값, PK. 앞뒤 공백 제거 |
| `식품명` | `name` | 필수값, 앞뒤 공백 제거 |
| `수입업체명` | `manufacturer` | 가공식품 파일에서 우선 사용 |
| `제조사명` | `manufacturer` | 가공식품 파일에서 `수입업체명`이 없을 때 사용 |
| `업체명` | `manufacturer` | 음식 파일에서 사용 |
| 없음 또는 빈 값 | `manufacturer` | 원재료성 식품은 `NULL` 허용 |
| `영양성분함량기준량` | `serving_size`, `serving_unit` | 예: `100g` -> `100.00`, `g` |
| `에너지(kcal)` | `calories` | 숫자 변환, 빈 값은 `0.00` |
| `탄수화물(g)` | `carbohydrate` | 숫자 변환, 빈 값은 `0.00` |
| `단백질(g)` | `protein` | 숫자 변환, 빈 값은 `0.00` |
| `지방(g)` | `fat` | 숫자 변환, 빈 값은 `0.00` |
| `나트륨(mg)` | `nat` | 숫자 변환, 빈 값은 `NULL` |
| `당류(g)` | `sugar` | 숫자 변환, 빈 값은 `NULL` |
| `수분(g)` | `water` | 숫자 변환, 빈 값은 `NULL` |
| `식이섬유(g)` | `dietary_fiber` | 숫자 변환, 빈 값은 `NULL` |
| `칼슘(mg)` | `calcium` | 숫자 변환, 빈 값은 `NULL` |
| `철(mg)` | `iron` | 숫자 변환, 빈 값은 `NULL` |
| `인(mg)` | `phosphorus` | 숫자 변환, 빈 값은 `NULL` |
| `칼륨(mg)` | `potassium` | 숫자 변환, 빈 값은 `NULL` |
| `비타민 A(μg RAE)` | `vitamin_a` | 숫자 변환, 빈 값은 `NULL` |
| `비타민 C(mg)` | `vitamin_c` | 숫자 변환, 빈 값은 `NULL` |
| `비타민 D(μg)` | `vitamin_d` | 숫자 변환, 빈 값은 `NULL` |
| `콜레스테롤(mg)` | `cholesterol` | 숫자 변환, 빈 값은 `NULL` |
| `포화지방산(g)` | `saturated_fat` | 숫자 변환, 빈 값은 `NULL` |
| `트랜스지방산(g)` | `trans_fat` | 숫자 변환, 빈 값은 `NULL` |

## 전처리 규칙

### 필수값 검증

아래 조건을 만족하지 않는 행은 적재하지 않고 오류 리포트에 기록한다.

- `식품코드`가 비어 있지 않아야 한다.
- `식품명`이 비어 있지 않아야 한다.
- `영양성분함량기준량`에서 기준량 숫자를 추출할 수 있어야 한다.
- `serving_size`는 0보다 커야 한다.
- `calories`, `carbohydrate`, `protein`, `fat`은 0 이상이어야 한다.

### 숫자 변환

- 빈 문자열: 필수 영양성분은 `0.00`, 선택 영양성분은 `NULL`
- `-`, `trace`, `Tr`: 필수 영양성분은 `0.00`, 선택 영양성분은 `NULL`
- 쉼표가 포함된 숫자: 쉼표 제거 후 숫자 변환
- 음수: 제약조건 위반이므로 해당 행 제외

### 기준량 파싱

`영양성분함량기준량` 값은 숫자와 단위를 분리한다.

| 원본 | serving_size | serving_unit |
| --- | --- | --- |
| `100g` | `100.00` | `g` |
| `100ml` | `100.00` | `ml` |
| `1개` | `1.00` | `개` |

단위를 분리할 수 없으면 기본 단위는 `g`로 두고 오류 리포트에 기록한다.

### 제조사 매핑

- 가공식품: `수입업체명`을 우선 사용하고, 값이 없을 때 `제조사명`을 사용
- 음식: `업체명`
- 원재료성 식품: `NULL`

가공식품에서 `수입업체명`과 `제조사명`이 모두 있으면 `수입업체명`을 저장한다. 선택된 제조사 값이 빈 문자열이거나 `해당없음`이면 `NULL`로 저장한다.

### 중복 처리

`foods.code`는 PK이므로 중복 코드는 한 건만 유지한다.

1. 같은 `code`가 여러 파일에 있으면 `데이터기준일자`가 가장 최신인 행을 사용한다.
2. 기준일자도 같으면 `가공식품 -> 음식 -> 원재료성식품` 우선순위를 적용한다.
3. 최종 적재는 `ON CONFLICT (code) DO UPDATE`를 사용한다.

## 적재 SQL 흐름

`data/foods/scripts/import-foods.sql`은 아래 순서로 동작한다.

1. 기존 `foods.manufacturer` 길이를 `VARCHAR(255)`로 보정한다.
2. `staging_foods` 임시 테이블을 만든다.
3. `data/foods/build/processed-foods.csv`를 컨테이너의 `/tmp/processed-foods.csv`로 복사한 뒤 `\copy`로 staging 테이블에 넣는다.
4. `foods` 테이블에 `INSERT ... ON CONFLICT (code) DO UPDATE`로 upsert한다.

## 검증 SQL

```sql
SELECT COUNT(*) AS food_count
FROM foods;
```

```sql
SELECT COUNT(*) AS invalid_required_values
FROM foods
WHERE code IS NULL
   OR length(trim(name)) = 0
   OR serving_size <= 0
   OR calories < 0
   OR carbohydrate < 0
   OR protein < 0
   OR fat < 0;
```

```sql
SELECT code, COUNT(*) AS duplicate_count
FROM foods
GROUP BY code
HAVING COUNT(*) > 1;
```

## 후속 작업

- `foods` 검색 API 추가
- 음식명 부분검색 또는 초성검색을 위한 인덱스 검토
- `meals`, `meal_items`와 연결되는 식단 기록 API 추가
