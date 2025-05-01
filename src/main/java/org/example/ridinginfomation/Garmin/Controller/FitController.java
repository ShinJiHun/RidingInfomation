package org.example.ridinginfomation.Garmin.Controller;

import com.garmin.fit.Decode;
import com.garmin.fit.MesgBroadcaster;
import com.garmin.fit.RecordMesgListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/fit")
public class FitController {

    Logger logger = Logger.getLogger(FitController.class.getName());

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

        } catch (Exception e) {
            logger.warning("❌ FIT 파일 처리 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
