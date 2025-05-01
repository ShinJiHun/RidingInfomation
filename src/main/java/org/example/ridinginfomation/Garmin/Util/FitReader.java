package org.example.ridinginfomation.Garmin.Util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesgListener;
import com.garmin.fit.SessionMesgListener;
import jakarta.annotation.PostConstruct;
import org.example.ridinginfomation.Garmin.Entity.ActivityCoreEntity;
import org.example.ridinginfomation.Garmin.Entity.ActivityCoreEntityIgnoreRouteMixin;
import org.example.ridinginfomation.Garmin.Entity.ActivityPointEntity;
import org.example.ridinginfomation.Garmin.Repository.ActivityCoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class FitReader {

    private static final Logger logger = LoggerFactory.getLogger(FitReader.class);
    private final ActivityCoreRepository coreRepo;

    @Value("${paths.local-root.linux}")
    private String nasRootPath;

    public FitReader(ActivityCoreRepository coreRepo) {
        this.coreRepo = coreRepo;
    }

    int fitCnt = 0, gpxCnt = 0, tcxCnt = 0;

    @PostConstruct
    public void init() throws IOException {
        logger.info("🚴 라이딩 파일 로딩 시작...");
        long start = System.currentTimeMillis();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL); // optional
        objectMapper.addMixIn(ActivityCoreEntity.class, ActivityCoreEntityIgnoreRouteMixin.class);

        File backupFile = new File(nasRootPath + "/backup/activity_core_backup.json");

        if (backupFile.exists()) {
            try {
                ActivityCoreEntity[] backupArray = objectMapper.readValue(backupFile, ActivityCoreEntity[].class);
                Set<String> dbFilenames = new HashSet<>(coreRepo.findAllFilenames());

                List<ActivityCoreEntity> toRestore = Arrays.stream(backupArray)
                        .filter(e -> e.getFilename() != null && !dbFilenames.contains(e.getFilename()))
                        .collect(Collectors.toList());

                if (!toRestore.isEmpty()) {
                    coreRepo.saveAll(toRestore);
                    logger.info("📦 누락된 백업 데이터 복원 완료: {}건", toRestore.size());
                }
            } catch (Exception e) {
                logger.error("⚠️ 백업 파일 복원 중 오류 발생", e);
            }
        }

        List<File> nasFiles = new ArrayList<>();
        nasFiles.addAll(listFiles(nasRootPath + "/fit"));
        nasFiles.addAll(listFiles(nasRootPath + "/gpx"));
        nasFiles.addAll(listFiles(nasRootPath + "/tcx"));

        Map<String, File> nasFileMap = new HashMap<>();
        for (File file : nasFiles) {
            nasFileMap.put(file.getName(), file);
        }

        List<ActivityCoreEntity> dbEntities = coreRepo.findAll();
        Set<String> dbFilenames = dbEntities.stream()
                .map(ActivityCoreEntity::getFilename)
                .collect(Collectors.toSet());

        for (Map.Entry<String, File> entry : nasFileMap.entrySet()) {
            String filename = entry.getKey();
            File file = entry.getValue();

            LocalDateTime fileModifiedTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(file.lastModified()), ZoneId.systemDefault()
            );

            ActivityCoreEntity existing = coreRepo.findByFilename(filename);
            boolean needsProcessing = existing == null || fileModifiedTime.isAfter(existing.getLastModifiedTime());

            if (needsProcessing) {
                try {
                    ActivityCoreEntity entity = null;
                    if (filename.endsWith(".fit")) entity = processFitFile(file);
                    else if (filename.endsWith(".gpx")) entity = processGpxFile(file);
                    else if (filename.endsWith(".tcx")) entity = processTcxFile(file);

                    if (entity != null) {
                        entity.setLastModifiedTime(fileModifiedTime);
                        if (entity.getTotalCalories() == null && entity.getTotalDistance() != null)
                            entity.setTotalCalories((int) (entity.getTotalDistance() * 30));
                        coreRepo.save(entity);
                        logger.info("✅ 파일 반영 완료: {}", filename);
                    }

                } catch (Exception e) {
                    logger.warn("❌ 파일 처리 실패: {}", filename, e);
                }
            }
        }

        for (ActivityCoreEntity entity : dbEntities) {
            if (!nasFileMap.containsKey(entity.getFilename())) {
                coreRepo.delete(entity);
                logger.info("🗑️ NAS에 존재하지 않아 DB에서 삭제됨: {}", entity.getFilename());
            }
        }

        logger.info("🏁 전체 동기화 완료 ({}ms)", (System.currentTimeMillis() - start));

        try {
            List<ActivityCoreEntity> allData = coreRepo.findAll();  // ⚠️ route 제외
            File backupDir = new File(nasRootPath + "/backup");
            if (!backupDir.exists()) backupDir.mkdirs();

            File backupFileFinal = new File(backupDir, "activity_core_backup.json");
            File tempFile = new File(backupDir, "activity_core_backup_temp.json");

            objectMapper.writeValue(tempFile, allData); // 임시 파일에 먼저 쓰기
            if (backupFileFinal.exists()) backupFileFinal.delete(); // 기존 백업 삭제
            boolean renamed = tempFile.renameTo(backupFileFinal); // 안전하게 교체

            if (renamed) {
                logger.info("💾 최종 DB 백업 완료: {}", backupFileFinal.getPath());
            } else {
                logger.warn("⚠️ 임시 파일에서 백업 파일로 이름 변경 실패");
            }
        } catch (Exception e) {
            logger.warn("⚠️ 백업 저장 실패", e);
        }

        backupDatabaseToSqlFile();
    }

    private List<File> listFiles(String folderPath) {
        File dir = new File(folderPath);
        File[] files = dir.listFiles();
        if (files == null) return Collections.emptyList();
        List<File> result = new ArrayList<>();
        for (File file : files) if (file.isFile()) result.add(file);
        return result;
    }

    public ActivityCoreEntity processFitFile(File file) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
            ActivityCoreEntity core = new ActivityCoreEntity();
            List<ActivityPointEntity> route = new ArrayList<>();

            broadcaster.addListener((SessionMesgListener) mesg -> {
                if (mesg.getStartTime() != null) {
                    LocalDateTime startTime = mesg.getStartTime().getDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDateTime();
                    core.setStartTime(startTime);
                    if (mesg.getTotalElapsedTime() != null)
                        core.setEndTime(startTime.plusSeconds(mesg.getTotalElapsedTime().longValue()));
                }
                core.setFilename(file.getName());
                if (mesg.getTotalDistance() != null) core.setTotalDistance(mesg.getTotalDistance() / 1000.0);
                if (mesg.getTotalCalories() != null) core.setTotalCalories(mesg.getTotalCalories());
                if (mesg.getTotalTimerTime() != null) core.setMovingTime((int) (mesg.getTotalTimerTime() / 60));
                if (mesg.getTotalElapsedTime() != null) core.setTotalTime((int) (mesg.getTotalElapsedTime() / 60));
                if (mesg.getTotalAscent() != null) core.setTotalAscent(mesg.getTotalAscent().intValue());
            });

            broadcaster.addListener((RecordMesgListener) mesg -> {
                if (mesg.getPositionLat() != null && mesg.getPositionLong() != null) {
                    double lat = mesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = mesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    ActivityPointEntity point = new ActivityPointEntity();
                    point.setLatitude(lat);
                    point.setLongitude(lon);
                    if (mesg.getAltitude() != null) point.setAltitude(Double.valueOf(mesg.getAltitude()));
                    if (mesg.getDistance() != null) point.setDistance(Double.valueOf(mesg.getDistance()));
                    if (mesg.getTimestamp() != null)
                        point.setTimestamp(mesg.getTimestamp().getDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDateTime());
                    point.setCore(core);
                    route.add(point);
                }
            });

            decode.read(is, broadcaster);
            core.setRoute(route);
            return core;
        }
    }

    public ActivityCoreEntity processGpxFile(File file) throws Exception {
        ActivityCoreEntity core = new ActivityCoreEntity();
        List<ActivityPointEntity> route = new ArrayList<>();
        try (InputStream fis = new FileInputStream(file);
             Reader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            InputSource is = new InputSource(reader);
            is.setEncoding("UTF-8");
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
            doc.getDocumentElement().normalize();
            NodeList trkpts = doc.getElementsByTagName("trkpt");
            for (int i = 0; i < trkpts.getLength(); i++) {
                Element e = (Element) trkpts.item(i);
                ActivityPointEntity point = new ActivityPointEntity();
                point.setLatitude(Double.parseDouble(e.getAttribute("lat")));
                point.setLongitude(Double.parseDouble(e.getAttribute("lon")));
                point.setAltitude(e.getElementsByTagName("ele").getLength() > 0 ?
                        Double.parseDouble(e.getElementsByTagName("ele").item(0).getTextContent()) : 0.0);
                if (e.getElementsByTagName("time").getLength() > 0)
                    point.setTimestamp(LocalDateTime.parse(e.getElementsByTagName("time").item(0).getTextContent().replace("Z", "")));
                point.setCore(core);
                route.add(point);
            }
            core.setFilename(file.getName());
            core.setRoute(route);
            if (!route.isEmpty()) {
                core.setStartTime(route.get(0).getTimestamp());
                core.setEndTime(route.get(route.size() - 1).getTimestamp());
                core.setTotalDistance(calculateTotalDistance(route));
                core.setTotalAscent((int) calculateTotalAscent(route));
                core.setTotalTime((int) Duration.between(core.getStartTime(), core.getEndTime()).toMinutes());
                core.setMovingTime(estimateMovingTime(route));
            }
            return core;
        }
    }

    public ActivityCoreEntity processTcxFile(File file) throws Exception {
        ActivityCoreEntity core = new ActivityCoreEntity();
        List<ActivityPointEntity> route = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line.trim());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();
        NodeList trackpoints = doc.getElementsByTagName("Trackpoint");
        for (int i = 0; i < trackpoints.getLength(); i++) {
            Element tp = (Element) trackpoints.item(i);
            NodeList posList = tp.getElementsByTagName("Position");
            if (posList.getLength() == 0) continue;
            Element pos = (Element) posList.item(0);
            ActivityPointEntity point = new ActivityPointEntity();
            point.setLatitude(Double.parseDouble(pos.getElementsByTagName("LatitudeDegrees").item(0).getTextContent()));
            point.setLongitude(Double.parseDouble(pos.getElementsByTagName("LongitudeDegrees").item(0).getTextContent()));
            point.setAltitude(tp.getElementsByTagName("AltitudeMeters").getLength() > 0 ?
                    Double.parseDouble(tp.getElementsByTagName("AltitudeMeters").item(0).getTextContent()) : 0.0);
            if (tp.getElementsByTagName("Time").getLength() > 0)
                point.setTimestamp(LocalDateTime.parse(tp.getElementsByTagName("Time").item(0).getTextContent().replace("Z", "")));
            point.setCore(core);
            route.add(point);
        }
        core.setFilename(file.getName());
        core.setRoute(route);
        if (!route.isEmpty()) {
            core.setStartTime(route.get(0).getTimestamp());
            core.setEndTime(route.get(route.size() - 1).getTimestamp());
            core.setTotalDistance(calculateTotalDistance(route));
            core.setTotalAscent((int) calculateTotalAscent(route));
            core.setTotalTime((int) Duration.between(core.getStartTime(), core.getEndTime()).toMinutes());
            core.setMovingTime(estimateMovingTime(route));
        }
        return core;
    }

    private double calculateTotalDistance(List<ActivityPointEntity> points) {
        double dist = 0.0;
        for (int i = 1; i < points.size(); i++)
            dist += haversine(points.get(i - 1).getLatitude(), points.get(i - 1).getLongitude(),
                    points.get(i).getLatitude(), points.get(i).getLongitude());
        return dist;
    }

    private double calculateTotalAscent(List<ActivityPointEntity> points) {
        double ascent = 0.0;
        for (int i = 1; i < points.size(); i++) {
            double diff = points.get(i).getAltitude() - points.get(i - 1).getAltitude();
            if (diff > 0) ascent += diff;
        }
        return ascent;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public int estimateMovingTime(List<ActivityPointEntity> points) {
        Duration moving = Duration.ZERO;
        for (int i = 1; i < points.size(); i++) {
            LocalDateTime prev = points.get(i - 1).getTimestamp();
            LocalDateTime curr = points.get(i).getTimestamp();
            Duration delta = Duration.between(prev, curr);
            if (!delta.isNegative() && delta.getSeconds() <= 10)
                moving = moving.plus(delta);
        }
        return (int) moving.toMinutes();
    }

    public void backupDatabaseToSqlFile() {
        String backupDir = nasRootPath + "/backup";
        String backupFile = backupDir + "/riding_backup.sql";

        // ✅ 백업 폴더 없으면 생성
        new File(backupDir).mkdirs();

        String username = "root";          // DB 사용자명
        String password = "1234";          // DB 비밀번호
        String database = "riding_db";     // DB 이름

        String dumpCommand = String.format(
                "mysqldump -u%s -p%s --databases %s > %s",
                username, password, database, backupFile
        );

        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/bin/bash", "-c", dumpCommand});
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.info("✅ DB SQL 백업 완료: {}", backupFile);
            } else {
                logger.error("❌ DB SQL 백업 실패 (exit code {})", exitCode);
            }
        } catch (Exception e) {
            logger.error("❌ DB SQL 백업 중 예외 발생", e);
        }
    }
}
