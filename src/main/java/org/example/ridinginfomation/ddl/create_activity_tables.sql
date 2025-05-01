-- 기존 테이블 삭제
DROP TABLE IF EXISTS activity_comment;
DROP TABLE IF EXISTS activity_image;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS activity_weather;
DROP TABLE IF EXISTS activity_point;
DROP TABLE IF EXISTS activity_core;
DROP TABLE IF EXISTS file_date;

-- 1. activity_core 테이블 수정: polyline 컬럼 추가
CREATE TABLE activity_core
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename       VARCHAR(255) NOT NULL UNIQUE,
    start_time     DATETIME,
    end_time       DATETIME,
    total_distance DOUBLE,
    total_calories INT,
    total_time     INT,
    total_ascent   INT,
    polyline       TEXT -- 폴리라인 컬럼 추가
);

-- 2. activity_point 테이블 (변경 없음)
CREATE TABLE activity_point
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_core_id BIGINT,
    latitude DOUBLE,
    longitude DOUBLE,
    altitude DOUBLE,
    timestamp        DATETIME,
    distance DOUBLE,
    FOREIGN KEY (activity_core_id) REFERENCES activity_core (id) ON DELETE CASCADE
);

-- 3. activity_weather 테이블 (변경 없음)
CREATE TABLE activity_weather
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_core_id BIGINT,
    temperature DOUBLE,
    humidity DOUBLE,
    wind_speed DOUBLE,
    condition        VARCHAR(100),
    FOREIGN KEY (activity_core_id) REFERENCES activity_core (id) ON DELETE CASCADE
);

-- 4. activity 테이블 (변경 없음)
CREATE TABLE activity
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_date    DATE,
    activity_name    VARCHAR(255),
    content          TEXT,
    visibility       VARCHAR(50),
    device           VARCHAR(100),
    activity_core_id BIGINT,
    FOREIGN KEY (activity_core_id) REFERENCES activity_core (id) ON DELETE CASCADE
);

-- 5. activity_image 테이블 (변경 없음)
CREATE TABLE activity_image
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url   VARCHAR(500),
    activity_id BIGINT,
    FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
);

-- 6. activity_comment 테이블 (변경 없음)
CREATE TABLE activity_comment
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    author      VARCHAR(100),
    content     TEXT,
    created_at  DATETIME,
    activity_id BIGINT,
    FOREIGN KEY (activity_id) REFERENCES activity (id) ON DELETE CASCADE
);

-- 7. file_date 테이블 (변경 없음)
CREATE TABLE file_date
(
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename  VARCHAR(255),
    file_date DATE
);
