package org.example.ridinginfomation.Config.Garmin.Controller;

import org.example.ridinginfomation.Config.Garmin.Util.CsvReader;
import org.example.ridinginfomation.Config.Garmin.VO.ActivityVO;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins = {"*"})
@RestController
@RequestMapping("/api/active")
public class ActiveController {

    private final CsvReader csvReader;

    public ActiveController(CsvReader csvReader) {
        this.csvReader = csvReader;
    }

    Logger logger = Logger.getLogger(ActiveController.class.getName());

    @GetMapping("/connectTest")
    public Map<String, String> connectTest() {
        Map<String, String> connect = new HashMap<>();

        try {
            // 1. csv 폴더 위치 가져오기
            URL csvFolderUrl = getClass().getClassLoader().getResource("csv");
            if (csvFolderUrl == null) {
                connect.put("error", "csv 폴더를 찾을 수 없습니다.");
                return connect;
            }

            File csvFolder = new File(csvFolderUrl.toURI());
            File[] files = csvFolder.listFiles((dir, name) -> name.endsWith(".csv"));

            if (files == null || files.length == 0) {
                connect.put("error", "csv 폴더에 파일이 없습니다.");
                return connect;
            }

            File firstCsv = files[0];
            logger.info("🔥 첫 번째 CSV 파일 이름: " + firstCsv.getName());

            // 2. CSV 파싱
            List<ActivityVO> activities = csvReader.readActivitiesFromCsv(firstCsv.getAbsolutePath());

            // 3. 결과 로그 출력 (예시)
            logger.info("✅ CSV 활동 개수: " + activities.size());

            connect.put("Connected", "Success");
            connect.put("FirstFile", firstCsv.getName());
            connect.put("ActivityCount", String.valueOf(activities.size()));

            for(ActivityVO activity : activities){
                logger.info("활동 값 : " + activity.getActivityName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            connect.put("error", "예외 발생: " + e.getMessage());
        }

        return connect;
    }
}
