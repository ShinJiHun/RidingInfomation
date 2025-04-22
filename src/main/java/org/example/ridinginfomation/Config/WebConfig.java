package org.example.ridinginfomation.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/")
                .setViewName("forward:/index.html");

        registry.addViewController("/{spring:[a-zA-Z0-9\\-]+}")
                .setViewName("forward:/index.html");

        registry.addViewController("/**/{spring:[a-zA-Z0-9\\-]+}")
                .setViewName("forward:/index.html");

        registry.addViewController("/{spring:[a-zA-Z0-9\\-]+}/**{spring:?!(\\.js|\\.css|\\.png|\\.jpg|\\.svg)$}")
                .setViewName("forward:/index.html");
    }
}
