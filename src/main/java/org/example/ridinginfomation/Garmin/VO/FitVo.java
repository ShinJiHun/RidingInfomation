package org.example.ridinginfomation.Garmin.VO;

import lombok.Getter;
import lombok.Setter;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Getter
@Setter
public class FitVo {
    private static final DecimalFormat df = new DecimalFormat("0.00");
    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E요일", Locale.KOREAN);
    private double distanceKm;
    private int calories;
    private int durationMinutes;
    private Date ridingDate;
    private float altitude; // ← 추가

    public FitVo(Date ridingDate) {
        this.ridingDate = ridingDate;
    }

    public FitVo(double distanceKm, int calories, int durationMinutes, Date ridingDate, float altitude) {
        this.distanceKm = distanceKm;
        this.calories = calories;
        this.durationMinutes = durationMinutes;
        this.ridingDate = ridingDate;
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E", Locale.KOREAN);
        String formattedDate = (ridingDate != null) ? sdf.format(ridingDate) : "";

        return String.format("| %-19s | %7.2f km | %6d kcal | %4d분 | %4.0f m |",
                formattedDate,
                distanceKm,
                calories,
                durationMinutes,
                altitude);
    }
}
