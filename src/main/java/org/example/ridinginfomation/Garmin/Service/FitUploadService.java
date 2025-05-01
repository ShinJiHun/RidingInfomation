// ✅ FitUploadService.java
package org.example.ridinginfomation.Garmin.Service;

import org.example.ridinginfomation.Garmin.Util.FitReader;
import org.example.ridinginfomation.Garmin.Util.MapImageGenerator;
import org.springframework.stereotype.Service;

@Service
public class FitUploadService {

    private final FitReader fitReader;
    private final MapImageGenerator mapImageGenerator;

    public FitUploadService(FitReader fitReader, MapImageGenerator mapImageGenerator) {
        this.fitReader = fitReader;
        this.mapImageGenerator = mapImageGenerator;
    }

}
