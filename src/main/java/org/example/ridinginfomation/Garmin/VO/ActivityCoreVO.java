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
}