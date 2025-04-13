package org.example.ridinginfomation.Garmin.VO;

import lombok.Getter;
import lombok.Setter;
import org.example.ridinginfomation.fit.LocalDateTime;

// ✅ ActivityPointVO.java

@Setter
@Getter
public class ActivityPointVO {
    private double latitude;
    private double longitude;
    private double altitude;
    private LocalDateTime timestamp;
    private double distance;

    public ActivityPointVO(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public ActivityPointVO(double latitude, double longitude, double altitude, LocalDateTime timestamp, double distance) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.distance = distance;
    }
}

