package org.example.ridinginfomation.Garmin.VO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class ActivityCoreVO {
    private String filename;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalDistance;
    private int totalCalories;
    private int totalTime;
    private int totalAscent;

    @Override
    public String toString() {
        return String.format("파일: %s | 거리: %.2f km | 고도: %d m | 칼로리: %d kcal | 시간: %d분",
                filename, totalDistance, totalAscent, totalCalories, totalTime);
    }
}