-- =====================================================================
-- EnjoyTrip Standard Maven Project Schema
-- =====================================================================
SET NAMES utf8mb4;
DROP DATABASE IF EXISTS enjoytrip;
CREATE DATABASE enjoytrip DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE enjoytrip;

-- =====================================================================
-- 1. 지역 코드 테이블 (시도, 구군) - 지역별 관광지 검색용
-- =====================================================================
CREATE TABLE sido (
  sido_code  VARCHAR(2)  NOT NULL COMMENT '시도 코드',
  sido_name  VARCHAR(20) NOT NULL COMMENT '시도 이름 (서울, 부산 등)',
  PRIMARY KEY (sido_code)
) COMMENT '시도';

CREATE TABLE gugun (
  sido_code  VARCHAR(2)  NOT NULL,
  gugun_code VARCHAR(3)  NOT NULL COMMENT '구군 코드',
  gugun_name VARCHAR(30) NOT NULL,
  PRIMARY KEY (sido_code, gugun_code),
  CONSTRAINT fk_gugun_sido FOREIGN KEY (sido_code) REFERENCES sido (sido_code)
) COMMENT '구군';

-- =====================================================================
-- 2. 회원 테이블 (Member DTO)
-- =====================================================================
CREATE TABLE members (
  member_id       VARCHAR(20)  NOT NULL COMMENT '회원 아이디 (PK)',
  member_name     VARCHAR(30)  NOT NULL COMMENT '이름',
  member_password VARCHAR(100) NOT NULL COMMENT '비밀번호',
  email_id        VARCHAR(50)  NOT NULL COMMENT '이메일 아이디',
  email_domain    VARCHAR(30)  NOT NULL COMMENT '이메일 도메인',
  role            VARCHAR(10)  NOT NULL DEFAULT 'user' COMMENT '권한 (user / admin)',
  join_date       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (member_id)
) COMMENT '회원';

-- =====================================================================
-- 3. 관광지 콘텐츠 유형 (ContentType DTO)
-- =====================================================================
CREATE TABLE content_type (
  content_type_id   INT         NOT NULL COMMENT '12:관광지, 14:문화시설, 15:축제 등',
  content_type_name VARCHAR(20) NOT NULL,
  PRIMARY KEY (content_type_id)
) COMMENT '관광지 콘텐츠 유형';

-- =====================================================================
-- 4. 관광지 테이블 (Attraction DTO)
-- =====================================================================
CREATE TABLE attraction (
  content_id      INT            NOT NULL COMMENT '관광지 번호 (PK)',
  content_type_id INT            NOT NULL,
  title           VARCHAR(200)   NOT NULL COMMENT '관광지명',
  addr            VARCHAR(300)   NOT NULL DEFAULT '' COMMENT '주소',
  first_image     VARCHAR(500)   NOT NULL DEFAULT '' COMMENT '대표 이미지 URL',
  latitude        DECIMAL(13,10) NOT NULL COMMENT '위도',
  longitude       DECIMAL(13,10) NOT NULL COMMENT '경도',
  sido_code       VARCHAR(2)     NOT NULL,
  gugun_code      VARCHAR(3)     NOT NULL,
  PRIMARY KEY (content_id),
  KEY idx_attraction_region (sido_code, gugun_code, content_type_id),
  CONSTRAINT fk_attr_type  FOREIGN KEY (content_type_id) REFERENCES content_type (content_type_id),
  CONSTRAINT fk_attr_sido  FOREIGN KEY (sido_code) REFERENCES sido (sido_code),
  CONSTRAINT fk_attr_gugun FOREIGN KEY (sido_code, gugun_code) REFERENCES gugun (sido_code, gugun_code)
) COMMENT '관광지';

-- =====================================================================
-- 5. 커뮤니티 게시판 (Board DTO) - 공지사항 및 여행 정보 공유
-- =====================================================================
CREATE TABLE board (
  article_no    INT          NOT NULL AUTO_INCREMENT COMMENT '글 번호 (PK)',
  board_type    VARCHAR(20)  NOT NULL DEFAULT 'board' COMMENT 'board: 여행정보, notice: 공지',
  member_id     VARCHAR(20)  NOT NULL COMMENT '작성자 아이디',
  subject       VARCHAR(100) NOT NULL COMMENT '제목',
  content       TEXT         NOT NULL COMMENT '내용',
  hit           INT          NOT NULL DEFAULT 0 COMMENT '조회수',
  register_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (article_no),
  CONSTRAINT fk_board_member FOREIGN KEY (member_id) REFERENCES members (member_id) ON DELETE CASCADE
) COMMENT '게시글';

-- =====================================================================
-- 6. 나만의 여행계획 (TripPlan & TripPlanPlace DTO) - 핵심 기능
-- =====================================================================
CREATE TABLE trip_plan (
  plan_id    INT         NOT NULL AUTO_INCREMENT COMMENT '계획 번호 (PK)',
  member_id  VARCHAR(20) NOT NULL COMMENT '작성자 아이디',
  title      VARCHAR(50) NOT NULL COMMENT '여행 제목',
  start_date DATE        NOT NULL COMMENT '여행 시작일',
  end_date   DATE        NOT NULL COMMENT '여행 종료일',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (plan_id),
  CONSTRAINT fk_plan_member FOREIGN KEY (member_id) REFERENCES members (member_id) ON DELETE CASCADE
) COMMENT '여행계획';

CREATE TABLE trip_plan_place (
  plan_id    INT         NOT NULL COMMENT '계획 번호 (FK)',
  seq        INT         NOT NULL COMMENT '방문 순서 (1, 2, 3...)',
  content_id INT         NOT NULL COMMENT '관광지 번호 (FK)',
  day        INT         NOT NULL DEFAULT 1 COMMENT '몇일차 방문인지',
  PRIMARY KEY (plan_id, seq),
  CONSTRAINT fk_place_plan       FOREIGN KEY (plan_id) REFERENCES trip_plan (plan_id) ON DELETE CASCADE,
  CONSTRAINT fk_place_attraction FOREIGN KEY (content_id) REFERENCES attraction (content_id)
) COMMENT '여행계획 상세 동선';