package org.example.ridinginfomation.Garmin.Controller;

import org.example.ridinginfomation.Garmin.Util.FitReader;
import org.example.ridinginfomation.Garmin.Util.Utils;
import org.example.ridinginfomation.Garmin.VO.ActivityCoreVO;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.nio.file.Paths;


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
    public List<ActivityCoreVO> getFitSummaryOnly() {
        return fitReader.getRideDataList().stream()
                .map(ride -> ride.getActivityCoreVO())
                .toList();
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

    @GetMapping("/detail/{filename}")
    public ResponseEntity<RideVO> getFitDetail(@PathVariable String filename) {
        RideVO ride = fitReader.readFromFile(Paths.get("/home/tho881/NAS/fit/" + filename));
        return ResponseEntity.ok(ride);
    }
}