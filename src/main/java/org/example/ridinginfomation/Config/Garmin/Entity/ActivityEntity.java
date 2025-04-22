package org.example.ridinginfomation.Config.Garmin.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "activity")
@Getter
@Setter
public class ActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String activityDate;
    private String activityName;
    private String content;
    private String visibility; // ex: public, private
    private String device;     // Garmin, Strava 등

    @ManyToOne
    @JoinColumn(name = "activity_core_id")
    private ActivityCoreEntity activityCore;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityImageEntity> images;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityCommentEntity> comments;
}
