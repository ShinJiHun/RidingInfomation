package org.example.ridinginfomation.Garmin;

import org.example.ridinginfomation.Garmin.Util.Utils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class FitReaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitReaderApplication.class, args);
        Path folder = Paths.get("src/main/resources/fit/fit");
             Map<String, LocalDateTime> fileTimeMap = new HashMap<>();

             try {
                 Files.list(folder)
                     .filter(p -> p.toString().endsWith(".fit"))
                     .forEach(file -> {
                         LocalDateTime time = Utils.extractStartTime(file);
                         if (time != null) {
                             fileTimeMap.put(file.getFileName().toString(), time);
                         } else {
                             System.out.println("⚠️ " + file.getFileName() + " ▶ 시작 시간 없음 또는 오류");
                         }
                     });

                 // 📌 날짜 기준 오름차순 정렬 후 출력
                 fileTimeMap.entrySet().stream()
                     .sorted(Map.Entry.comparingByValue()) // LocalDateTime 오름차순
                     .forEach(entry ->
                         System.out.println("📂 " + entry.getKey() + " ▶ 기록 시작 시각: " + entry.getValue())
                     );

             } catch (IOException e) {
                 throw new RuntimeException(e);
             }
         }
}
