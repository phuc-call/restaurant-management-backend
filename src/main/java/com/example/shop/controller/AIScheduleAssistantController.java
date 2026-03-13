package com.example.shop.controller;

import com.example.shop.payloads.AIScheduleQuestionRequest;
import com.example.shop.payloads.reponse.ChatMessageDTO;
import com.example.shop.payloads.reponse.ChatSessionDetailDTO;
import com.example.shop.service.impl.AIScheduleAssistantServiceImpl;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api")
@SecurityRequirement(name = "E-commerce Application")
public class AIScheduleAssistantController {

    @Autowired
    private AIScheduleAssistantServiceImpl aiService;

    @PostMapping("/admin/ask/schedule")
    public ResponseEntity<String> askOnce(
            @RequestBody AIScheduleQuestionRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId
    ) {
        String answer = aiService.askOnce(sessionId, request);
        return ResponseEntity.ok(answer);
    }

    @GetMapping("/admin/chat/sessions/{sessionId}")
    public ResponseEntity<ChatSessionDetailDTO> getSchedule(
            @PathVariable String sessionId
    ) {
        return ResponseEntity.ok(
                aiService.getSessionDetail(sessionId)
        );
    }

    @GetMapping("/admin/chat/sessions")
    public ResponseEntity<List<ChatMessageDTO>> getSessions() {
        return ResponseEntity.ok(
                aiService.getSession()
        );
    }
}
