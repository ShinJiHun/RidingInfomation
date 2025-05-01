package org.example.ridinginfomation.Garmin.Entity;

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

    private String filename;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private Double totalDistance;
    private Integer totalCalories;
    private Integer movingTime;
    private Integer totalTime;
    private Integer totalAscent;

    @Column(columnDefinition = "TEXT")
    private String polyline;

    @Column(name = "last_modified")
    private LocalDateTime lastModifiedTime;

    @OneToMany(mappedBy = "core", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityPointEntity> route;
}
