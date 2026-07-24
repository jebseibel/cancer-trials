package com.seibel.cancer.aiprovider.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.seibel.cancer.aiprovider.config.AiConfigProperties;
import com.seibel.cancer.aiprovider.dto.OpenRouterModel;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OpenRouterModelService {

    private static final String MODELS_URL = "https://openrouter.ai/api/v1/models";

    private final AiConfigProperties aiConfigProperties;
    private final RestClient restClient;

    private List<OpenRouterModel> cachedModels = Collections.emptyList();
    private Map<String, OpenRouterModel> modelIndex = Collections.emptyMap();

    public OpenRouterModelService(AiConfigProperties aiConfigProperties) {
        this.aiConfigProperties = aiConfigProperties;
        this.restClient = RestClient.create();
    }

    @PostConstruct
    public void init() {
        var openrouter = aiConfigProperties.getOpenrouter();
        if (openrouter == null || !openrouter.isEnabled()) {
            log.info("OpenRouter not configured — skipping model cache load.");
            return;
        }
        try {
            cachedModels = fetchModels();
            modelIndex = cachedModels.stream()
                    .collect(Collectors.toMap(OpenRouterModel::id, Function.identity(), (a, b) -> a));
            log.info("OpenRouter model cache loaded: {} models", cachedModels.size());
        } catch (Exception e) {
            log.warn("Failed to load OpenRouter model cache at startup: {}", e.getMessage());
        }
    }

    public List<OpenRouterModel> getModels() {
        return cachedModels;
    }

    public List<OpenRouterModel> getVisionModels() {
        return cachedModels.stream()
                .filter(OpenRouterModel::supportsVision)
                .toList();
    }

    public Optional<OpenRouterModel> findByModelId(String modelId) {
        if (modelId == null) return Optional.empty();
        // Try exact match first
        if (modelIndex.containsKey(modelId)) return Optional.of(modelIndex.get(modelId));
        // Fuzzy match: strip provider prefix, normalize dots/dashes
        String normalized = normalize(modelId);
        return cachedModels.stream()
                .filter(m -> normalize(stripPrefix(m.id())).equals(normalized))
                .findFirst();
    }

    private String stripPrefix(String id) {
        int slash = id.indexOf('/');
        return slash >= 0 ? id.substring(slash + 1) : id;
    }

    private String normalize(String id) {
        return id.toLowerCase().replace('.', '-');
    }

    private List<OpenRouterModel> fetchModels() {
        ModelsResponse response = restClient.get()
                .uri(MODELS_URL)
                .retrieve()
                .body(ModelsResponse.class);

        if (response == null || response.data() == null) {
            return Collections.emptyList();
        }

        return response.data().stream()
                .map(raw -> new OpenRouterModel(
                        raw.id(),
                        raw.name(),
                        raw.description(),
                        raw.contextLength(),
                        raw.pricing() != null
                                ? new OpenRouterModel.Pricing(raw.pricing().prompt(), raw.pricing().completion())
                                : null,
                        raw.architecture() != null
                                && raw.architecture().modality() != null
                                && raw.architecture().modality().contains("image"),
                        raw.architecture() != null && raw.architecture().inputModalities() != null
                                ? raw.architecture().inputModalities()
                                : Collections.emptyList()))
                .toList();
    }

    private record ModelsResponse(List<RawModel> data) {}

    private record RawModel(
            String id,
            String name,
            String description,
            @JsonProperty("context_length") int contextLength,
            RawPricing pricing,
            RawArchitecture architecture
    ) {}

    private record RawPricing(
            String prompt,
            String completion
    ) {}

    private record RawArchitecture(
            String modality,
            @JsonProperty("input_modalities") List<String> inputModalities
    ) {}
}
