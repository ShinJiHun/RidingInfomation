package org.example.ridinginfomation.Garmin.DTO;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RideDetailDTO {
    private Long id;
    private String filename;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double totalDistance;
    private Integer totalCalories;
    private Integer movingTime;
    private Integer totalTime;
    private Integer totalAscent;

    private List<PointDTO> route;

    @Data
    public static class PointDTO {
        private double latitude;
        private double longitude;
        private Double altitude;
        private LocalDateTime timestamp;
        private Double distance;
    }
}
