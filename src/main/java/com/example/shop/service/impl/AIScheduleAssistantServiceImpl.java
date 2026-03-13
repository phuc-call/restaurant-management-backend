package com.example.shop.service.impl;


import com.example.shop.config.AppConstants;
import com.example.shop.entity.AIConversation;

import com.example.shop.exception.APIException;
import com.example.shop.hellper.AISchedule;
import com.example.shop.payloads.AIScheduleQuestionRequest;
import com.example.shop.payloads.reponse.ChatMessageDTO;
import com.example.shop.payloads.reponse.ChatSessionDetailDTO;
import com.example.shop.payloads.reponse.EmployeeScheduleResponse;
import com.example.shop.repository.AIConversationRepository;
import com.example.shop.service.WorkShiftService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;


@Service
@RequiredArgsConstructor
public class AIScheduleAssistantServiceImpl {

    private final WorkShiftService workShiftService;
    private final OpenAIClient openAIClient;
    private final AIConversationService memoryService;
    @Autowired
    AIConversationRepository aiConversationRepository;

    private String generateSessionIdFromQuestion(String question) {
        String prompt = """
                Hãy tóm tắt câu hỏi sau thành một tiêu đề ngắn (3–6 từ),
                không dấu câu, không xuống dòng:
                
                %s
                """.formatted(question);
        String title = openAIClient.ask(prompt);
        return title == null
                ? "Hoi lich lam viec"
                : title.replaceAll("[\\r\\n]", "").trim();
    }

    private String buildPrompt(
            String lastQ,
            String lastA,
            AIScheduleQuestionRequest request
    ) {

        EmployeeScheduleResponse hasSchedule =
                workShiftService.getEmployeesByScheduleStatus(
                        request.from(),
                        request.to(),
                        true,
                        0, 50,
                        AppConstants.SORT_WORK_SCHEDULE,
                        "asc",
                        null
                );

        EmployeeScheduleResponse noSchedule =
                workShiftService.getEmployeesByScheduleStatus(
                        request.from(),
                        request.to(),
                        false,
                        0, 50,
                        AppConstants.SORT_WORK_SCHEDULE,
                        "asc",
                        null
                );

        String context = AISchedule.buildContext(hasSchedule, noSchedule);
        return """
                Bạn là trợ lý AI cho quản lý nhân sự.
                Trả lời bằng tiếng Việt, rõ ràng, ngắn gọn.
                
                YÊU CẦU TRÌNH BÀY:
                - Viết tiếng Việt có dấu
                - Giữ nguyên các dòng xuống hàng trong dữ liệu
                - Không gộp các dòng lại
                - Không in dữ liệu thô
                
                Câu hỏi trước: %s
                Trả lời trước: %s
                
                %s
                
                Câu hỏi hiện tại:
                %s
                """.formatted(lastQ, lastA, context, request.question());

    }


    public void askStream(
            String sessionId,
            AIScheduleQuestionRequest request,
            Consumer<String> onToken
    ) {

        final String finalSessionId =
                (sessionId == null || sessionId.isBlank())
                        ? UUID.randomUUID().toString()
                        : sessionId;

        AIConversation last = memoryService.getLastMessage(finalSessionId);

        String lastQ = last != null ? last.getLastQuestion() : "";
        String lastA = last != null ? last.getLastAnswer() : "";

        String prompt = buildPrompt(lastQ, lastA, request);

        StringBuilder fullAnswer = new StringBuilder();
        StringBuilder streamBuffer = new StringBuilder();
        long[] lastFlush = {System.currentTimeMillis()};

        openAIClient.stream(
                prompt,
                token -> {
                    fullAnswer.append(token);
                    streamBuffer.append(token);

                    long now = System.currentTimeMillis();
                    if (now - lastFlush[0] >= 40) {
                        onToken.accept(streamBuffer.toString());
                        streamBuffer.setLength(0);
                        lastFlush[0] = now;
                    }
                },
                () -> {
                    // flush lần cuối
                    if (streamBuffer.length() > 0) {
                        onToken.accept(streamBuffer.toString());
                    }

                    memoryService.createNew(
                            finalSessionId,
                            request.question(),
                            fullAnswer.toString()
                    );
                },
                e -> onToken.accept("[ERROR]")
        );
    }


    public ChatSessionDetailDTO getSessionDetail(String sessionId) {
        List<AIConversation> aiConversations = aiConversationRepository.getChatBySession(sessionId);
        if (aiConversations.isEmpty()) {
            throw new APIException("Hãy đặt câu hỏi");
        }
        List<ChatMessageDTO> chatMessageDTOS = aiConversations.stream().map(a ->
                new ChatMessageDTO(
                        a.getLastQuestion(),
                        a.getLastAnswer(),
                        a.getUpdatedAt()
                )
        ).toList();
        ChatSessionDetailDTO dto = new ChatSessionDetailDTO();
        dto.setSessionId(sessionId);
        dto.setChatMessageDTOS(chatMessageDTOS);
        return dto;
    }

    public List<ChatMessageDTO> getSession() {
        return aiConversationRepository.getChatSidebar().stream().map(row -> new ChatMessageDTO(
                (String) row[0],
                (String) row[1],
                (LocalDateTime) row[2]
        )).toList();
    }

    public String askOnce(
            String sessionId,
            AIScheduleQuestionRequest request
    ) {
        String finalSessionId =
                (sessionId == null || sessionId.isBlank())
                        ? UUID.randomUUID().toString()
                        : sessionId;

        AIConversation last = memoryService.getLastMessage(finalSessionId);

        String lastQ = last != null ? last.getLastQuestion() : "";
        String lastA = last != null ? last.getLastAnswer() : "";

        String prompt = buildPrompt(lastQ, lastA, request);

        String answer = openAIClient.ask(prompt);

        memoryService.createNew(
                finalSessionId,
                request.question(),
                answer
        );

        return answer;
    }
}



