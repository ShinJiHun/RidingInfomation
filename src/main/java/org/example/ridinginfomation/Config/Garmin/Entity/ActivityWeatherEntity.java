package org.example.ridinginfomation.Config.Garmin.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "activity_weather")
@Getter
@Setter
public class ActivityWeatherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double temperature;
    private double humidity;
    private double windSpeed;
    private String condition;

    @OneToOne
    @JoinColumn(name = "activity_core_id", nullable = false)
    private ActivityCoreEntity activityCore;
}
