package com.viro.app.aiprovider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "viro.ai")
public class AiUiProperties {

    private Prompts prompts = new Prompts();
    private Defaults defaults = new Defaults();

    public Prompts getPrompts() {
        return prompts;
    }

    public void setPrompts(Prompts prompts) {
        this.prompts = prompts;
    }

    public Defaults getDefaults() {
        return defaults;
    }

    public void setDefaults(Defaults defaults) {
        this.defaults = defaults;
    }

    public static class Prompts {
        private String defaultImageDescription = "Describe this image";
        private String visionContextPrefix = "You are a technical expert. Based on this image description, provide detailed analysis.";
        private String describeImageDefault = "What's in this image? Describe it in detail.";
        private String compareImagesDefault = "Compare these images in detail";
        private Ocr ocr = new Ocr();

        public String getDefaultImageDescription() { return defaultImageDescription; }
        public void setDefaultImageDescription(String v) { this.defaultImageDescription = v; }
        public String getVisionContextPrefix() { return visionContextPrefix; }
        public void setVisionContextPrefix(String v) { this.visionContextPrefix = v; }
        public String getDescribeImageDefault() { return describeImageDefault; }
        public void setDescribeImageDefault(String v) { this.describeImageDefault = v; }
        public String getCompareImagesDefault() { return compareImagesDefault; }
        public void setCompareImagesDefault(String v) { this.compareImagesDefault = v; }
        public Ocr getOcr() { return ocr; }
        public void setOcr(Ocr ocr) { this.ocr = ocr; }

        public static class Ocr {
            private String extraction = "Extract all text from this document exactly as written";
            private String summary = "Summarize the following text in 3 bullet points: %s";
            private String keyInfo = "From this text, extract: dates, names, amounts, and key decisions: %s";

            public String getExtraction() { return extraction; }
            public void setExtraction(String extraction) { this.extraction = extraction; }
            public String getSummary() { return summary; }
            public void setSummary(String summary) { this.summary = summary; }
            public String getKeyInfo() { return keyInfo; }
            public void setKeyInfo(String keyInfo) { this.keyInfo = keyInfo; }
        }
    }

    public static class Defaults {
        private String imageMime = "image/jpeg";

        public String getImageMime() { return imageMime; }
        public void setImageMime(String imageMime) { this.imageMime = imageMime; }
    }
}
