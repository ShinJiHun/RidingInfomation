package org.example.ridinginfomation.Garmin.Repository;

import org.example.ridinginfomation.Garmin.Entity.ActivityCoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ActivityCoreRepository extends JpaRepository<ActivityCoreEntity, Long> {
    boolean existsByFilename(String filename);
    ActivityCoreEntity findByFilename(String filename);

    @Query("SELECT a.filename FROM ActivityCoreEntity a")
    List<String> findAllFilenames();

    @Query("SELECT c FROM ActivityCoreEntity c LEFT JOIN FETCH c.route")
    List<ActivityCoreEntity> findAllWithRoute();
}
