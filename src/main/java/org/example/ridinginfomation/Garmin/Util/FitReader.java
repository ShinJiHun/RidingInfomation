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

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.InputStream;

@Component
public class FitReader {

    private static final Logger logger = LoggerFactory.getLogger(FitReader.class);

    private final List<RideVO> cache = new ArrayList<>();

    @PostConstruct
    public void init() {
        System.out.println("🚴 FIT 파일 로딩 시작...");
        long start = System.currentTimeMillis();
        cache.addAll(loadAllFitGpxTcxData());
        System.out.println("✅ FIT 파일 로딩 완료 (" + (System.currentTimeMillis() - start) + "ms)");

        // 🔽 서버에 업로드 (이 부분!)
        try {
            Utils.uploadWithOsBasedKey("34.172.162.148", 22, "tho881");
        } catch (Exception e) {
            logger.error("❌ 서버 업로드 중 오류 발생", e);
        }
    }

    public List<RideVO> getRideDataList() {
        return cache;
    }

    public List<RideVO> loadAllFitGpxTcxData() {
        List<RideVO> results = new ArrayList<>();
        results.addAll(loadFromFolder("fit/fit"));
        results.addAll(loadFromFolder("fit/gpx"));
        results.addAll(loadFromFolder("fit/tcx"));
        return results;
    }

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
//                } else if (name.endsWith(".gpx")) {
//                    RideVO ride = processGpxFile(file);
//                    if (ride != null) list.add(ride);
//                } else if (name.endsWith(".tcx")) {
//                    RideVO ride = processTcxFile(file);
//                    if (ride != null) list.add(ride);
                }
                // TODO: GPX/TCX 처리 추가 예정
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
                if (mesg.getTotalTimerTime() != null) core.setMovingTime((int) (mesg.getTotalTimerTime() / 60)); // 🟢 라이딩 시간
                if (mesg.getTotalElapsedTime() != null) core.setTotalTime((int) (mesg.getTotalElapsedTime() / 60)); // 🔵 총 시간
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

    public List<String> getFitFileNames(String folderPath) {
        List<String> result = new ArrayList<>();

        try {
            URL resourceUrl = getClass().getClassLoader().getResource(folderPath);
            if (resourceUrl == null) {
                logger.warn("❌ 리소스를 찾을 수 없습니다: " + folderPath);
                return result;
            }

            File folder = new File(resourceUrl.toURI());
            File[] files = folder.listFiles((dir, name) ->
                    name.endsWith(".fit") || name.endsWith(".gpx") || name.endsWith(".tcx")
            );

            if (files != null) {
                for (File file : files) {
                    result.add(file.getName());
                }
            }
        } catch (Exception e) {
            logger.error("⚠️ 파일 목록 가져오기 실패: {}", e.getMessage());
        }

        return result;
    }

    public RideVO readFromFile(Path path) {
        RideVO ride = new RideVO();
        List<ActivityPointVO> route = new ArrayList<>();

        try (InputStream in = Files.newInputStream(path)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            final ActivityCoreVO core = new ActivityCoreVO();

            broadcaster.addListener((RecordMesgListener) record -> {
                if (record.getTimestamp() != null && core.getStartTime() == null) {
                    core.setStartTime(Utils.convertFitTimestamp(record.getTimestamp().getTimestamp()));
                }
                if (record.getPositionLat() != null && record.getPositionLong() != null) {
                    double lat = record.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lng = record.getPositionLong() * (180.0 / Math.pow(2, 31));
                    route.add(new ActivityPointVO(lat, lng));
                }
                if (record.getDistance() != null) {
                    core.setTotalDistance(record.getDistance() / 1000.0); // ✅ totalDistance 필드에 대응
                }
                if (record.getAltitude() != null) {
                    core.setTotalAscent(record.getAltitude().intValue()); // ✅ totalAscent 필드에 대응
                }
            });

            decode.read(in, broadcaster);

            core.setFilename(path.getFileName().toString()); // ✅ filename 필드에 대응
            ride.setActivityCoreVO(core);
            ride.setRoute(route);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ride;
    }
}
