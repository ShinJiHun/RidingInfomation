package org.example.ridinginfomation.Config.Garmin.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_point")
@Getter
@Setter
public class ActivityPointEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double latitude;
    private double longitude;
    private double altitude;
    private LocalDateTime timestamp;
    private double distance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_core_id")
    private ActivityCoreEntity activityCore;
}
