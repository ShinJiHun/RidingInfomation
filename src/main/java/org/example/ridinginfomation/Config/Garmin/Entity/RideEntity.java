package org.example.ridinginfomation.Config.Garmin.Entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// 이건 통합 응답용으로 유지 가능 (비영속 DTO로)
@Getter
@Setter
public class RideEntity {
    private ActivityCoreEntity activityCore;
    private List<ActivityPointEntity> route;
    private ActivityWeatherEntity activityWeather;
}
