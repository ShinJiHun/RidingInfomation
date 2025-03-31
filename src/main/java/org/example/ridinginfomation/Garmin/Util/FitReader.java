package org.example.ridinginfomation.Garmin.Util;

import jakarta.annotation.PostConstruct;
import org.example.ridinginfomation.Garmin.VO.FitVo;
import org.example.ridinginfomation.Garmin.VO.MapVO;
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
import java.util.*;

@Component
public class FitReader {

    private final List<FitVo> fitCache = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(FitReader.class);

    @PostConstruct
    public void init() {
        System.out.println("🚴 FIT 파일 로딩 시작...");
        long start = System.currentTimeMillis();
        fitCache.addAll(loadFitData());
        System.out.println("✅ FIT 파일 로딩 완료 (" + (System.currentTimeMillis() - start) + "ms)");
    }

    public List<FitVo> getFitList() {
        return fitCache;
    }

    public List<FitVo> loadFitData() {
        var fitFolderUrl = getClass().getClassLoader().getResource("fit/fit");
        if (fitFolderUrl == null) {
            System.out.println("❌ fit 폴더를 찾을 수 없습니다.");
            return null;
        }

        var dir = new File(fitFolderUrl.getFile());
        var files = dir.listFiles((d, name) -> name.endsWith(".fit"));

        if (files == null || files.length == 0) {
            System.out.println("❌ fit 폴더에 .fit 파일이 없습니다.");
            return null;
        }

        List<FitVo> resultList = new ArrayList<>();

        for (var file : files) {
            try (InputStream inputStream = new FileInputStream(file)) {
                Decode decode = new Decode();
                MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

                broadcaster.addListener((SessionMesgListener) mesg -> {
                    double distanceKm = 0.0;
                    int calories = 0;
                    int durationMin = 0;
                    Date ridingDate = null;
                    float altitude = 0.0f;

                    if (mesg.getTotalDistance() != null)
                        distanceKm = mesg.getTotalDistance() / 1000.0;

                    if (mesg.getTotalCalories() != null)
                        calories = mesg.getTotalCalories();

                    if (mesg.getTotalTimerTime() != null)
                        durationMin = (int) (mesg.getTotalTimerTime() / 60);

                    if (mesg.getStartTime() != null)
                        ridingDate = mesg.getStartTime().getDate();

                    if (mesg.getTotalAscent() != null)
                        altitude = mesg.getTotalAscent();

                    resultList.add(new FitVo(distanceKm, calories, durationMin, ridingDate, altitude));
                });

                decode.read(inputStream, broadcaster);
            } catch (Exception e) {
                System.out.println("⚠️ 오류 발생: " + e.getMessage());
            }
        }

        return resultList;
    }

    public List<String> getFitFileNames(String folderPath) {
        try {
            // 클래스패스에서 리소스 디렉터리 찾기
            URL resourceUrl = getClass().getClassLoader().getResource(folderPath);
            if (resourceUrl == null) {
                logger.warn("❗ 리소스를 찾을 수 없습니다: " + folderPath);
                return Collections.emptyList();
            }

            File folder = new File(resourceUrl.toURI());
            String[] fileNames = folder.list((dir, name) ->
                    name.endsWith(".fit") || name.endsWith(".gpx") || name.endsWith(".tcx")
            );

            if (fileNames != null) {
                for (String fileName : fileNames) {
                    logger.info("getFitFileNames : " + fileName);
                }
                return Arrays.asList(fileNames);
            } else {
                logger.warn("❗ 해당 폴더에 파일이 없습니다: " + folder.getAbsolutePath());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("❗ 리소스 접근 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    public ResponseEntity<MapVO> getMapForFile(String fileName) {
        var fitFolderUrl = getClass().getClassLoader().getResource("fit/fit"); // ✅ 수정됨

        if (fitFolderUrl == null) return ResponseEntity.notFound().build();

        File file = new File(fitFolderUrl.getFile(), fileName);
        if (!file.exists()) return ResponseEntity.notFound().build();

        MapVO map = new MapVO(); // ✅ map 객체 선언 및 초기화

        try (InputStream inputStream = new FileInputStream(file)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

            broadcaster.addListener((RecordMesgListener) mesg -> {
                if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                    double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    map.addPoint(lat, lon);
                }
            });

            decode.read(inputStream, broadcaster);
        } catch (Exception e) {
            System.out.println("⚠️ 오류 발생 (" + file.getName() + "): " + e.getMessage());
        }

        return ResponseEntity.ok(map);
    }

    private void readFitFiles(String resourcePath, MapVO map) {
        var url = getClass().getClassLoader().getResource(resourcePath);
        if (url == null) return;

        File dir = new File(url.getFile());
        File[] files = dir.listFiles((d, name) -> name.endsWith(".fit"));
        if (files == null) return;

        for (File file : files) {
            try (InputStream inputStream = new FileInputStream(file)) {
                Decode decode = new Decode();
                MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

                broadcaster.addListener((RecordMesgListener) mesg -> {
                    if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                        double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                        double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                        map.addPoint(lat, lon);
                    }
                });

                decode.read(inputStream, broadcaster);

            } catch (Exception e) {
                System.out.println("⚠️ FIT 오류: " + file.getName() + " → " + e.getMessage());
            }
        }
    }

    public ResponseEntity<MapVO> loadMapDataFromGpx(String fileName) {
        var map = new MapVO();

        try {
            URL gpxFolderUrl = getClass().getClassLoader().getResource("fit/gpx");
            if (gpxFolderUrl == null) return ResponseEntity.notFound().build();

            File file = new File(gpxFolderUrl.getFile(), fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trkpts = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < trkpts.getLength(); i++) {
                Element trkpt = (Element) trkpts.item(i);
                double lat = Double.parseDouble(trkpt.getAttribute("lat"));
                double lon = Double.parseDouble(trkpt.getAttribute("lon"));
                map.addPoint(lat, lon); // ✅ 동일한 구조
            }
        } catch (Exception e) {
            System.out.println("⚠️ GPX 파싱 오류 (" + fileName + "): " + e.getMessage());
        }

        return ResponseEntity.ok(map);
    }

    public ResponseEntity<MapVO> loadMapDataFromTcx(String fileName) {
        var map = new MapVO();

        try {
            URL tcxFolderUrl = getClass().getClassLoader().getResource("fit/tcx");
            if (tcxFolderUrl == null) return ResponseEntity.notFound().build();

            File file = new File(tcxFolderUrl.getFile(), fileName);
            if (!file.exists()) return ResponseEntity.notFound().build();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);

            NodeList trackpoints = doc.getElementsByTagName("Trackpoint");
            for (int i = 0; i < trackpoints.getLength(); i++) {
                Element trackpoint = (Element) trackpoints.item(i);
                NodeList positionNodes = trackpoint.getElementsByTagName("Position");

                if (positionNodes.getLength() > 0) {
                    Element position = (Element) positionNodes.item(0);
                    NodeList latNodes = position.getElementsByTagName("LatitudeDegrees");
                    NodeList lonNodes = position.getElementsByTagName("LongitudeDegrees");

                    if (latNodes.getLength() > 0 && lonNodes.getLength() > 0) {
                        double lat = Double.parseDouble(latNodes.item(0).getTextContent());
                        double lon = Double.parseDouble(lonNodes.item(0).getTextContent());
                        map.addPoint(lat, lon); // ✅ 동일한 구조
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("⚠️ TCX 파싱 오류 (" + fileName + "): " + e.getMessage());
        }

        return ResponseEntity.ok(map);
    }
}
