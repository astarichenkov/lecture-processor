package com.lectureprocessor.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.lectureprocessor.chat.config.ChatProperties;

@SpringBootApplication
@EnableConfigurationProperties(ChatProperties.class)
public class PrivateAiChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(PrivateAiChatApplication.class, args);
    }
}
