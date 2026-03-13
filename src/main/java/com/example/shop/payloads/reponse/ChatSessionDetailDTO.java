package com.example.shop.payloads.reponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChatSessionDetailDTO {
    private String sessionId;
    private List<ChatMessageDTO>chatMessageDTOS;
}
