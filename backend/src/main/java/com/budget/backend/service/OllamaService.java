package com.budget.backend.service;

import com.budget.backend.dto.OllamaRequest;
import com.budget.backend.dto.OllamaResponse;
import com.budget.backend.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class OllamaService {

    @Autowired
    private WebClient ollamaWebClient;

    @Value("${ollama.model:gemma2:2b}")
    private String model;

    @Value("${ollama.timeout:60}")
    private Long timeout;

    public String generateCompletion(String systemPrompt, String userPrompt, Double temperature, Integer maxTokens) {
        try {
            OllamaRequest request = new OllamaRequest();
            request.setModel(model);
            request.setPrompt(userPrompt);
            request.setSystemPrompt(systemPrompt);
            request.setStream(false);
            
            OllamaRequest.Options options = new OllamaRequest.Options();
            options.setTemperature(temperature != null ? temperature : 0.7);
            options.setNumPredict(maxTokens != null ? maxTokens : 1000);
            request.setOptions(options);

            OllamaResponse response = ollamaWebClient.post()
                    .uri("/api/generate")
                    .body(Mono.just(request), OllamaRequest.class)
                    .retrieve()
                    .bodyToMono(OllamaResponse.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            if (response != null && response.getResponse() != null) {
                return response.getResponse();
            }

            throw new BadRequestException("No response from Ollama");

        } catch (Exception e) {
            System.err.println("Ollama Error: " + e.getMessage());
            throw new BadRequestException("Failed to get Ollama response: " + e.getMessage());
        }
    }

    public boolean isAvailable() {
        try {
            ollamaWebClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}