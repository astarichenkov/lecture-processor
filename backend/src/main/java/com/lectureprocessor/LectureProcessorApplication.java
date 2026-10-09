package com.lectureprocessor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lectureprocessor.config.AppProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class LectureProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LectureProcessorApplication.class, args);
    }
}
