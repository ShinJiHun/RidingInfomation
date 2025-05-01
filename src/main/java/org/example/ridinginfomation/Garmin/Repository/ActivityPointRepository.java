package org.example.ridinginfomation.Garmin.Repository;

import org.example.ridinginfomation.Garmin.Entity.ActivityPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityPointRepository extends JpaRepository<ActivityPointEntity, Long> {}
