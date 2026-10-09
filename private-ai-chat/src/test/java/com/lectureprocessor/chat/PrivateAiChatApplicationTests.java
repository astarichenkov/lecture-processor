package com.lectureprocessor.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.lectureprocessor.chat.service.ChatService;

@SpringBootTest
class PrivateAiChatApplicationTests {

    @Autowired
    private ChatService chatService;

    @Test
    void contextLoadsAndModelConfigured() {
        assertThat(chatService.modelName()).isEqualTo("qwen3:1.7b");
    }
}
