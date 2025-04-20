package org.example.ridinginfomation.Garmin.Controller;

import org.example.ridinginfomation.Garmin.Util.FitReader;
import org.example.ridinginfomation.Garmin.VO.ActivityPointVO;
import org.example.ridinginfomation.Garmin.VO.RideVO;
import org.example.ridinginfomation.fit.Decode;
import org.example.ridinginfomation.fit.FitRuntimeException;
import org.example.ridinginfomation.fit.MesgBroadcaster;
import org.example.ridinginfomation.fit.RecordMesgListener;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/fit")
public class FitController {

    private final FitReader fitReader;

    public FitController(FitReader fitReader) {
        this.fitReader = fitReader;
    }

    @GetMapping("/connectTest")
    public Map<String, String> connectTest() {
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
    public List<RideVO> getFitRidesForJanuary2025() throws IOException {
        List<RideVO> result = new ArrayList<>();

        // ✅ fit 경로 처리
        Path fitDir = Paths.get("src/main/resources/fit/fit");
        try (Stream<Path> stream = Files.list(fitDir)) {
            stream.filter(p -> p.toString().endsWith(".fit")).forEach(file -> {
                try {
                    RideVO ride = fitReader.getRideByFile(file.getFileName().toString());
                    if (ride != null) {
                        result.add(ride);
                    }
                } catch (Exception e) {
                    System.out.println("❌ FIT 파일 읽기 실패: " + file + " → " + e.getMessage());
                }
            });
        }

        // ✅ gpx 경로 처리
        Path gpxDir = Paths.get("src/main/resources/fit/gpx");
        try (Stream<Path> stream = Files.list(gpxDir)) {
            stream.filter(p -> p.toString().endsWith(".gpx")).forEach(file -> {
                try {
                    RideVO ride = fitReader.getRideByFile(file.getFileName().toString());
                    if (ride != null) {
                        result.add(ride);
                    }
                } catch (Exception e) {
                    System.out.println("❌ GPX 파일 읽기 실패: " + file + " → " + e.getMessage());
                }
            });
        }

        // ✅ tcx 경로 처리
        Path tcxDir = Paths.get("src/main/resources/fit/tcx");
        try (Stream<Path> stream = Files.list(tcxDir)) {
            stream.filter(p -> p.toString().endsWith(".tcx")).forEach(file -> {
                try {
                    RideVO ride = fitReader.getRideByFile(file.getFileName().toString());
                    if (ride != null) {
                        result.add(ride);
                    }
                } catch (Exception e) {
                    System.out.println("❌ TCX 파일 읽기 실패: " + file + " → " + e.getMessage());
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