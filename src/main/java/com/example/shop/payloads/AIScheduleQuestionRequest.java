package com.example.shop.payloads;

import java.time.LocalDate;

public record AIScheduleQuestionRequest(
        String question,
        LocalDate from,
        LocalDate to
) {}
