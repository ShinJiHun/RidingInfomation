package org.example.ridinginfomation.Garmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;


@SpringBootApplication
public class FitReaderApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(FitReaderApplication.class)
                .properties(
                    "spring.config.name=application",
                    "server.port=8085" // ✅ 여기서 강제 지정
                )
                .run(args);
    }
}