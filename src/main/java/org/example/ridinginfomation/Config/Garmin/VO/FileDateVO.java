package org.example.ridinginfomation.Config.Garmin.VO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// ✅ FileDateVO.java

@Setter
@Getter
public class FileDateVO {
    private String filename;
    private LocalDate fileDate;
}
