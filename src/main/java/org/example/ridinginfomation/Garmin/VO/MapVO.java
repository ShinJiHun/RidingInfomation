package org.example.ridinginfomation.Garmin.VO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.ArrayList;


@Getter
@Setter
public class MapVO {

    private List<Double> latitudes = new ArrayList<>();
    private List<Double> longitudes = new ArrayList<>();

    public MapVO() {

    }

    public MapVO(List<Double> latitudes, List<Double> longitudes) {

    }

    public void addPoint(double lat, double lon) {
        latitudes.add(lat);
        longitudes.add(lon);
    }
}
