package com.example.shop.service.impl;

import com.example.shop.entity.AIConversation;
import com.example.shop.repository.AIConversationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AIConversationService {

    private final AIConversationRepository repo;

    public AIConversation getLastMessage(String sessionId) {
        return repo
                .findFirstBySessionIdOrderByUpdatedAtDesc(sessionId)
                .orElse(null);
    }


    public void update(
            AIConversation c,
            String question,
            String answer
    ) {
        c.setLastQuestion(question);
        c.setLastAnswer(answer);
        c.setUpdatedAt(LocalDateTime.now());
        repo.save(c);
    }
    public AIConversation createNew(String sessionId,String lastQuestion,String lastAnswer){
        AIConversation aiConversation=new AIConversation();
        aiConversation.setLastAnswer(lastAnswer);
        aiConversation.setLastQuestion(lastQuestion);
        aiConversation.setSessionId(sessionId);
        aiConversation.setUpdatedAt(LocalDateTime.now());
        return repo.save(aiConversation);
    }
}