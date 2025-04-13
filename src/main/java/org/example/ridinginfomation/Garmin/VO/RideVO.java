package org.example.ridinginfomation.Garmin.VO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// ✅ RideVO.java

@Getter
@Setter
public class RideVO {
    private ActivityCoreVO activityCoreVO;
    private List<ActivityPointVO> route;
    private ActivityWeatherVO activityWeatherVO;
}
