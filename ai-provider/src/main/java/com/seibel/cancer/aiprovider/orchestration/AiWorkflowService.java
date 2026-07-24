package com.seibel.cancer.aiprovider.orchestration;

import com.seibel.cancer.aiprovider.config.AiUiProperties;
import com.seibel.cancer.aiprovider.orchestration.model.CombinedAnalysisResult;
import com.seibel.cancer.aiprovider.orchestration.model.ContentType;
import com.seibel.cancer.aiprovider.orchestration.model.ConversationResult;
import com.seibel.cancer.aiprovider.orchestration.model.DocumentAnalysis;
import com.seibel.cancer.aiprovider.service.AiService;
import org.springframework.stereotype.Service;

/**
 * Orchestration layer for AI services - Industry Standard Pattern
 * Coordinates between text and vision AI capabilities and applies business logic.
 *
 * Uses the simplified AiService for all AI operations.
 */
@Service
public class AiWorkflowService {

    private final AiService aiService;
    private final AiUiProperties uiProperties;

    public AiWorkflowService(AiService aiService, AiUiProperties uiProperties) {
        this.aiService = aiService;
        this.uiProperties = uiProperties;
    }

    /**
     * Analyze content that could be text or image
     */
    public String analyzeContent(String content, ContentType contentType) {
        return switch (contentType) {
            case TEXT -> aiService.chat(content);
            case IMAGE_URL -> aiService.analyzeImageUrl(content,
                uiProperties.getPrompts().getDefaultImageDescription(), "openai");
            case IMAGE_PATH -> throw new UnsupportedOperationException(
                "IMAGE_PATH not yet implemented in simplified service");
        };
    }

    /**
     * Smart analysis - detects if input is URL, text, etc.
     */
    public String smartAnalyze(String input, String question) {
        if (isImageUrl(input)) {
            return aiService.analyzeImageUrl(input, question, "openai");
        } else {
            String prompt = String.format("%s Context: %s", question, input);
            return aiService.chat(prompt);
        }
    }

    /**
     * Combined text and vision analysis
     * Example: "Describe this image and explain the technical details"
     */
    public CombinedAnalysisResult analyzeImageWithContext(
            String imageUrl,
            String contextPrompt,
            String technicalQuestion) {

        // First, get vision analysis
        String visionDescription = aiService.analyzeImageUrl(imageUrl, contextPrompt, "openai");

        // Then, use text service to add technical context
        String technicalAnalysis = aiService.chatWithContext(
                uiProperties.getPrompts().getVisionContextPrefix(),
                String.format("Image description: %s\n\nQuestion: %s", visionDescription, technicalQuestion),
                "openai"
        );

        return new CombinedAnalysisResult(visionDescription, technicalAnalysis);
    }

    /**
     * Multi-step reasoning: analyze image, then ask follow-up questions
     */
    public ConversationResult analyzeWithFollowUp(String imageUrl) {
        // Step 1: Get initial description
        String initialDescription = aiService.analyzeImageUrl(
            imageUrl,
            uiProperties.getPrompts().getDescribeImageDefault(),
            "openai"
        );

        // Step 2: Ask follow-up based on description
        String followUp = aiService.chat(
                String.format("Based on this description: '%s', what are 3 interesting facts or insights?",
                        initialDescription)
        );

        return new ConversationResult(initialDescription, followUp);
    }

    /**
     * Compare images and provide structured analysis
     */
    public String compareAndAnalyze(String[] imageUrls, String analysisType) {
        // Note: Multiple image comparison would need to be implemented in AiService
        // For now, we'll analyze them sequentially and combine results
        StringBuilder comparison = new StringBuilder();
        for (int i = 0; i < imageUrls.length; i++) {
            String analysis = aiService.analyzeImageUrl(
                imageUrls[i],
                uiProperties.getPrompts().getCompareImagesDefault() + " (Image " + (i+1) + ")",
                "openai"
            );
            comparison.append("Image ").append(i+1).append(": ").append(analysis).append("\n\n");
        }

        // Apply specific analysis based on type
        String detailedAnalysis = switch (analysisType) {
            case "technical" -> aiService.chatWithContext(
                    "You are a technical analyst",
                    "Based on this comparison: " + comparison + ", provide technical insights",
                    "openai"
            );
            case "business" -> aiService.chatWithContext(
                    "You are a business analyst",
                    "Based on this comparison: " + comparison + ", provide business implications",
                    "openai"
            );
            default -> comparison.toString();
        };

        return detailedAnalysis;
    }

    /**
     * Process document: OCR + text analysis
     */
    public DocumentAnalysis analyzeDocument(String documentImageUrl) {
        // Extract text via vision
        String extractedText = aiService.analyzeImageUrl(
                documentImageUrl,
                uiProperties.getPrompts().getOcr().getExtraction(),
                "anthropic"  // Use Anthropic for document analysis
        );

        // Summarize extracted text
        String summary = aiService.chat(
                String.format(uiProperties.getPrompts().getOcr().getSummary(), extractedText)
        );

        // Extract key information
        String keyInfo = aiService.chat(
                String.format(uiProperties.getPrompts().getOcr().getKeyInfo(), extractedText)
        );

        return new DocumentAnalysis(extractedText, summary, keyInfo);
    }

    /**
     * Simple text question
     */
    public String askQuestion(String question) {
        return aiService.chat(question);
    }

    /**
     * Analyze image
     */
    public String analyzeImage(String imageUrl, String question) {
        return aiService.analyzeImageUrl(imageUrl, question, "openai");
    }

    // Helper method
    private boolean isImageUrl(String input) {
        if (input == null) {
            return false;
        }
        String lowerInput = input.toLowerCase();
        return (lowerInput.startsWith("http://") || lowerInput.startsWith("https://")) &&
                (lowerInput.endsWith(".jpg") ||
                        lowerInput.endsWith(".jpeg") ||
                        lowerInput.endsWith(".png") ||
                        lowerInput.endsWith(".gif") ||
                        lowerInput.endsWith(".webp"));
    }
}
