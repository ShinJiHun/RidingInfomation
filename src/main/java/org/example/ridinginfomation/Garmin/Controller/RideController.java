package org.example.ridinginfomation.Garmin.Controller;

import lombok.RequiredArgsConstructor;
import org.example.ridinginfomation.Garmin.DTO.RideDetailDTO;
import org.example.ridinginfomation.Garmin.DTO.RideSummaryDTO;
import org.example.ridinginfomation.Garmin.Entity.ActivityCoreEntity;
import org.example.ridinginfomation.Garmin.Repository.ActivityCoreRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rides")
@RequiredArgsConstructor
public class RideController {

    private final ActivityCoreRepository coreRepo;

    // 전체 목록
    @GetMapping
    public List<RideSummaryDTO> getAllRides() {
        return coreRepo.findAll().stream().map(entity -> {
            RideSummaryDTO dto = new RideSummaryDTO();
            dto.setId(entity.getId());
            dto.setFilename(entity.getFilename());
            dto.setStartTime(entity.getStartTime());
            dto.setTotalDistance(entity.getTotalDistance());
            dto.setTotalCalories(entity.getTotalCalories());
            dto.setTotalAscent(entity.getTotalAscent());
            dto.setMovingTime(entity.getMovingTime());
            dto.setTotalTime(entity.getTotalTime());
            return dto;
        }).collect(Collectors.toList());
    }

    // 상세 조회
    @GetMapping("/{id}")
    public RideDetailDTO getRide(@PathVariable Long id) {
        ActivityCoreEntity entity = coreRepo.findById(id).orElseThrow();

        RideDetailDTO dto = new RideDetailDTO();
        dto.setId(entity.getId());
        dto.setFilename(entity.getFilename());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setTotalDistance(entity.getTotalDistance());
        dto.setTotalCalories(entity.getTotalCalories());
        dto.setMovingTime(entity.getMovingTime());
        dto.setTotalTime(entity.getTotalTime());
        dto.setTotalAscent(entity.getTotalAscent());

        dto.setRoute(entity.getRoute().stream().map(p -> {
            RideDetailDTO.PointDTO point = new RideDetailDTO.PointDTO();
            point.setLatitude(p.getLatitude());
            point.setLongitude(p.getLongitude());
            point.setAltitude(p.getAltitude());
            point.setTimestamp(p.getTimestamp());
            point.setDistance(p.getDistance());
            return point;
        }).collect(Collectors.toList()));

        return dto;
    }
}
