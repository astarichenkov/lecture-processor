package com.lectureprocessor.chat.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.lectureprocessor.chat.ratelimit.RateLimitExceededException;
import com.lectureprocessor.chat.ratelimit.RateLimiter;
import com.lectureprocessor.chat.service.ChatService;

/**
 * Minimal HTMX chat UI.
 */
@Controller
public class ChatWebController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    public ChatWebController(ChatService chatService, RateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("sessionId", UUID.randomUUID().toString());
        model.addAttribute("model", chatService.modelName());
        return "index";
    }

    @PostMapping("/chat")
    public String ask(@RequestParam("session") String session, @RequestParam("message") String message,
            Model model) {
        model.addAttribute("sessionId", session);
        if (!rateLimiter.tryAcquire("ui")) {
            model.addAttribute("error", "Превышен лимит запросов. Попробуйте позже.");
            return "fragments/answer :: answer";
        }
        try {
            model.addAttribute("question", message);
            model.addAttribute("answer", chatService.ask(session, message));
        } catch (InvalidChatRequestException | RateLimitExceededException | ServiceBusyException e) {
            model.addAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            model.addAttribute("error", "Не удалось получить ответ: " + e.getMessage());
        }
        return "fragments/answer :: answer";
    }

    @PostMapping("/chat/clear")
    public String clear(@RequestParam("session") String session) {
        chatService.clear(session);
        return "redirect:/";
    }
}
