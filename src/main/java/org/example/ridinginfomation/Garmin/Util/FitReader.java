package org.example.ridinginfomation.Garmin.Util;

import jakarta.annotation.PostConstruct;
import org.example.ridinginfomation.Garmin.VO.ActivityCoreVO;
import org.example.ridinginfomation.Garmin.VO.ActivityPointVO;
import org.example.ridinginfomation.Garmin.VO.RideVO;
import org.example.ridinginfomation.fit.Decode;
import org.example.ridinginfomation.fit.MesgBroadcaster;
import org.example.ridinginfomation.fit.RecordMesgListener;
import org.example.ridinginfomation.fit.SessionMesgListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Component
public class FitReader {

    private static final Logger logger = LoggerFactory.getLogger(FitReader.class);

    private final List<RideVO> cache = new ArrayList<>();

    @PostConstruct
    public void init() {
//        System.out.println("🚴 FIT 파일 로딩 시작...");
//        long start = System.currentTimeMillis();
//        cache.addAll(loadAllFitGpxTcxData());
//        System.out.println("✅ FIT 파일 로딩 완료 (" + (System.currentTimeMillis() - start) + "ms)");
//
//        // 🔽 서버에 업로드 (이 부분!)
//        try {
//            Utils.uploadWithOsBasedKey("34.70.180.38", 22, "jihoon.shin");
//        } catch (Exception e) {
//            logger.error("❌ 서버 업로드 중 오류 발생", e);
//        }
    }

//    public List<RideVO> getRideDataList() {
//        return cache;
//    }
//
//    public List<RideVO> loadAllFitGpxTcxData() {
//        List<RideVO> results = new ArrayList<>();
//        results.addAll(loadFromFolder("fit/fit"));
//        results.addAll(loadFromFolder("fit/gpx"));
//        results.addAll(loadFromFolder("fit/tcx"));
//        return results;
//    }

    private List<RideVO> loadFromFolder(String folderPath) {
        List<RideVO> list = new ArrayList<>();

        try {
            URL resource = getClass().getClassLoader().getResource(folderPath);
            if (resource == null) {
                logger.warn("❌ 리소스를 찾을 수 없습니다: " + folderPath);
                return Collections.emptyList();
            }

            File dir = new File(resource.getFile());
            File[] files = dir.listFiles();
            if (files == null) return Collections.emptyList();

            for (File file : files) {
                String name = file.getName().toLowerCase();

                if (name.endsWith(".fit")) {
                    RideVO ride = processFitFile(file);
                    if (ride != null) list.add(ride);
                } else if (name.endsWith(".gpx")) {
                    RideVO ride = processGpxFile(file);
                    if (ride != null) list.add(ride);
                } else if (name.endsWith(".tcx")) {
                    RideVO ride = processTcxFile(file);
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

            // ✅ Session 메시지 처리
            broadcaster.addListener((SessionMesgListener) mesg -> {
                Map<String, Object> jsonMap = new HashMap<>();
                jsonMap.put("startTime", mesg.getStartTime() != null ? mesg.getStartTime().toString() : null);
                jsonMap.put("totalDistance", mesg.getTotalDistance());
                jsonMap.put("totalCalories", mesg.getTotalCalories());
                jsonMap.put("totalAscent", mesg.getTotalAscent());
                jsonMap.put("totalTimerTime", mesg.getTotalTimerTime());

//                try {
//                    String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
//                    System.out.println("📦 SessionMesg JSON:\n" + json);
//                } catch (JsonProcessingException e) {
//                    logger.warn("❌ SessionMesg JSON 변환 실패", e);
//                }

                // ✅ core 세팅
                if (mesg.getStartTime() != null) {
                    LocalDateTime localDateTime = mesg.getStartTime().getDate()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();
                    core.setStartTime(localDateTime);
                }
                core.setTotalDistance(mesg.getTotalDistance() != null ? mesg.getTotalDistance() / 1000.0 : 0.0);
                core.setTotalCalories(mesg.getTotalCalories() != null ? mesg.getTotalCalories() : 0);
                core.setTotalTime(mesg.getTotalTimerTime() != null ? (int) (mesg.getTotalTimerTime() / 60) : 0);
                core.setTotalAscent(mesg.getTotalAscent() != null ? mesg.getTotalAscent() : 0);
                core.setFilename(file.getName());
            });

            // ✅ Record 메시지 처리
            broadcaster.addListener((RecordMesgListener) mesg -> {
                if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                    double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    points.add(new ActivityPointVO(lat, lon));
                }
            });

            decode.read(is, broadcaster);

        } catch (Exception e) {
            logger.warn("⚠️ FIT 파싱 오류 (" + file.getName() + "): " + e.getMessage(), e);
            return null;
        }

        ride.setActivityCoreVO(core);
        ride.setRoute(points);
        return ride;
    }

    private RideVO processGpxFile(File file) {
        List<ActivityPointVO> points = new ArrayList<>();
        ActivityCoreVO core = new ActivityCoreVO();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trkpts = doc.getElementsByTagName("trkpt");

            double totalDistance = 0.0;
            double totalAscent = 0.0;
            Double lastLat = null, lastLon = null, lastEle = null;

            for (int i = 0; i < trkpts.getLength(); i++) {
                Element trkpt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                double ele = 0.0;

                NodeList eleNode = trkpt.getElementsByTagName("ele");
                if (eleNode.getLength() > 0) {
                    ele = Double.parseDouble(eleNode.item(0).getTextContent());
                }

                // 고도 누적 계산
                if (lastEle != null && ele > lastEle) {
                    totalAscent += (ele - lastEle);
                }

                // 거리 누적 계산
                if (lastLat != null) {
                    totalDistance += haversine(lastLat, lastLon, lat, lon);
                }

                lastLat = lat;
                lastLon = lon;
                lastEle = ele;

                ActivityPointVO point = new ActivityPointVO(lat, lon);
                point.setAltitude(ele); // 고도 저장
                points.add(point);
            }

            core.setFilename(file.getName());
            core.setStartTime(Utils.extractStartTime(file.toPath()));
            core.setTotalDistance(Math.round(totalDistance * 100.0) / 100.0); // km
            core.setTotalAscent((int) totalAscent);
            core.setTotalCalories((int) (totalDistance * 40)); // 대략 거리 x 40kcal
            core.setTotalTime(points.size() / 60); // rough estimate

        } catch (Exception e) {
            logger.warn("❌ GPX 처리 실패: {}", e.getMessage());
            return null;
        }

        RideVO ride = new RideVO();
        ride.setActivityCoreVO(core);
        ride.setRoute(points);
        return ride;
    }

    private RideVO processTcxFile(File file) {
        List<ActivityPointVO> points = new ArrayList<>();
        ActivityCoreVO core = new ActivityCoreVO();
        double totalDistance = 0;
        double totalAscent = 0;
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        double lastAltitude = -1;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trackpoints = doc.getElementsByTagName("Trackpoint");
            for (int i = 0; i < trackpoints.getLength(); i++) {
                Element tp = (Element) trackpoints.item(i);

                // 🗺️ 위치 정보
                NodeList positions = tp.getElementsByTagName("Position");
                if (positions.getLength() > 0) {
                    Element pos = (Element) positions.item(0);
                    double lat = Double.parseDouble(pos.getElementsByTagName("LatitudeDegrees").item(0).getTextContent());
                    double lon = Double.parseDouble(pos.getElementsByTagName("LongitudeDegrees").item(0).getTextContent());
                    points.add(new ActivityPointVO(lat, lon));
                }

                // 🕒 시간
                NodeList timeNodes = tp.getElementsByTagName("Time");
                if (timeNodes.getLength() > 0) {
                    String timeText = timeNodes.item(0).getTextContent();
                    LocalDateTime time = LocalDateTime.parse(timeText.replace("Z", ""));
                    if (startTime == null) startTime = time;
                    endTime = time;
                }

                // 🧗 고도
                NodeList altNodes = tp.getElementsByTagName("AltitudeMeters");
                if (altNodes.getLength() > 0) {
                    double altitude = Double.parseDouble(altNodes.item(0).getTextContent());
                    if (lastAltitude > 0 && altitude > lastAltitude)
                        totalAscent += (altitude - lastAltitude);
                    lastAltitude = altitude;
                }

                // 📏 거리
                NodeList distNodes = tp.getElementsByTagName("DistanceMeters");
                if (distNodes.getLength() > 0) {
                    totalDistance = Double.parseDouble(distNodes.item(0).getTextContent());
                }
            }

            core.setFilename(file.getName());
            core.setStartTime(startTime);
            core.setEndTime(endTime);
            core.setTotalDistance(totalDistance / 1000.0); // km로 변환
            core.setTotalAscent((int) totalAscent);
            core.setTotalCalories(0); // 없음
            core.setTotalTime(startTime != null && endTime != null
                    ? (int) java.time.Duration.between(startTime, endTime).toMinutes()
                    : 0
            );

        } catch (Exception e) {
            logger.warn("❌ TCX 처리 실패: {} → {}", file.getName(), e.getMessage());
            return null;
        }

        RideVO ride = new RideVO();
        ride.setActivityCoreVO(core);
        ride.setRoute(points);
        return ride;
    }

    public ResponseEntity<List<ActivityPointVO>> getPointsFromFit(String fileName) {
        List<ActivityPointVO> points = new ArrayList<>();

        try {
            URL fitUrl = getClass().getClassLoader().getResource("fit/fit");
            if (fitUrl == null) return ResponseEntity.notFound().build();

            File file = new File(fitUrl.getFile(), fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();

            try (InputStream is = new FileInputStream(file)) {
                Decode decode = new Decode();
                MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

                broadcaster.addListener((RecordMesgListener) mesg -> {
                    if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                        double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                        double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                        points.add(new ActivityPointVO(lat, lon));
                    }
                });

                decode.read(is, broadcaster);
            }

        } catch (Exception e) {
            logger.warn("❗ FIT 파일 파싱 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok(points);
    }

    public ResponseEntity<List<ActivityPointVO>> loadPointsFromGpx(String fileName) {
        List<ActivityPointVO> points = new ArrayList<>();
        try {
            URL gpxUrl = getClass().getClassLoader().getResource("fit/gpx");
            if (gpxUrl == null) return ResponseEntity.notFound().build();

            File file = new File(gpxUrl.getFile(), fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trkpts = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < trkpts.getLength(); i++) {
                Element trkpt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                points.add(new ActivityPointVO(lat, lon));
            }
            System.out.println("GPX 파일 명 : " + fileName);
        } catch (Exception e) {
            logger.warn("❗ GPX 파싱 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok(points);
    }

    public ResponseEntity<List<ActivityPointVO>> loadPointsFromTcx(String fileName) {
        List<ActivityPointVO> points = new ArrayList<>();
        try {
            URL tcxUrl = getClass().getClassLoader().getResource("fit/tcx");
            if (tcxUrl == null) return ResponseEntity.notFound().build();

            File file = new File(tcxUrl.getFile(), fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trackpoints = doc.getElementsByTagName("Trackpoint");
            for (int i = 0; i < trackpoints.getLength(); i++) {
                Element tp = (Element) trackpoints.item(i);
                NodeList positions = tp.getElementsByTagName("Position");
                if (positions.getLength() > 0) {
                    Element pos = (Element) positions.item(0);
                    double lat = Double.parseDouble(pos.getElementsByTagName("LatitudeDegrees").item(0).getTextContent());
                    double lon = Double.parseDouble(pos.getElementsByTagName("LongitudeDegrees").item(0).getTextContent());
                    points.add(new ActivityPointVO(lat, lon));
                }
            }
        } catch (Exception e) {
            logger.warn("❗ TCX 파싱 실패: {}", e.getMessage());
            return ResponseEntity.status(500).body(null);
        }

        return ResponseEntity.ok(points);
    }

    public RideVO getRideByFile(String fileName) {
        try {
            String lower = fileName.toLowerCase();
            String folderPath;
            if (lower.endsWith(".fit")) {
                folderPath = "fit/fit/";
            } else if (lower.endsWith(".gpx")) {
                folderPath = "fit/gpx/";
            } else if (lower.endsWith(".tcx")) {
                folderPath = "fit/tcx/";
            } else {
                logger.warn("❌ 지원하지 않는 파일 형식: " + fileName);
                return null;
            }

            URL resource = getClass().getClassLoader().getResource(folderPath + fileName);
            if (resource == null) return null;

            File file = new File(resource.getFile());

            if (lower.endsWith(".fit")) return processFitFile(file);
            else if (lower.endsWith(".gpx")) return processGpxFile(file);
            else if (lower.endsWith(".tcx")) return processTcxFile(file);

        } catch (Exception e) {
            logger.error("❌ RideVO 생성 실패: {}", e.getMessage());
        }
        return null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of Earth in KM
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
