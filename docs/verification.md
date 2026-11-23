# DB 버전 실행·검증 기록

확인일: **2026-11-06**. 대상: BE_06의 MySQL·JDBC 전환 버전.
검증 기준은 「자바전공_enjoytrip_16_06_DB.pdf」와 [현재 요구사항](requirements.md)입니다.

## 1. 결과와 범위

| 검사 | 결과 | 범위 |
|---|---|---|
| Maven WAR 빌드 | 통과 | Java 21, `target/BE_06.war` 패키징 |
| EnjoyTripApp | 통과 | Service를 통한 10개 테이블 건수·시도·관광지·공지/일반 글 조회 |
| Java DB 통합 검사 | **40개 통과** | 독립 MySQL 테스트 환경, `DbIntegrationCheck` 실행 |
| 서버 요청 | **33개 HTTP 검사 통과** | 독립 MySQL·Tomcat, 회원·관광·커뮤니티·계획·사진 요청 |
| JSP 렌더링 | 통과 | 독립 Tomcat의 실제 JSP 응답 확인 |
| 실제 브라우저 | 통과 | 사용 중인 `http://localhost:8080/BE_06/`, 실제 MySQL·카카오 SDK |
| 실행 화면 | **14장** | 데스크톱 1440px, 모바일 에뮬레이션 390px |
| 시연 데이터 정리 | 통과 | 10개 테이블의 시연 전후 행 수 동일 |

40개 Java 검사와 33개 HTTP 검사는 서로 다른 범위입니다. Maven 기본 테스트가 73개 실행된다는 의미가 아닙니다. HTTP 검사는 검증 당시 별도 보조 실행기로 수행했으며, 저장소의 `mvn test`에 등록된 테스트가 아닙니다.

## 2. Java 통합 검사에서 확인한 항목

실행 소스: [`DbIntegrationCheck.java`](../src/test/java/com/ssafy/live/DbIntegrationCheck.java).

- 10개 테이블과 제공 관광 데이터, 지역·유형 목록
- 관광 페이지 분리, 복합 지역 필터, 파라미터 바인딩과 주변 거리 조회
- SQL 더미의 PBKDF2 호환 로그인, 회원 번호 0 보존, 표시용 작성자 로그인 차단
- 회원 등록·수정·비밀번호·복구 답변 검증
- 일반 글·공지·댓글의 저장과 서버 권한 검사
- 여행계획·방문 순서 저장, 다른 회원의 조회·삭제 제한, 저장 실패 시 롤백
- 이미지 내용 확인·저장 및 본인 Hotplace 관리

### 재실행 조건

검사는 일반 단위 테스트가 아닌 main 프로그램입니다. 제공 데이터와 동일한 구조·초기 더미가 있는 **독립된 `ssafy_trip_test` DB**를 준비해야 합니다. SQL 파일은 `ssafy_trip`을 명시하므로, 테스트용으로는 DB명을 변경한 복사본을 사용하고 사용 중인 DB에 초기화 SQL을 실행하지 않습니다.

프로젝트 루트의 Windows PowerShell 예시:

```powershell
mvn package -DskipTests
$env:ENJOYTRIP_TEST_DB_URL='jdbc:mysql://localhost:3306/ssafy_trip_test?characterEncoding=UTF-8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false'
$env:ENJOYTRIP_TEST_DB_USERNAME='YOUR_TEST_DB_USER'
$env:ENJOYTRIP_TEST_DB_PASSWORD='YOUR_TEST_DB_PASSWORD'
java -cp "target/test-classes;target/classes;target/BE_06/WEB-INF/lib/*" com.ssafy.live.DbIntegrationCheck
```

테스트 URL이 없거나 `/ssafy_trip_test`를 포함하지 않으면 프로그램이 시작을 거부합니다. 검사는 테스트 DB에 데이터를 쓰므로 운영·개인 실사용 DB를 지정하지 않습니다.

## 3. 실제 브라우저 사용 흐름

headless Edge에서 실제 페이지를 열고 폼·버튼·지도 SDK를 사용했습니다. DB·지도·서버 응답을 테스트용 가짜 데이터나 지도 이미지로 바꾸지 않았습니다.

| 순서 | 사용 흐름 | 확인 사항 |
|---|---|---|
| 1 | 메인·관광 조회 | 시도 17개와 전체 선택, 콘텐츠 유형 8개와 전체 선택, 서울 구군 25개와 전체 선택 |
| 2 | 서울 강남구 조회 | 관광지·문화시설 조회, 실제 지도 타일·마커·DB 대표 사진 표시 |
| 3 | 회원가입 | 아이디 사용 가능 여부 확인 후 신규 가입 |
| 4 | 로그인·내 정보 | 실제 세션 로그인, 본인 이름 수정 |
| 5 | 비밀번호 찾기 | 복구 질문·답변 확인, 새 비밀번호 설정 후 재로그인 |
| 6 | 여행계획 | 실제 마커의 추가 버튼으로 두 장소 담기, 드래그 순서 변경, 기간·예산·메모 저장 |
| 7 | 계획 재조회 | 페이지 새로고침 후 로그인·저장 목록 유지, 저장한 계획의 직선 동선 표시 |
| 8 | Hotplace | 실제 파일 업로드, 주소 좌표 변환, 본인 기록 저장·사진 조회·지도 위치 표시 |
| 9 | 공유게시판 | 글 등록·댓글 등록·글 수정·목록/상세 조회 |
| 10 | 관리자 공지 | 관리자 로그인 후 공지 등록·수정·목록 조회 |
| 11 | 모바일 | 390px 화면 배치와 가로 넘침 없음 |
| 12 | 정리 | 시연 글·공지·계획·핫플·사진 삭제, 시연 계정 탈퇴 후 임시 계정 행 정리 |

사진 업로드에는 제공 관광 DB에 저장된 [관광공사 대표 이미지](http://tong.visitkorea.or.kr/cms/resource/64/3432064_image2_1.jpg)를 사용했습니다. 시연용 후기와 공지는 검증 과정에서 작성한 것으로, 실제 여행 후기나 운영 공지가 아닙니다.

## 4. 캡처와 시연 데이터 정리

현재 실행 화면은 [`screenshots/db/`](screenshots/db/)에 저장했습니다.

| 번호 | 파일 | 화면 |
|---|---|---|
| 01 | `01-main.png` | 메인 |
| 02 | `02-tour-search.png` | 지역별 관광정보 |
| 03 | `03-tour-culture.png` | 문화시설 유형 조회 |
| 04 | `04-tour-photo.png` | 마커·대표 사진 |
| 05 | `05-signup.png` | 가입·중복 확인 |
| 06 | `06-login.png` | 로그인 |
| 07 | `07-profile.png` | 본인 정보 수정·탈퇴 |
| 08 | `08-recovery.png` | 질문·답변 방식 비밀번호 재설정 |
| 09 | `09-trip-plan.png` | DB 저장 계획·직선 동선 |
| 10 | `10-hotplace.png` | 본인 사진·핫플 목록·지도 |
| 11 | `11-notices.png` | 관리자 공지 |
| 12 | `12-board.png` | 게시판 목록 |
| 13 | `13-post-detail.png` | 게시글 상세·댓글 |
| 14 | `14-mobile.png` | 390px 모바일 |

캡처용 계정만 새로 생성해 사용했습니다. 시연 자료 정리 후 데이터는 다음과 같이 유지되었습니다.

| 테이블 | 실행 전 | 정리 후 |
|---|---:|---:|
| sidos | 17 | 17 |
| guguns | 234 | 234 |
| contenttypes | 8 | 8 |
| attractions | 50,915 | 50,915 |
| members | 125 | 125 |
| trip_plans | 0 | 0 |
| trip_plan_places | 0 | 0 |
| posts | 12 | 12 |
| comments | 12 | 12 |
| travel_records | 0 | 0 |

따라서 최초 실행 시 시연 화면의 여행계획·핫플·추가 공지·추가 게시글이 기본 데이터로 나타나지는 않습니다. 기존 회원과 원본 게시글을 시연 데이터로 덮어쓰지 않았습니다.

## 5. 검증의 한계

- 실제 웹 실행은 로컬 Tomcat 11·MySQL 8.0.46 기준입니다. 다른 PC의 설정·허용 도메인은 별도 확인해야 합니다.
- 모바일은 브라우저 에뮬레이션이며 다양한 실제 기기·브라우저 검증은 수행하지 않았습니다.
- 동시 부하, 상시 운영 가용성, 모든 보안 조건에 대한 전수 검증은 수행하지 않았습니다.
- 지도 선은 도로 길찾기 결과가 아니며, 날짜별 일정 편집과 저장된 계획 수정은 없습니다.
- 뉴스·날씨·사진 추천·일출/일몰·충전소와 이메일 인증은 구현·검증 범위에 포함하지 않았습니다.
- 관광정보 최신성·원본 유형명 정제, 별도 CSRF 토큰·요청 횟수 제한은 후속 보완 항목입니다.

이 문서는 이전 2026-10-30의 TourAPI·파일·localStorage 기반 검증 내용을 현재 DB 버전 기준으로 갱신한 것입니다.
