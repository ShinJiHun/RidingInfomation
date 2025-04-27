// ✅ FitReader.java
package org.example.ridinginfomation.Garmin.Util;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SessionMesgListener;
import jakarta.annotation.PostConstruct;
import org.example.ridinginfomation.Garmin.VO.ActivityCoreVO;
import org.example.ridinginfomation.Garmin.VO.ActivityPointVO;
import org.example.ridinginfomation.Garmin.VO.RideVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class FitReader {

    private static final Logger logger = LoggerFactory.getLogger(FitReader.class);
    private final List<RideVO> cache = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("🚴 FIT 파일 로딩 시작...");
        long start = System.currentTimeMillis();
        List<RideVO> loaded = loadAllFitGpxTcxData();

        // 최신순 정렬
        loaded.sort((a, b) -> {
            LocalDateTime t1 = a.getActivityCoreVO().getStartTime();
            LocalDateTime t2 = b.getActivityCoreVO().getStartTime();
            return t2.compareTo(t1); // 최신 먼저
        });

        cache.addAll(loaded);
        System.out.println("✅ FIT 파일 로딩 완료 (" + (System.currentTimeMillis() - start) + "ms)");
    }


    public List<RideVO> getRideDataList() {
        return cache;
    }

    public List<RideVO> loadAllFitGpxTcxData() {
        List<RideVO> results = new ArrayList<>();
        results.addAll(loadFromFolder("/home/tho881/NAS/fit"));
        results.addAll(loadFromFolder("/home/tho881/NAS/gpx"));
        results.addAll(loadFromFolder("/home/tho881/NAS/tcx"));
        return results;
    }

    private List<RideVO> loadFromFolder(String folderPath) {
        List<RideVO> list = new ArrayList<>();

        try {
            File dir = new File(folderPath);
            File[] files = dir.listFiles();
            if (files == null) return Collections.emptyList();

            for (File file : files) {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".fit")) {
                    RideVO ride = processFitFile(file);
                    if (ride != null) list.add(ride);
                }
            }
        } catch (Exception e) {
            logger.error("❗ 파일 로딩 오류: " + folderPath, e);
        }

        return list;
    }

    private RideVO processFitFile(File file) {
        RideVO ride = new RideVO();
        ActivityCoreVO core = new ActivityCoreVO();
        List<ActivityPointVO> points = new ArrayList<>();

        try (InputStream is = new FileInputStream(file)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            broadcaster.addListener((SessionMesgListener) mesg -> {
                if (mesg.getStartTime() != null) {
                    LocalDateTime localDateTime = mesg.getStartTime().getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    core.setStartTime(localDateTime);
                }
                if (mesg.getTotalDistance() != null) core.setTotalDistance(mesg.getTotalDistance() / 1000.0);
                if (mesg.getTotalCalories() != null) core.setTotalCalories(mesg.getTotalCalories());
                if (mesg.getTotalTimerTime() != null) core.setMovingTime((int) (mesg.getTotalTimerTime() / 60));
                if (mesg.getTotalElapsedTime() != null) {
                    core.setTotalTime((int) (mesg.getTotalElapsedTime() / 60));

                    LocalDateTime localDateTime = mesg.getStartTime().getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    LocalDateTime localEndTime = localDateTime.plusSeconds(mesg.getTotalElapsedTime().longValue());
                    core.setEndTime(localEndTime);
                }
                if (mesg.getTotalAscent() != null) core.setTotalAscent((int) mesg.getTotalAscent());
                core.setFilename(file.getName());
            });

            broadcaster.addListener((RecordMesgListener) mesg -> {
                if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                    double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    ActivityPointVO point = new ActivityPointVO(lat, lon);
                    points.add(point);
                }
            });

            decode.read(is, broadcaster);

        } catch (Exception e) {
            logger.warn("❌ TCX 처리 실패: {} → {}", file.getName(), e.getMessage());
            return null;
        }

        ride.setActivityCoreVO(core);
        ride.setRoute(points);
        return ride;
    }

    public RideVO readFromFile(String fileName) {
        RideVO ride = new RideVO();
        List<ActivityPointVO> route = new ArrayList<>();
        ActivityCoreVO core = new ActivityCoreVO();

        String linuxPath = "/home/tho881/NAS/fit/" + fileName;
        try (InputStream in = Files.newInputStream(Paths.get(linuxPath))) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            // ✅ Session 메시지 리스너 등록 (총합 데이터: 거리, 칼로리, 총 시간, 고도 상승)
            broadcaster.addListener((SessionMesgListener) mesg -> {
                if (mesg.getStartTime() != null && core.getStartTime() == null) {
                    core.setStartTime(mesg.getStartTime().getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime());
                }
                if (mesg.getTotalDistance() != null) {
                    core.setTotalDistance(mesg.getTotalDistance() / 1000.0);
                }
                if (mesg.getTotalCalories() != null) {
                    core.setTotalCalories(mesg.getTotalCalories());
                }
                if (mesg.getTotalTimerTime() != null) {
                    core.setMovingTime((int) (mesg.getTotalTimerTime() / 60));
                }
                if (mesg.getTotalElapsedTime() != null) {
                    core.setTotalTime((int) (mesg.getTotalElapsedTime() / 60));
                }
                if (mesg.getTotalAscent() != null) {
                    core.setTotalAscent(mesg.getTotalAscent().intValue()); // ✅ 고도 상승 추가
                }
            });

            // ✅ Record 메시지 리스너 등록 (위치, 거리 계속 업데이트)
            broadcaster.addListener((RecordMesgListener) record -> {
                if (record.getTimestamp() != null && core.getStartTime() == null) {
                    core.setStartTime(Utils.convertFitTimestamp(record.getTimestamp().getTimestamp()));
                }
                if (record.getPositionLat() != null && record.getPositionLong() != null) {
                    double lat = record.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lng = record.getPositionLong() * (180.0 / Math.pow(2, 31));
                    route.add(new ActivityPointVO(lat, lng));
                }
                if (record.getDistance() != null && core.getTotalDistance() == null) {
                    core.setTotalDistance(record.getDistance() / 1000.0);
                }
            });

            decode.read(in, broadcaster);

            core.setFilename(fileName);
            ride.setActivityCoreVO(core);
            ride.setRoute(route);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ride;
    }

    // FitReader.java 안에 추가
    public ActivityCoreVO extractCoreInfo(File file) throws IOException {
        ActivityCoreVO core = new ActivityCoreVO();
        try (InputStream is = new FileInputStream(file)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
            broadcaster.addListener((SessionMesgListener) mesg -> {
                if (mesg.getStartTime() != null) {
                    LocalDateTime startTime = mesg.getStartTime().getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    core.setStartTime(startTime);
                }
            });
            decode.read(is, broadcaster);
        } catch (Exception e) {
            throw new IOException("FIT 파일 읽기 실패", e);
        }
        return core;
    }

    public List<double[]> extractRoutePoints(File file) throws IOException {
        List<double[]> points = new ArrayList<>();
        try (InputStream is = new FileInputStream(file)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
            broadcaster.addListener((RecordMesgListener) mesg -> {
                if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                    double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    points.add(new double[]{lat, lon});
                }
            });
            decode.read(is, broadcaster);
        } catch (Exception e) {
            throw new IOException("FIT 파일 읽기 실패", e);
        }
        return points;
    }

}
