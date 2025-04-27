package org.example.ridinginfomation.Garmin.Util;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.List;

@Component
public class MapImageGenerator {

    private static final String BASE_URL = "https://staticmap.openstreetmap.de/staticmap.php";

    public void generateMapImage(List<double[]> routePoints, String savePath) {
        try {
            if (routePoints.isEmpty()) {
                throw new IllegalArgumentException("❌ 경로 데이터가 없습니다");
            }

            // 1. 중심 좌표 계산 (여기서는 첫 번째 포인트를 중심으로 함)
            double[] center = routePoints.get(routePoints.size() / 2);
            double centerLat = center[0];
            double centerLon = center[1];

            // 2. 경로 마커 만들기
            StringBuilder markers = new StringBuilder();
            for (double[] point : routePoints) {
                markers.append("&markers=").append(point[0]).append(",").append(point[1]);
            }

            // 3. StaticMap API URL 생성
            String url = BASE_URL +
                    "?center=" + centerLat + "," + centerLon +
                    "&zoom=13" +
                    "&size=600x400" +
                    "&maptype=mapnik" +
                    markers;

            System.out.println("✅ 요청할 지도 URL: " + url);

            // 4. 이미지 다운로드
            BufferedImage image = ImageIO.read(new URL(url));
            File outputfile = new File(savePath);
            ImageIO.write(image, "png", outputfile);

            System.out.println("✅ 지도 이미지 생성 완료: " + savePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ 지도 이미지 생성 실패", e);
        }
    }
}
