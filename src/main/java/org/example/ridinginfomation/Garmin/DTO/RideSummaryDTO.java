package org.example.ridinginfomation.Garmin.DTO;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RideSummaryDTO {
    private Long id;
    private String filename;
    private LocalDateTime startTime;
    private Double totalDistance;
    private Integer totalCalories;
    private Integer totalAscent;
    private Integer totalTime;
    private Integer movingTime;
}
