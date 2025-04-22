INSERT INTO activity_core (filename, start_time, end_time, total_distance, total_calories, total_time, total_ascent)
VALUES
('2025_ride_01.fit', '2025-01-10 07:00:00', '2025-01-10 09:30:00', 48.75, 1220, 9000, 600),
('2025_ride_02.fit', '2025-02-15 08:10:00', '2025-02-15 10:20:00', 38.50, 980, 7800, 420);

INSERT INTO activity_point (activity_core_id, latitude, longitude, altitude, timestamp, distance)
VALUES
(1, 37.5665, 126.9780, 35.0, '2025-01-10 07:05:00', 0.5),
(1, 37.5670, 126.9800, 36.5, '2025-01-10 07:10:00', 1.3),
(2, 35.1796, 129.0756, 15.0, '2025-02-15 08:15:00', 0.8);

INSERT INTO activity_weather (activity_core_id, temperature, humidity, wind_speed, condition)
VALUES
(1, 11.5, 60.0, 3.5, '맑음'),
(2, 7.2, 70.0, 2.0, '흐림');

INSERT INTO activity (activity_date, activity_name, content, visibility, device, activity_core_id)
VALUES
('2025-01-10', '한강 라이딩', '한강 따라 시원하게 달렸어요!', 'public', 'Garmin', 1),
('2025-02-15', '부산 해안 라이딩', '광안리 경치가 예술이었어요!', 'public', 'Garmin', 2);

INSERT INTO activity_image (image_url, activity_id)
VALUES
('/images/2025_ride_01_1.jpg', 1),
('/images/2025_ride_02_1.jpg', 2);

INSERT INTO activity_comment (author, content, created_at, activity_id)
VALUES
('riderA', '우와 풍경 너무 예뻐요!', '2025-01-10 10:00:00', 1),
('userB', '저도 다음에 같이 가요~', '2025-02-15 11:00:00', 2);

INSERT INTO file_date (filename, file_date)
VALUES
('2025_ride_01.fit', '2025-01-10'),
('2025_ride_02.fit', '2025-02-15');
