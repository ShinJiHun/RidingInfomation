package org.example.ridinginfomation.Config.Garmin.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "activity_core")
@Getter
@Setter
public class ActivityCoreEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String filename;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double totalDistance;
    private int totalCalories;
    private int totalTime;
    private int totalAscent;

    @OneToMany(mappedBy = "activityCore", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityPointEntity> route;

    @OneToOne(mappedBy = "activityCore", cascade = CascadeType.ALL, orphanRemoval = true)
    private ActivityWeatherEntity activityWeather;
}
