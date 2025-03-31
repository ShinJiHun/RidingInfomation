package org.example.ridinginfomation.Garmin.Controller;

import org.example.ridinginfomation.Garmin.VO.FitVo;
import org.example.ridinginfomation.Garmin.VO.MapVO;
import org.example.ridinginfomation.Garmin.Util.FitReader;
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

@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/fit")
public class FitController {

    private final FitReader fitReader;

    public FitController(FitReader fitReader) {
        this.fitReader = fitReader;
    }

    Logger logger = Logger.getLogger(FitController.class.getName());

    @GetMapping("/connectTest")
    public Map<String, String> connectTest() {
        logger.info("Connecting to Fit");
        Map<String, String> connect = new HashMap<>();
        connect.put("Connected", "Test");

        return connect;
    }

    @GetMapping("/summary")
    public List<FitVo> getFitSummaries() {
        return fitReader.getFitList();
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
    public ResponseEntity<List<String>> getAllFileNames() {
        System.out.println("🔥 /files 요청 도착");

        List<String> files = new ArrayList<>();
        files.addAll(fitReader.getFitFileNames("fit/fit"));
        files.addAll(fitReader.getFitFileNames("fit/gpx"));
        files.addAll(fitReader.getFitFileNames("fit/tcx"));

        return ResponseEntity.ok(files);
    }


    @GetMapping("/map-by-file")
    public ResponseEntity<MapVO> getMapByFile(@RequestParam("file") String fileName) {
        if (fileName.endsWith(".fit")) {
            return fitReader.getMapForFile(fileName);
        } else if (fileName.endsWith(".gpx")) {
            return fitReader.loadMapDataFromGpx(fileName);
        } else if (fileName.endsWith(".tcx")) {
            return fitReader.loadMapDataFromTcx(fileName);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

}