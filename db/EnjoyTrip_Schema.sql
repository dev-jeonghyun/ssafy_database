-- EnjoyTrip 전체 스키마: 관광정보 4개 + 서비스 6개 = 총 10개 테이블
-- MySQL 8.0.16 이상 / 8.4, 비어 있는 ssafy_trip의 최초 생성용
-- 실행 순서: EnjoyTrip_Schema.sql -> EnjoyTrip_Dump.sql
-- 같은 이름의 기존 테이블이 있으면 중단하세요. 기존 테이블 자동 변경/삭제는 하지 않습니다.
-- Workbench의 '오류 발생 시 스크립트 중단' 옵션을 켜고 실행하세요.
-- 기존 SSAFY_TRIP_Schema.sql 대신 이 파일을 사용합니다.
-- 원본 관광 테이블의 컬럼 순서와 타입은 유지합니다.
-- is_login_enabled=FALSE인 회원 행은 원본 게시판의 표시용 작성자이며 로그인 계정이 아닙니다.
-- Java 로그인 구현 시 활성 여부 확인과 PBKDF2 비밀번호 검증을 적용해야 합니다.

SET NAMES utf8mb4;
-- MySQL Workbench Forward Engineering



-- -----------------------------------------------------
-- Schema ssafy_trip
-- -----------------------------------------------------
CREATE SCHEMA IF NOT EXISTS `ssafy_trip` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci ;
USE `ssafy_trip` ;

-- -----------------------------------------------------
-- Table `ssafy_trip`.`sidos`
-- -----------------------------------------------------


CREATE TABLE `ssafy_trip`.`sidos` (
  `no` int NOT NULL AUTO_INCREMENT  comment '시도번호',
  `sido_code` int NOT NULL comment '시도코드',
  `sido_name` varchar(20) DEFAULT NULL comment '시도이름',
  PRIMARY KEY (`no`),
  UNIQUE INDEX `sido_code_UNIQUE` (`sido_code` ASC) VISIBLE)
ENGINE = InnoDB
AUTO_INCREMENT = 18
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci comment '시도정보테이블';


-- -----------------------------------------------------
-- Table `ssafy_trip`.`guguns`
-- -----------------------------------------------------


CREATE TABLE `ssafy_trip`.`guguns` (
  `no` int NOT NULL AUTO_INCREMENT comment '구군번호',
  `sido_code` int NOT NULL comment '시도코드',
  `gugun_code` int NOT NULL comment '구군코드',
  `gugun_name` varchar(20) DEFAULT NULL comment '구군이름',
  PRIMARY KEY (`no`),
  INDEX `guguns_sido_to_sidos_cdoe_fk_idx` (`sido_code` ASC) VISIBLE,
  INDEX `gugun_code_idx` (`gugun_code` ASC) VISIBLE,
  UNIQUE KEY `uq_guguns_region` (`sido_code`, `gugun_code`),
  CONSTRAINT `guguns_sido_to_sidos_cdoe_fk`
    FOREIGN KEY (`sido_code`)
    REFERENCES `ssafy_trip`.`sidos` (`sido_code`))
ENGINE = InnoDB
AUTO_INCREMENT = 235
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
comment '구군정보테이블';


-- -----------------------------------------------------
-- Table `ssafy_trip`.`contenttypes`
-- -----------------------------------------------------


CREATE TABLE `ssafy_trip`.`contenttypes` (
  `content_type_id` int NOT NULL comment '콘텐츠타입번호',
  `content_type_name` varchar(45) DEFAULT NULL comment '콘텐츠타입이름',
  PRIMARY KEY (`content_type_id`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci comment '콘텐츠타입정보테이블';


-- -----------------------------------------------------
-- Table `ssafy_trip`.`attractions`
-- -----------------------------------------------------


CREATE TABLE `ssafy_trip`.`attractions` (
  `no` int NOT NULL AUTO_INCREMENT  comment '명소코드',
  `content_id` int DEFAULT NULL comment '콘텐츠번호',
  `title` varchar(500) DEFAULT NULL comment '명소이름',
  `content_type_id` int DEFAULT NULL comment '콘텐츠타입',
  `area_code` int DEFAULT NULL comment '시도코드',
  `si_gun_gu_code` int DEFAULT NULL comment '구군코드',
  `first_image1` varchar(100) DEFAULT NULL comment '이미지경로1',
  `first_image2` varchar(100) DEFAULT NULL comment '이미지경로2',
  `map_level` int DEFAULT NULL comment '줌레벨',
  `latitude` decimal(20,17) DEFAULT NULL comment '위도',
  `longitude` decimal(20,17) DEFAULT NULL comment '경도',
  `tel` varchar(20) DEFAULT NULL comment '전화번호',
  `addr1` varchar(100) DEFAULT NULL comment '주소1',
  `addr2` varchar(100) DEFAULT NULL comment '주소2',
  `homepage` varchar(1000) DEFAULT NULL comment '홈페이지',
  `overview` varchar(10000) DEFAULT NULL comment '설명',
  PRIMARY KEY (`no`),
  KEY `idx_attractions_content_id` (`content_id`),
  KEY `idx_attractions_region_type` (`area_code`, `si_gun_gu_code`, `content_type_id`),
  INDEX `attractions_typeid_to_types_typeid_fk_idx` (`content_type_id` ASC) VISIBLE,
  INDEX `attractions_sido_to_sidos_code_fk_idx` (`area_code` ASC) VISIBLE,
  INDEX `attractions_sigungu_to_guguns_gugun_fk_idx` (`si_gun_gu_code` ASC) VISIBLE,
  CONSTRAINT `attractions_area_to_sidos_code_fk`
    FOREIGN KEY (`area_code`)
    REFERENCES `ssafy_trip`.`sidos` (`sido_code`),
  CONSTRAINT `attractions_sigungu_to_guguns_gugun_fk`
    FOREIGN KEY (`area_code`, `si_gun_gu_code`)
    REFERENCES `ssafy_trip`.`guguns` (`sido_code`, `gugun_code`),
  CONSTRAINT `attractions_typeid_to_types_typeid_fk`
    FOREIGN KEY (`content_type_id`)
    REFERENCES `ssafy_trip`.`contenttypes` (`content_type_id`))
ENGINE = InnoDB
AUTO_INCREMENT = 56644
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_0900_ai_ci
comment '명소정보테이블';



-- ============================================================
-- 추가 서비스 테이블 6개
-- 회원 번호는 BIGINT, 관광지 번호는 INT(attractions.no)를 사용합니다.
-- ============================================================

CREATE TABLE `members` (
  `member_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '회원 번호',
  `login_id` VARCHAR(50) NOT NULL COMMENT '로그인 아이디: 대소문자 구분 없이 유일',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '애플리케이션에서 생성한 비밀번호 해시',
  `name` VARCHAR(100) NOT NULL COMMENT '회원 이름',
  `is_login_enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '표시용 예시 작성자는 FALSE',
  `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT 'USER 또는 ADMIN',
  `recovery_question` VARCHAR(255) DEFAULT NULL COMMENT '복구 질문',
  `recovery_answer_hash` VARCHAR(255) DEFAULT NULL COMMENT '복구 답변 해시',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '가입 일시',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`member_id`),
  UNIQUE KEY `uq_members_login_id` (`login_id`),
  CONSTRAINT `chk_members_role` CHECK (`role` IN ('USER', 'ADMIN')),
  CONSTRAINT `chk_members_recovery_pair` CHECK (
    (`recovery_question` IS NULL AND `recovery_answer_hash` IS NULL)
    OR (`recovery_question` IS NOT NULL AND `recovery_answer_hash` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='회원: 기존 member.dat를 대체할 저장 구조';

CREATE TABLE `trip_plans` (
  `plan_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '여행 계획 번호',
  `member_id` BIGINT NOT NULL COMMENT '소유 회원 번호',
  `title` VARCHAR(200) NOT NULL COMMENT '계획 제목',
  `start_date` DATE NOT NULL COMMENT '여행 시작일',
  `end_date` DATE NOT NULL COMMENT '여행 종료일',
  `budget` DECIMAL(14,2) NOT NULL DEFAULT 0 COMMENT '여행 총예산',
  `memo` TEXT DEFAULT NULL COMMENT '계획 메모',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`plan_id`),
  KEY `idx_trip_plans_member_created` (`member_id`, `created_at`),
  CONSTRAINT `fk_trip_plans_member`
    FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_trip_plans_dates` CHECK (`end_date` >= `start_date`),
  CONSTRAINT `chk_trip_plans_budget` CHECK (`budget` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 계획 기본 정보';

CREATE TABLE `trip_plan_places` (
  `plan_place_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '계획 내 방문 항목 번호',
  `plan_id` BIGINT NOT NULL COMMENT '여행 계획 번호',
  `attraction_no` INT NOT NULL COMMENT '관광지 내부 번호: attractions.no 참조',
  `visit_order` INT NOT NULL COMMENT '계획 전체 방문 순서: 1부터 시작',
  `memo` TEXT DEFAULT NULL COMMENT '방문 장소 메모',
  PRIMARY KEY (`plan_place_id`),
  UNIQUE KEY `uq_trip_plan_places_order` (`plan_id`, `visit_order`),
  KEY `idx_trip_plan_places_attraction` (`attraction_no`),
  CONSTRAINT `fk_trip_plan_places_plan`
    FOREIGN KEY (`plan_id`) REFERENCES `trip_plans` (`plan_id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_trip_plan_places_attraction`
    FOREIGN KEY (`attraction_no`) REFERENCES `attractions` (`no`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_trip_plan_places_order` CHECK (`visit_order` >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='계획별 방문 관광지: 다른 순서의 동일 관광지 재방문 허용';

CREATE TABLE `posts` (
  `post_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '게시글 번호',
  `member_id` BIGINT NOT NULL COMMENT '작성 회원 번호',
  `post_type` VARCHAR(20) NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL: 일반글, NOTICE: 공지',
  `title` VARCHAR(200) NOT NULL COMMENT '제목',
  `post_password_hash` VARCHAR(255) DEFAULT NULL COMMENT '기존 일반 게시글 수정/삭제 비밀번호 해시',
  `content` TEXT NOT NULL COMMENT '본문',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`post_id`),
  KEY `idx_posts_type_created` (`post_type`, `created_at`, `post_id`),
  KEY `idx_posts_member` (`member_id`),
  CONSTRAINT `fk_posts_member`
    FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_posts_type` CHECK (`post_type` IN ('GENERAL', 'NOTICE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='공지사항과 일반 게시글: 공지 작성 권한은 서비스에서 검사';

CREATE TABLE `comments` (
  `comment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '댓글 번호',
  `post_id` BIGINT NOT NULL COMMENT '대상 게시글 번호',
  `member_id` BIGINT NOT NULL COMMENT '작성 회원 번호',
  `content` TEXT NOT NULL COMMENT '댓글 내용',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`comment_id`),
  KEY `idx_comments_post_created` (`post_id`, `created_at`, `comment_id`),
  KEY `idx_comments_member` (`member_id`),
  CONSTRAINT `fk_comments_post`
    FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_comments_member`
    FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='게시글 댓글';

CREATE TABLE `travel_records` (
  `record_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '여행 기록 번호',
  `member_id` BIGINT NOT NULL COMMENT '작성 회원 번호',
  `visit_date` DATE NOT NULL COMMENT '방문일',
  `record_type` VARCHAR(50) NOT NULL COMMENT '기록 유형: 현재 화면의 유형 값',
  `place_name` VARCHAR(500) NOT NULL COMMENT '기록 당시 장소명: 직접 입력 장소도 허용',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '기록 당시 주소',
  `latitude` DECIMAL(20,17) DEFAULT NULL COMMENT '위도: mapy',
  `longitude` DECIMAL(20,17) DEFAULT NULL COMMENT '경도: mapx',
  `content` TEXT DEFAULT NULL COMMENT '방문 후기 및 설명',
  `image_path` VARCHAR(2048) DEFAULT NULL COMMENT '사진 파일 경로 또는 URL',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`record_id`),
  KEY `idx_travel_records_member_date` (`member_id`, `visit_date`),
  CONSTRAINT `fk_travel_records_member`
    FOREIGN KEY (`member_id`) REFERENCES `members` (`member_id`)
    ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `chk_travel_records_latitude` CHECK (`latitude` BETWEEN -90 AND 90),
  CONSTRAINT `chk_travel_records_longitude` CHECK (`longitude` BETWEEN -180 AND 180),
  CONSTRAINT `chk_travel_records_coordinates` CHECK (
    (`latitude` IS NULL AND `longitude` IS NULL)
    OR (`latitude` IS NOT NULL AND `longitude` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='여행 기록 및 핫플: 관광지 목록에 없는 장소도 기록 가능';

