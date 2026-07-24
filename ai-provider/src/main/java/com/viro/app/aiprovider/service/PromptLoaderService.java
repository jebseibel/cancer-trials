package com.viro.app.aiprovider.service;

import com.viro.common.domain.AiPrompt;
import com.viro.database.db.service.AiPromptDbService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptLoaderService {

    private final AiPromptDbService aiPromptDbService;

    public String getPrompt(String promptKey) {
        RetirementPrompt config = getPromptConfig(promptKey);
        return (config != null) ? config.getPrompt() : null;
    }

    public RetirementPrompt getPromptConfig(String promptKey) {
        try {
            AiPrompt prompt = aiPromptDbService.findByUniqueId(promptKey);
            log.info("AI prompt loaded — name: {}, description: {}", prompt.getName(), prompt.getDescription());
            return toRetirementPrompt(prompt);
        } catch (Exception e) {
            log.warn("Prompt not found for key: {}", promptKey);
            return null;
        }
    }

    public List<RetirementPrompt> getAllPrompts() {
        return aiPromptDbService.findAll().stream()
                .map(this::toRetirementPrompt)
                .toList();
    }

    public boolean isLoaded() {
        return !aiPromptDbService.findAll().isEmpty();
    }

    private RetirementPrompt toRetirementPrompt(AiPrompt prompt) {
        RetirementPrompt rp = new RetirementPrompt();
        rp.setTrackingSystem(prompt.getTrackingSystem());
        rp.setDocType(prompt.getUniqueId());
        rp.setProvider(prompt.getProvider());
        rp.setModel(prompt.getModel());
        rp.setVersion(prompt.getVersion());
        rp.setPrompt(assemblePrompt(prompt));
        return rp;
    }

    private String assemblePrompt(AiPrompt prompt) {
        StringBuilder sb = new StringBuilder();
        appendTagged(sb, null, prompt.getContent());
        appendTagged(sb, "result_format", prompt.getResultFormat());
        appendExample(sb, prompt.getExampleInput(), prompt.getExampleOutput());
        return sb.toString();
    }

    private void appendTagged(StringBuilder sb, String tag, String value) {
        if (value == null || value.isBlank()) return;
        if (!sb.isEmpty()) sb.append("\n\n");
        if (tag == null) {
            sb.append(value);
        } else {
            sb.append('<').append(tag).append(">\n")
              .append(value)
              .append("\n</").append(tag).append('>');
        }
    }

    private void appendExample(StringBuilder sb, String input, String output) {
        boolean hasInput = input != null && !input.isBlank();
        boolean hasOutput = output != null && !output.isBlank();
        if (!hasInput && !hasOutput) return;
        if (!sb.isEmpty()) sb.append("\n\n");
        sb.append("<example>\n");
        if (hasInput) {
            sb.append("  <input>\n").append(input).append("\n  </input>\n");
        }
        if (hasOutput) {
            sb.append("  <output>\n").append(output).append("\n  </output>\n");
        }
        sb.append("</example>");
    }

    @Data
    public static class RetirementPrompt {
        private String trackingSystem;
        private String docType;
        private String provider;
        private String model;
        private String version;
        private String prompt;
    }
}
