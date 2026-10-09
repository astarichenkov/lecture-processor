package com.lectureprocessor.web;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lectureprocessor.benchmark.BenchmarkService;
import com.lectureprocessor.chat.LectureChatService;
import com.lectureprocessor.domain.Lecture;
import com.lectureprocessor.job.LectureJobService;
import com.lectureprocessor.lecture.InvalidUploadException;
import com.lectureprocessor.lecture.LectureService;

/**
 * Minimal Thymeleaf + HTMX UI: upload, monitor status, download results, chat
 * per lecture and view benchmark config.
 */
@Controller
public class LectureWebController {

    private final LectureService lectureService;
    private final LectureJobService jobService;
    private final LectureChatService chatService;
    private final BenchmarkService benchmarkService;

    public LectureWebController(LectureService lectureService, LectureJobService jobService,
            LectureChatService chatService, BenchmarkService benchmarkService) {
        this.lectureService = lectureService;
        this.jobService = jobService;
        this.chatService = chatService;
        this.benchmarkService = benchmarkService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("lectures", lectureService.findAll());
        return "index";
    }

    @PostMapping("/lectures")
    public String upload(@RequestParam("file") MultipartFile file,
            @RequestParam(name = "clean", defaultValue = "false") boolean clean,
            @RequestParam(name = "slides", defaultValue = "false") boolean slides,
            RedirectAttributes redirect) {
        try {
            Lecture lecture = lectureService.create(file, clean, slides);
            jobService.submit(lecture.id());
            redirect.addFlashAttribute("message", "Лекция загружена, обработка запущена");
            return "redirect:/lectures/" + lecture.id();
        } catch (InvalidUploadException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/lectures/{id}")
    public String lecture(@PathVariable String id, Model model) {
        model.addAttribute("lecture", lectureService.getRequired(id));
        return "lecture";
    }

    @PostMapping("/lectures/{id}/process")
    public String process(@PathVariable String id) {
        lectureService.getRequired(id);
        jobService.submit(id);
        return "redirect:/lectures/" + id;
    }

    @PostMapping("/lectures/{id}/delete")
    public String delete(@PathVariable String id) {
        lectureService.delete(id);
        return "redirect:/";
    }

    @GetMapping("/lectures/{id}/status")
    public String status(@PathVariable String id, Model model) {
        model.addAttribute("lecture", lectureService.getRequired(id));
        return "fragments/status :: status";
    }

    @GetMapping("/lectures/{id}/chat")
    public String chat(@PathVariable String id, Model model) {
        Lecture lecture = lectureService.getRequired(id);
        String sessionId = UUID.randomUUID().toString();
        model.addAttribute("lecture", lecture);
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("history", List.of());
        return "chat";
    }

    @PostMapping("/lectures/{id}/chat")
    public String ask(@PathVariable String id, @RequestParam("session") String session,
            @RequestParam("question") String question, Model model) {
        model.addAttribute("lecture", lectureService.getRequired(id));
        model.addAttribute("sessionId", session);
        model.addAttribute("answer", chatService.ask(id, session, question));
        return "fragments/chat :: answer";
    }

    @PostMapping("/lectures/{id}/chat/clear")
    public String clearChat(@PathVariable String id, @RequestParam("session") String session) {
        chatService.clear(id, session);
        return "redirect:/lectures/" + id + "/chat";
    }

    @GetMapping("/benchmarks")
    public String benchmarks(Model model) {
        model.addAttribute("config", benchmarkService.loadDefaultConfig());
        return "benchmarks";
    }
}
