// ✅ FitUploadService.java
package org.example.ridinginfomation.Garmin.Service;

import org.example.ridinginfomation.Garmin.Util.FitReader;
import org.example.ridinginfomation.Garmin.Util.MapImageGenerator;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class FitUploadService {

    private final FitReader fitReader;
    private final MapImageGenerator mapImageGenerator;

    public FitUploadService(FitReader fitReader, MapImageGenerator mapImageGenerator) {
        this.fitReader = fitReader;
        this.mapImageGenerator = mapImageGenerator;
    }

    public void uploadFitFile(MultipartFile file) throws IOException {
        // 1. 임시 저장
        String tempPath = "/home/tho881/NAS/temp/" + file.getOriginalFilename();
        File tempFile = new File(tempPath);
        file.transferTo(tempFile);

        // 2. FIT 파일에서 시작시간(startTime) 추출
        var core = fitReader.extractCoreInfo(tempFile);
        if (core.getStartTime() == null) {
            throw new IllegalStateException("FIT 파일에서 시작 시간을 읽을 수 없습니다");
        }
        String folderName = core.getStartTime().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 3. 폴더 생성
        String targetFolderPath = "/home/tho881/NAS/" + folderName;
        File targetFolder = new File(targetFolderPath);
        if (!targetFolder.exists()) targetFolder.mkdirs();

        // 4. FIT 파일 이동
        File targetFitFile = new File(targetFolder, file.getOriginalFilename());
        if (!tempFile.renameTo(targetFitFile)) {
            throw new IOException("FIT 파일 이동 실패");
        }

        // 5. 경로 좌표 추출
        List<double[]> routePoints = fitReader.extractRoutePoints(targetFitFile);

        // 6. 지도 이미지 생성
        mapImageGenerator.generateMapImage(routePoints, targetFolderPath + "/map.png");
    }
}
