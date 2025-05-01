// 경로: org.example.ridinginfomation.Garmin.Mixin
package org.example.ridinginfomation.Garmin.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

public abstract class ActivityCoreEntityIgnoreRouteMixin {
    @JsonIgnore
    abstract List<ActivityPointEntity> getRoute();
}
