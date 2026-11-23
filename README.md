# BE_06 · EnjoyTrip

지역별 관광정보를 조회하고, 여행 동선을 계획하며, 방문한 장소를 사진으로 기록하는 여행 서비스입니다. Servlet·JSP 기반 MVC 구조에 Service·DAO·DTO를 구성하고, JDBC로 MySQL의 관광정보·회원·커뮤니티·여행 데이터를 조회하고 저장합니다.

관광정보는 제공된 **SSAFY TRIP 관광 데이터 덤프를 MySQL에 적재하여 조회**합니다. 지도 표시와 주소의 좌표 변환에는 **Kakao Maps JavaScript API**를 사용합니다.

> 기준: 「자바전공_enjoytrip_16_06_DB.pdf」의 DB 설계·구현 과제와 기능 요구사항. 구현·실행 확인일: **2026-11-06**.

## 1. 프로젝트 목표

- 관광정보와 서비스 데이터를 엔티티별로 나누고 PK·FK·UNIQUE·CHECK 제약조건을 적용합니다.
- Controller → Service → DAO → MySQL로 역할을 분리하고 DTO로 데이터를 전달합니다.
- 기존 관광 API 조회와 브라우저 저장 중심의 기능을 DB 조회·저장으로 전환합니다.
- `EnjoyTripApp.java`의 main과 웹 화면에서 동일한 Service를 호출해 결과를 확인합니다.
- SQL 스키마·초기 데이터·Workbench ERD와 실제 실행 화면을 제출 자료로 정리합니다.

## 2. 요구사항과 구현 현황

체크는 **아래 설명한 범위가 현재 구현된 항목**에만 표시했습니다. PDF의 확장 예시 전체를 구현했다는 의미는 아닙니다.

### 필수 기능

- [x] **F101 지역별 관광정보** — 제공 관광 데이터를 DB에 적재하고 시·도, 시·군·구 선택으로 조회합니다.
- [x] **F102 관광지·숙박·음식점 조회** — 콘텐츠 유형 코드 12·32·39로 구분해 조회합니다.
- [x] **F103 문화시설·공연·여행코스·쇼핑 조회** — 콘텐츠 유형 코드 14·15·25·38로 조회합니다. 레포츠(28)도 지원합니다.
- [x] **F107 회원 관리** — 회원가입, 아이디 중복 확인, 본인 정보 조회·수정·탈퇴를 제공합니다.
- [x] **F108 로그인 관리** — 세션 로그인·로그아웃과 복구 질문·답변 확인 후 비밀번호 재설정을 제공합니다.

### 추가·심화 기능

- [x] **F104 여행계획 경로 설정** — 장소 추가, 저장 전 드래그 순서 변경, 제목·기간·예산·메모·방문 순서 저장, 본인 계획 조회·삭제와 지도 동선 표시를 제공합니다.
- [x] **F105 회원 주도 Hotplace 등록** — 사진·방문일·유형·장소명·주소·후기를 등록하고 본인의 저장 목록·사진·지도 위치를 조회하거나 삭제합니다.
- [x] **F109 공지사항** — 공개 목록·상세 조회와 관리자의 등록·수정·삭제를 제공합니다.
- [x] **F110 공유게시판** — 공개 목록·상세 조회, 로그인 회원의 글·댓글 등록, 본인 글 수정·삭제, 본인 댓글 삭제를 제공합니다.
- [ ] **F106 관광 뉴스 크롤링** — 미구현입니다.
- [ ] **F111 관광지 날씨** — 미구현입니다.
- [ ] **F112 관광지별 추천 사진** — DB의 대표 이미지 조회·표시는 구현했지만, 별도 사진 추천 기능은 구현하지 않았습니다.
- [ ] **F113 일출·일몰 시각** — 미구현입니다.
- [ ] **F114 전기차 충전소 위치·상태** — 미구현입니다.

### 주요 동작과 구현 범위

| 구분 | 구현 내용 |
|---|---|
| 관광정보 | 콘텐츠 유형·시도·시군구를 **선택 박스**로 지정하고 MySQL에서 검색·페이지 조회 |
| 주변 관광지 | 지도 기준 좌표와 DB 좌표의 거리를 `ST_Distance_Sphere`로 비교하여 조회 |
| 지도·사진 | 관광지 목록과 지도 마커 연동, 마커 선택 시 장소명·대표 이미지 표시 및 계획에 추가 |
| 회원 | 현재 비밀번호 확인 후 본인 정보 수정·탈퇴, 비밀번호·복구 답변 PBKDF2 해시 저장 |
| 여행계획 | 계획 기본 정보와 방문 장소를 하나의 트랜잭션으로 저장, 새로고침 후 DB에서 다시 조회 |
| Hotplace | 주소를 좌표로 변환하고 기록을 DB에 저장, 사진 파일은 서버의 별도 폴더에 보관 |
| 공지·게시판 | 공지는 관리자 권한 검사, 일반 글 수정·삭제는 작성자 계정과 별도 글 비밀번호 확인 |
| 댓글 | 게시글별 조회·등록, 본인 댓글 삭제, 게시글 삭제 시 댓글 함께 삭제 |

여행계획의 선은 **방문 순서대로 장소를 연결한 직선**입니다. 실제 도로 길찾기, 날짜별 일정 편집, 저장된 계획의 수정은 제공하지 않습니다. Hotplace는 로그인한 **본인만 조회하는 기록**이며 공개 공유 기능은 없습니다. 비밀번호 찾기는 이메일 인증이 아닌 복구 질문 방식입니다.

관광정보는 제공 덤프 시점의 데이터이며, 실행 중 TourAPI를 호출하거나 최신 데이터를 자동 수집하지 않습니다. 콘텐츠 유형 이름은 제공 DB 값을 사용하므로, 원본의 코드 39 이름이 `관광지`로 들어 있는 점은 후속 데이터 정제 항목입니다.

## 3. 실제 실행 화면

`http://localhost:8080/BE_06/`에서 Tomcat 11·MySQL 8.0.46으로 실행한 화면입니다. 지도 SDK와 실제 DB를 사용했으며, 캡처용 계정·공지·게시글·댓글·계획·핫플은 확인 후 정리했습니다. 초기 덤프에는 여행계획과 핫플 데이터가 없습니다.

### 3.1 메인 화면

관광정보 탐색·동선 수립, 나의 핫플, 공지사항과 공유게시판으로 이동합니다.

![EnjoyTrip 메인 화면](docs/screenshots/db/01-main.png)

### 3.2 지역·유형별 관광정보 조회

콘텐츠 유형·시도·시군구를 선택하여 조회한 결과를 목록과 지도 마커로 확인합니다. 아래는 서울 강남구 관광지 조회와 문화시설 조회 화면입니다.

![서울 강남구 관광정보 DB 조회](docs/screenshots/db/02-tour-search.png)

![문화시설 유형으로 DB 조회](docs/screenshots/db/03-tour-culture.png)

### 3.3 관광지 대표 이미지와 계획에 추가

목록이나 마커를 선택하면 장소명과 DB의 대표 이미지를 확인하고 여행계획에 장소를 추가할 수 있습니다.

![관광지 마커 대표 이미지](docs/screenshots/db/04-tour-photo.png)

### 3.4 회원가입과 로그인

아이디 사용 가능 여부를 확인하고 이름·비밀번호·복구 질문·답변을 입력하여 가입합니다. 로그인은 서버 세션으로 유지합니다.

![아이디 중복 확인과 회원가입](docs/screenshots/db/05-signup.png)

![회원 로그인](docs/screenshots/db/06-login.png)

### 3.5 내 정보 조회·수정·탈퇴

본인 정보를 조회하고 현재 비밀번호를 확인하여 수정하거나 탈퇴합니다. 탈퇴 시 로그인을 비활성화하고 이름을 익명화하며 기존 게시글·댓글의 참조 관계는 유지합니다.

![회원정보 수정과 탈퇴 화면](docs/screenshots/db/07-profile.png)

### 3.6 비밀번호 찾기

아이디로 복구 질문을 확인하고 가입 시 설정한 답변을 검증한 뒤 새 비밀번호를 설정합니다.

![복구 질문을 통한 비밀번호 재설정](docs/screenshots/db/08-recovery.png)

### 3.7 여행계획 저장과 동선 조회

실제 마커에서 두 장소를 추가하고, 저장 전에 방문 순서를 변경했습니다. 계획 저장 후 새로고침하여 DB에서 다시 불러온 목록과 직선 동선을 확인한 화면입니다.

![DB에 저장한 여행계획과 방문 동선](docs/screenshots/db/09-trip-plan.png)

### 3.8 나의 핫플 사진·지도·목록

사진을 업로드하고 방문일·주소·후기를 저장한 뒤 목록의 `지도에서 보기`로 위치를 확인합니다. 캡처용 사진은 제공 관광 DB의 [관광공사 대표 이미지](http://tong.visitkorea.or.kr/cms/resource/64/3432064_image2_1.jpg)를 사용했으며 직접 촬영한 사진이 아닙니다.

![본인의 Hotplace 사진과 지도 위치](docs/screenshots/db/10-hotplace.png)

### 3.9 관리자 공지사항

공지 목록과 상세는 누구나 조회할 수 있고, 등록·수정·삭제는 관리자에게 허용됩니다.

![관리자 공지사항 목록](docs/screenshots/db/11-notices.png)

### 3.10 공유게시판과 댓글

게시글 목록·상세와 댓글을 확인합니다. 본인 글은 작성 시 지정한 글 비밀번호를 확인하여 수정·삭제합니다.

![공유게시판 목록](docs/screenshots/db/12-board.png)

![게시글 상세와 댓글](docs/screenshots/db/13-post-detail.png)

### 3.11 모바일 화면

390px 화면에서 메뉴·관광정보 선택 영역이 세로로 배치되는 것을 확인했습니다.

<img src="docs/screenshots/db/14-mobile.png" alt="390px 모바일 관광정보 화면" width="390">

## 4. 데이터베이스 설계

기존 관광정보 **4개 테이블**에 서비스용 **6개 테이블**을 추가하여 총 **10개 테이블**로 구성했습니다.

### 4.1 SQL과 Workbench 모델

| 파일 | 용도 |
|---|---|
| [EnjoyTrip_Schema.sql](db/EnjoyTrip_Schema.sql) | 관광정보 4개와 서비스 6개 테이블의 DDL, PK·FK·인덱스·기본값·제약조건 |
| [EnjoyTrip_Dump.sql](db/EnjoyTrip_Dump.sql) | 제공 관광 데이터와 기존 STS 소스의 회원·공지·게시글·댓글 더미 |
| [EnjoyTrip.mwb](db/EnjoyTrip.mwb) | MySQL Workbench에서 열어 확인·편집할 수 있는 ERD 모델 |

![EnjoyTrip 전체 10개 테이블 ERD](docs/assets/db-erd.png)

### 4.2 테이블과 초기 데이터

| 테이블 | 역할 | 초기 행 수 |
|---|---|---:|
| `sidos` | 시도 코드·이름 | 17 |
| `guguns` | 시도별 구군 코드·이름 | 234 |
| `contenttypes` | 관광 콘텐츠 유형 | 8 |
| `attractions` | 관광지·좌표·주소·대표 이미지 등 | 50,915 |
| `members` | 로그인 계정·권한·해시 비밀번호·복구 정보 | 125 |
| `trip_plans` | 회원별 여행 제목·기간·예산·메모 | 0 |
| `trip_plan_places` | 계획별 관광지와 방문 순서 | 0 |
| `posts` | 공지와 일반 게시글 | 12 |
| `comments` | 게시글별 댓글 | 12 |
| `travel_records` | 회원별 핫플·방문 기록·사진 파일명 | 0 |

회원 125행은 기존 로그인 더미 101명과 게시글 표시용 작성자 24명입니다. 표시용 작성자는 로그인이 비활성화되어 있습니다. 게시글 12건은 공지 4건과 일반 글 8건이며, 여행계획·핫플에는 초기 더미를 추가하지 않았습니다.

### 4.3 테이블 분리와 관계·무결성

- 지역·유형 이름은 `sidos`, `guguns`, `contenttypes`로 분리하고 관광정보에서 코드로 참조합니다. 구군은 **시도 코드 + 구군 코드** 조합으로 식별합니다.
- 계획 기본 정보와 반복되는 방문 장소를 `trip_plans`·`trip_plan_places`로 분리했습니다. `(plan_id, visit_order)`를 유일하게 하여 방문 순서를 관리합니다.
- 관광지 참조는 외부 `content_id`와 구분되는 내부 PK **`attractions.no`**를 사용합니다.
- 공지와 일반 게시글은 `posts.post_type`으로 구분하고 댓글은 별도 `comments`로 분리했습니다.
- 핫플은 DB 관광 목록에 없는 직접 입력 장소도 허용하므로 장소명·주소·좌표를 기록 당시 값으로 보관합니다.
- 총 **11개 FK**로 참조 관계를 관리합니다. 계획 삭제 시 방문 항목, 게시글 삭제 시 댓글을 `ON DELETE CASCADE`로 삭제합니다.
- 회원 아이디의 UNIQUE, 기간·예산·좌표·방문 순서의 CHECK, 생성·수정 시각의 기본값을 적용했습니다.
- 지역·관광 유형 복합 인덱스와 회원별 계획·기록, 유형별 게시글 인덱스를 구성했습니다.

## 5. 기술 구성과 프로젝트 구조

| 영역 | 사용 기술 |
|---|---|
| 언어 | Java 21, JavaScript ES Modules, HTML, CSS |
| 웹 | Jakarta Servlet 6.1, JSP 4.0, JSTL 3.0 |
| 실행 서버 | Apache Tomcat 11 |
| DB | MySQL 8.0.16 이상, JDBC, MySQL Connector/J 8.4.0 |
| JSON | Jackson Databind 3.0.4 |
| 지도·주소 변환 | Kakao Maps JavaScript API·services 라이브러리 |
| 비밀번호·복구 답변 | PBKDF2WithHmacSHA256, 개별 salt |
| 빌드·개발 | Maven WAR, STS for Eclipse, Git, MySQL Workbench |

Spring Framework를 사용하지 않는 Servlet·JSP 프로젝트입니다. 메인 화면은 JSP와 JavaScript로 구성하며, 관광·회원·계획·커뮤니티 기능은 JSON 요청·응답을 사용합니다.

```text
BE_06/
├── README.md
├── pom.xml
├── db/
│   ├── EnjoyTrip_Schema.sql
│   ├── EnjoyTrip_Dump.sql
│   └── EnjoyTrip.mwb
├── docs/
│   ├── requirements.md               # 현재 DB 과제 요구사항
│   ├── verification.md               # 검증 범위와 한계
│   ├── assets/db-erd.png
│   └── screenshots/db/               # 현재 DB 버전 실제 실행 화면 14장
└── src/
    ├── main/
    │   ├── java/com/ssafy/live/
    │   │   ├── EnjoyTripApp.java      # main: Service 호출 후 조회 결과 확인
    │   │   ├── controller/            # Servlet·JSON 요청 처리
    │   │   ├── model/
    │   │   │   ├── service/
    │   │   │   │   ├── EnjoyTripService.java
    │   │   │   │   └── EnjoyTripServiceImpl.java
    │   │   │   ├── dao/
    │   │   │   │   ├── EnjoyTripDAO.java
    │   │   │   │   └── EnjoyTripDAOImpl.java
    │   │   │   └── dto/               # 테이블별 DTO 10개 등
    │   │   ├── util/                  # DBUtil·PasswordUtil·ImageStore 등
    │   │   ├── filter/
    │   │   └── listener/
    │   ├── resources/
    │   │   └── db.properties.example
    │   └── webapp/
    │       ├── index.jsp
    │       ├── js/                    # 메인·회원·커뮤니티
    │       ├── css/
    │       ├── 01_api/                # 기존 폴더명 유지, 관광 데이터는 DB 조회
    │       └── WEB-INF/               # web.xml·JSP·.env.example
    └── test/java/com/ssafy/live/
        └── DbIntegrationCheck.java    # 독립 MySQL 테스트 DB 전용 실행형 검사
```

Controller는 요청과 세션을 처리하고, `EnjoyTripServiceImpl`은 입력값·권한을 검증한 뒤 DAO를 호출합니다. DAO는 `DBUtil`의 연결과 `PreparedStatement`를 사용해 SQL을 실행합니다. 테이블별 DTO는 Java record로 구성했으며, 기존 회원 JSP에서 사용하는 `Member` 등도 유지했습니다.

## 6. 실행 방법

### 6.1 프로젝트 가져오기

1. 저장소를 clone하거나 소스를 준비합니다.
2. STS에서 **File → Import → Maven → Existing Maven Projects**를 선택하고 `pom.xml`이 있는 폴더를 지정합니다.
3. 프로젝트 이름 **BE_06**, **JDK 21**, **Tomcat 11** 실행 환경을 확인합니다.
4. 프로젝트 우클릭 → **Maven → Update Project…**를 실행합니다. 기존에 열려 있던 프로젝트라면 F5로 새 파일도 반영합니다.

### 6.2 DB 최초 생성과 초기 데이터 적재

MySQL Workbench에서 접속한 뒤 아래 순서로 실행합니다.

1. `db/EnjoyTrip_Schema.sql`
2. `db/EnjoyTrip_Dump.sql`

DB명은 `ssafy_trip`입니다. 두 파일은 **빈 DB의 최초 구축용**이며 기존 데이터가 있는 환경에서는 재실행하지 않습니다. 스키마의 같은 이름 테이블 생성 오류를 무시하고 덤프까지 이어서 실행하지 않도록 Workbench의 오류 시 중단 옵션을 사용합니다.

제공된 원본 `SSAFY_TRIP_Schema.sql`·`SSAFY_TRIP_Dump.sql`의 관광정보를 포함한 통합 파일이므로, 원본 SQL을 먼저 실행할 필요는 없습니다. 기존 브라우저 localStorage의 개인 계획·기록을 자동 이관하지는 않습니다.

### 6.3 DB 접속 설정

[`src/main/resources/db.properties.example`](src/main/resources/db.properties.example)을 같은 폴더에 `db.properties`로 복사하고 접속 계정에 맞게 작성합니다. 현재 PC에서 사용하는 MySQL 계정은 `ssafy`입니다.

```properties
db.url=jdbc:mysql://localhost:3306/ssafy_trip?characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
db.username=ssafy
db.password=YOUR_LOCAL_MYSQL_PASSWORD
```

MySQL 로그인 계정과 웹 서비스의 회원 계정은 별개입니다. `db.properties`는 Git 제외 대상이며, 이미 설정한 로컬 파일은 유지합니다. 예제 파일의 기본 사용자명은 `root`이므로 본인의 접속 계정으로 변경합니다.

환경 변수 `DB_URL`·`DB_USERNAME`·`DB_PASSWORD`도 지원합니다. Java 시스템 속성 → 환경 변수 → 로컬 설정 → 기본값 순으로 적용됩니다. 콘솔 실행은 `-Ddb.config=파일경로` 또는 `DB_CONFIG`로 별도 설정 파일을 지정할 수 있고, 웹 실행은 `WEB-INF/.env`의 DB 설정도 읽습니다. 설정을 변경한 뒤에는 Java Application 또는 Tomcat을 다시 시작합니다.

### 6.4 카카오 지도 설정

[`src/main/webapp/WEB-INF/.env.example`](src/main/webapp/WEB-INF/.env.example)을 같은 위치의 `.env`로 복사하고 **JavaScript 키**를 입력합니다.

```dotenv
KAKAO_MAP_KEY=YOUR_KAKAO_JAVASCRIPT_KEY
```

동일한 이름의 환경 변수로도 설정할 수 있습니다. 카카오 개발자 설정에는 실제 실행 주소인 `http://localhost:8080`을 허용 도메인으로 등록합니다. 지도와 주소 변환은 외부 SDK 연결이 필요하며, 관광 DB 조회에는 TourAPI 인증키가 필요하지 않습니다.

### 6.5 main과 웹 실행

**main으로 DB 연결 확인**

STS에서 `com.ssafy.live.EnjoyTripApp` → **Run As → Java Application**을 실행합니다. Service를 통해 테이블별 행 수, 지역 목록, 관광정보 일부와 커뮤니티 건수를 조회합니다. 이 main은 데이터 초기화를 수행하지 않습니다.

**웹 화면 실행**

1. MySQL 서버가 실행 중인지 확인합니다.
2. STS에서 **Project → Clean**을 수행합니다.
3. BE_06 → **Run As → Run on Server**에서 Tomcat 11을 선택합니다.
4. `http://localhost:8080/BE_06/`에 접속합니다.

Maven을 명령행에서 사용할 수 있다면 프로젝트 루트에서 다음과 같이 WAR를 만들 수 있습니다.

```shell
mvn package -DskipTests
```

생성 파일은 `target/BE_06.war`입니다. 패키징 후 Windows 콘솔에서 main을 확인하려면 다음과 같이 실행합니다.

```powershell
java -cp "target/classes;target/BE_06/WEB-INF/lib/*" com.ssafy.live.EnjoyTripApp
```

### 6.6 초기 웹 회원 계정

| 구분 | 아이디 | 비밀번호 | 비고 |
|---|---|---|---|
| 관리자 | `ssafy` | `1234` | 공지사항 관리 가능 |
| 일반 회원 | `user000` ~ `user099` | `1234` | 본인 계획·핫플·게시글 관리 |

초기 회원의 복구 질문은 `교육기관 이름은?`, 답변은 `ssafy`입니다. 원본 일반 게시글의 별도 글 비밀번호는 `1234`이지만 작성자가 표시용 계정이므로 임의 회원이 수정할 수 없습니다. 새 회원으로 가입해 본인이 작성한 글에서 수정·삭제를 확인할 수 있습니다.

## 7. 기능 사용 방법

| 기능 | 사용 순서 |
|---|---|
| 관광정보 | 콘텐츠 유형·시도·시군구 선택 → 관광지 조회 → 목록 또는 마커 선택 |
| 여행계획 | 로그인 → 마커의 계획 추가 버튼 → 저장 전 드래그 순서 변경 → 여행 정보 입력 → 저장 → 저장 목록에서 동선 조회·삭제 |
| Hotplace | 로그인 → 나의 핫플 → 사진·방문일·유형·장소·주소·후기 입력 → 저장 → 저장된 핫플에서 지도 조회·삭제 |
| 공지사항 | 공개 목록·상세 조회, 관리자 로그인 시 글쓰기·수정·삭제 |
| 공유게시판 | 로그인 → 글쓰기와 글 비밀번호 지정 → 본인 글 수정·삭제 시 비밀번호 확인, 상세에서 댓글 등록·본인 댓글 삭제 |
| 비밀번호 찾기 | 아이디로 복구 질문 확인 → 답변 검증 → 새 비밀번호 설정 → 재로그인 |

Hotplace 사진은 PNG/JPEG, 5MB 이하, 최대 1,600만 화소를 허용하며 서버에서 실제 이미지 내용을 확인해 다시 저장합니다. 기본 저장 위치는 `%USERPROFILE%/.enjoytrip/uploads/{회원번호}`이며 환경 변수 `UPLOAD_DIR` 또는 `-Dupload.dir`로 변경할 수 있습니다. DB에는 파일명만 저장하므로 재배포·백업 시 DB와 사진 폴더를 함께 보존합니다.

## 8. 주요 요청 경로

모든 경로는 `/BE_06` 아래에 있습니다. 아래 쉼표로 나열한 action은 **하나씩** 지정합니다.

| 경로 | 역할 |
|---|---|
| `/api/tour` | `regions`, `categories`, `search`, `nearby`, `images`로 관광 DB 조회; `config`로 지도 공개 설정 조회 |
| `/api/member` | `register`, `login`, `logout`, `current`, `profile`, `delete`, 복구 질문·답변·비밀번호 재설정 |
| `/api/enjoytrip?action=community` | 공개 공지·게시글·댓글 조회 |
| `/api/enjoytrip?action=plans` | 본인의 여행계획과 장소 조회 |
| `/api/enjoytrip?action=records` | 본인의 Hotplace 조회 |
| `/api/enjoytrip`의 POST | `save-post`, `delete-post`, `add-comment`, `delete-comment`, `save-plan`, `delete-plan`, `save-record`, `delete-record` |
| `/api/record-images` | POST multipart 본인 사진 업로드, GET 로그인 소유자의 사진 응답 |

관광 조회 예: `/api/tour?action=search&areaCode=1&sigunguCode=1&contentTypeId=12&pageNo=1&numOfRows=20`

변경 요청은 POST와 `X-Requested-With: XMLHttpRequest` 헤더를 사용하며, 권한이 필요한 요청은 서버 세션과 작성자·소유자·관리자 여부를 확인합니다. 별도 CSRF 토큰 방식은 구현하지 않았습니다.

## 9. 검증 범위

| 항목 | 확인 결과 |
|---|---|
| Maven | Java 21 기준 WAR 패키징 성공 |
| main | `EnjoyTripApp`의 Service 호출·실제 DB 조회 확인 |
| Java DB 통합 검사 | 별도 MySQL 테스트 DB에서 **40개 검사 통과** |
| HTTP·JSP | 별도 MySQL·Tomcat 환경에서 **33개 HTTP 검사**와 JSP 렌더링 확인 |
| 실제 브라우저 | 지역·유형 조회, 실제 지도 타일·마커·사진, 회원 가입·수정·복구·재로그인, 계획 저장·재조회, 사진 업로드·핫플 저장, 게시글·댓글·공지 변경 확인 |
| 화면 | 실제 실행 캡처 14장, 390px 모바일 화면의 가로 넘침 없음 확인 |
| 시연 데이터 정리 | 임시 계정·게시물·사진·계획·핫플 정리 후 10개 테이블 행 수가 실행 전과 같음을 확인 |

[`DbIntegrationCheck.java`](src/test/java/com/ssafy/live/DbIntegrationCheck.java)는 **독립된 `ssafy_trip_test` DB 전용 main 검사**입니다. `ENJOYTRIP_TEST_DB_URL`과 필요한 테스트 계정 환경 변수를 설정하고 실행하며, `mvn test`만으로 자동 실행되는 JUnit 테스트는 아닙니다. 자세한 확인 내용과 한계는 [검증 문서](docs/verification.md)에 정리했습니다.

## 10. 남은 보완 사항과 자료 안내

- [ ] 여행계획의 저장 후 수정과 날짜별 일정 편집
- [ ] Hotplace의 공개 공유 기능
- [ ] 제공 관광 데이터의 콘텐츠 유형명 정제와 최신 데이터 갱신
- [ ] 관광지 상세의 전화번호·홈페이지·개요를 보여주는 별도 화면
- [ ] 뉴스·날씨·추천 사진·일출·일몰·전기차 충전소 연동
- [ ] 별도 CSRF 토큰, 요청 횟수 제한 등 인증·변경 요청 보호 보완
- [ ] 동시 사용자 부하·다양한 실제 모바일 기기·브라우저 검증

현재 제출 자료는 이 README, [DB 과제 요구사항](docs/requirements.md), [검증 기록](docs/verification.md), `db/`의 SQL 2개와 Workbench 모델 1개, `docs/screenshots/db/`의 실제 실행 화면입니다. `docs/`에 남아 있는 이전 단계 다이어그램·화면은 과거 자료이며 현재 구현 설명은 이 README와 위 두 문서를 기준으로 합니다.

로컬 DB 비밀번호와 지도 키는 `db.properties`·`.env`로 분리하고 Git에서 제외합니다.
