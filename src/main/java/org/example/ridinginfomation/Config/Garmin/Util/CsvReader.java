package org.example.ridinginfomation.Config.Garmin.Util;

import org.example.ridinginfomation.Config.Garmin.VO.ActivityVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsvReader {

    private static final Logger logger = LoggerFactory.getLogger(CsvReader.class);

    public List<ActivityVO> readActivitiesFromCsv(String path) {
        List<ActivityVO> activities = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(path, StandardCharsets.UTF_8))) {
            String header = br.readLine(); // 첫 줄은 헤더
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] tokens = parseCsvLine(line);
                    ActivityVO vo = convertToActivityVO(tokens);
                    if (vo != null) {
                        logger.info("Read ActivityVO: activityId={}, activityName={}, activityDate={}",
                                vo.getActivityId(), vo.getActivityName(), vo.getActivityDate());
                        activities.add(vo);
                    }
                } catch (Exception e) {
                    logger.warn("⚠️ VO 변환 중 오류: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("CSV 파일 읽기 실패: {}", e.getMessage());
        }

        return activities;
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());

        return tokens.toArray(new String[0]);
    }

    private ActivityVO convertToActivityVO(String[] tokens) {
        if (tokens.length < 3) throw new IllegalArgumentException("empty String");

        ActivityVO vo = new ActivityVO();
        try {
            vo.setActivityId(parseLong(tokens[0]));
            vo.setActivityDate(tokens[1]);
            vo.setActivityName(tokens[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("VO 변환 실패: " + e.getMessage());
        }
        return vo;
    }

    private Long parseLong(String str) {
        if (str == null || str.isEmpty()) return 0L;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}