package com.example.shop.service.impl;

import com.example.shop.config.OpenAIProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Consumer;


@Service
@RequiredArgsConstructor
public class OpenAIClient {

    private final OkHttpClient client;
    private final OpenAIProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String ask(String prompt) {

        if (properties.getApiUrl() == null) {
            throw new IllegalStateException("OpenAI apiUrl is NULL");
        }

        try {

            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", properties.getModel());

            ArrayNode inputArray = objectMapper.createArrayNode();
            ObjectNode message = objectMapper.createObjectNode();
            message.put("role", "user");

            ArrayNode contentArray = objectMapper.createArrayNode();
            ObjectNode textNode = objectMapper.createObjectNode();
            textNode.put("type", "input_text");
            textNode.put("text", prompt);

            contentArray.add(textNode);
            message.set("content", contentArray);
            inputArray.add(message);

            root.set("input", inputArray);

            RequestBody body = RequestBody.create(objectMapper.writeValueAsString(root), MediaType.parse("application/json"));

            Request request = new Request.Builder().url(properties.getApiUrl()).addHeader("Authorization", "Bearer " + properties.getApiKey()).addHeader("Content-Type", "application/json").post(body).build();

            try (Response response = client.newCall(request).execute()) {

                String responseBody = response.body().string();

                if (!response.isSuccessful()) {
                    throw new RuntimeException("OpenAI API error: " + responseBody);
                }

                JsonNode json = objectMapper.readTree(responseBody);


                JsonNode output = json.path("output");

                if (output.isArray() && !output.isEmpty()) {
                    JsonNode content = output.get(0).path("content");
                    if (content.isArray() && !output.isEmpty()) {
                        return content.get(0).path("text").asText();
                    }
                }

                return null;

            }

        } catch (Exception e) {
            throw new RuntimeException("AI error", e);
        }
    }

    public void stream(
            String prompt,
            Consumer<String> onToken,
            Runnable onComplete,
            Consumer<Throwable> onError
    ) {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", properties.getModel());
            root.put("stream", true);

            ArrayNode input = objectMapper.createArrayNode();
            ObjectNode msg = objectMapper.createObjectNode();
            msg.put("role", "user");

            ArrayNode content = objectMapper.createArrayNode();
            ObjectNode text = objectMapper.createObjectNode();
            text.put("type", "input_text");
            text.put("text", prompt);

            content.add(text);
            msg.set("content", content);
            input.add(msg);
            root.set("input", input);

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/responses")
                    .post(RequestBody.create(
                            objectMapper.writeValueAsBytes(root),
                            MediaType.parse("application/json")
                    ))
                    .addHeader("Authorization", "Bearer " + properties.getApiKey())
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    onError.accept(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try (BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(response.body().byteStream()))) {

                        String line;
                        while ((line = reader.readLine()) != null) {

                            if (!line.startsWith("data:")) continue;

                            String payload = line.substring(5).trim();
                            if (payload.equals("[DONE]")) break;

                            JsonNode json = objectMapper.readTree(payload);

                            if ("response.output_text.delta"
                                    .equals(json.path("type").asText())) {

                                String delta = json.path("delta").asText();
                                onToken.accept(delta); // ❗ RAW TOKEN
                            }

                            if ("response.completed"
                                    .equals(json.path("type").asText())) {
                                onComplete.run();
                            }
                        }

                    } catch (Exception e) {
                        onError.accept(e);
                    }
                }
            });

        } catch (Exception e) {
            onError.accept(e);
        }
    }
}
