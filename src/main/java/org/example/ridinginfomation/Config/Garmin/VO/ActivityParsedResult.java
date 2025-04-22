package org.example.ridinginfomation.Config.Garmin.VO;

import java.util.List;

// ✅ ActivityParsedResult.java
public class ActivityParsedResult {
    private List<ActivityCoreVO> coreList;
    private List<ActivityPointVO> pointList;
    private ActivityWeatherVO weather;

    // Getters & Setters
    public List<ActivityCoreVO> getCoreList() { return coreList; }
    public void setCoreList(List<ActivityCoreVO> coreList) { this.coreList = coreList; }

    public List<ActivityPointVO> getPointList() { return pointList; }
    public void setPointList(List<ActivityPointVO> pointList) { this.pointList = pointList; }

    public ActivityWeatherVO getWeather() { return weather; }
    public void setWeather(ActivityWeatherVO weather) { this.weather = weather; }
}
