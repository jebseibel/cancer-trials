package com.seibel.cancer.rag.chunk;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Not an assertion test - prints real chunk output so a human can eyeball whether the
 * chunks are actually good, which passing assertions does not prove. Temporary; delete
 * once the chunking strategy is settled.
 */
class ChunkOutputInspection {

    @Test
    void printChunks() throws Exception {
        var chunker = new EligibilityCriteriaChunker();
        for (String name : List.of("nested-numbered-NCT04942054", "clean-baseline-NCT06649565",
                "prose-NCT05076266", "longest-nested-NCT04244552")) {
            String raw;
            try (var in = getClass().getResourceAsStream("/eligibility/" + name + ".txt")) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            List<EligibilityChunk> chunks = chunker.chunk(raw);
            System.out.println("\n######## " + name + " (" + raw.length() + " chars -> "
                    + chunks.size() + " chunks)");
            int shown = 0;
            for (EligibilityChunk c : chunks) {
                // Always show the ANC family - it is the parent-suppression case under review.
                boolean interesting = c.text().toLowerCase().contains("neutrophil");
                if (shown++ >= 7 && !interesting) continue;
                String t = c.text().length() > 230 ? c.text().substring(0, 230) + "..." : c.text();
                System.out.println("  [" + c.type() + " #" + c.ordinal() + "] " + t);
            }
        }
    }
}
