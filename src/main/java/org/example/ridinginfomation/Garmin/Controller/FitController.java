package org.example.ridinginfomation.Garmin.Controller;

import org.example.ridinginfomation.Garmin.Util.FitReader;
import org.example.ridinginfomation.Garmin.Util.Utils;
import org.example.ridinginfomation.Garmin.VO.ActivityPointVO;
import org.example.ridinginfomation.fit.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;


@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/fit")
public class FitController {

    private final FitReader fitReader;
    private final Utils utils;
    Logger logger = Logger.getLogger(FitController.class.getName());

    public FitController(FitReader fitReader, Utils utils) {
        this.fitReader = fitReader;
        this.utils = utils;
    }

    @GetMapping("/connectTest")
    public Map<String, String> connectTest() {
        logger.info("Connecting to Fit");
        Map<String, String> connect = new HashMap<>();
        connect.put("Connected", "Test");

        return connect;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Map<String, Double>>> uploadFitFile(@RequestParam("file") MultipartFile file) {
        List<Map<String, Double>> gpsPoints = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            Decode decode = new Decode();
            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);
            broadcaster.addListener((RecordMesgListener) recordMesg -> {
                if (recordMesg.getPositionLat() != null && recordMesg.getPositionLong() != null) {
                    double lat = recordMesg.getPositionLat() * (180.0 / Math.pow(2, 31));
                    double lon = recordMesg.getPositionLong() * (180.0 / Math.pow(2, 31));
                    Map<String, Double> point = new HashMap<>();
                    point.put("lat", lat);
                    point.put("lon", lon);
                    gpsPoints.add(point);
                }
            });

            decode.read(inputStream, broadcaster, broadcaster);
            return ResponseEntity.ok(gpsPoints);

        } catch (IOException | FitRuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @GetMapping("/files")
    public List<String> getFitFileNamesForJanuary2025() throws IOException {
        Path dir = Paths.get("src/main/resources/fit/fit");
        List<String> result = new ArrayList<>();

        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.toString().endsWith(".fit"))
                    .forEach(file -> {
                        try (InputStream in = Files.newInputStream(file)) {
                            Decode decode = new Decode();
                            MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

                            final LocalDateTime[] startTime = {null};

                            broadcaster.addListener((RecordMesg mesg) -> {
                                if (startTime[0] == null && mesg.getTimestamp() != null) {
                                    long fitTimestamp = mesg.getTimestamp().getTimestamp();
                                    startTime[0] = utils.convertFitTimestamp(fitTimestamp); // ✅ 여기에 적용!
                                }
                            });

                            decode.read(in, broadcaster);

                            if (startTime[0] != null && startTime[0].getYear() == 2025 && (startTime[0].getMonthValue() == 1 || startTime[0].getMonthValue() == 2)) {
                                String name = file.getFileName().toString();
                                result.add(name);
                                System.out.println("📂 추가됨: " + name + " ▶ 기록 시각: " + startTime[0]);
                            }

                        } catch (Exception e) {
                            System.out.println("❌ FIT 파일 읽기 실패: " + file + " → " + e.getMessage());
                        }
                    });
        }
        return result;
    }


    @GetMapping("/map-by-file")
    public ResponseEntity<List<ActivityPointVO>> getMapByFile(@RequestParam("file") String fileName) {
        if (fileName.endsWith(".fit")) {
            return fitReader.getPointsFromFit(fileName);
        } else if (fileName.endsWith(".gpx")) {
            return fitReader.loadPointsFromGpx(fileName);
        } else if (fileName.endsWith(".tcx")) {
            return fitReader.loadPointsFromTcx(fileName);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}