package com.seibel.cancer.datafetcher.normalization;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalTrialsGovParserTest {

    private final ClinicalTrialsGovParser parser = new ClinicalTrialsGovParser();

    @Test
    void parse_shouldMapTrialFieldsFromSamplePayload() throws IOException {
        NormalizedTrial normalized = parser.parse(readFixture());

        assertTrue(parser.supports("CLINICALTRIALS_GOV"));
        assertFalse(parser.supports("SOME_OTHER_SOURCE"));

        var trial = normalized.getTrial();
        assertEquals("NCT01234567", trial.getNctId());
        assertEquals("A Study of Test Drug in Advanced Cancer", trial.getBriefTitle());
        assertEquals("A Phase 2 Study of Test Drug in Participants With Advanced Solid Tumors", trial.getOfficialTitle());
        assertEquals("RECRUITING", trial.getOverallStatus());
        assertEquals("INTERVENTIONAL", trial.getStudyType());
        assertEquals("This study evaluates Test Drug in patients with advanced solid tumors.", trial.getBriefSummary());
        assertEquals(LocalDate.of(2024, 1, 15), trial.getStartDate());
        assertEquals(LocalDate.of(2025, 6, 1), trial.getPrimaryCompletionDate());
        assertEquals(LocalDate.of(2025, 12, 31), trial.getCompletionDate());
        assertEquals(120, trial.getEnrollmentCount());
        assertEquals("ESTIMATED", trial.getEnrollmentType());
        assertEquals(false, trial.getHealthyVolunteers());
        assertEquals("ALL", trial.getSex());
        assertEquals("18 Years", trial.getMinimumAge());
        assertEquals("N/A", trial.getMaximumAge());
        assertTrue(trial.getEligibilityCriteria().contains("Inclusion Criteria"));
        assertNull(trial.getPrimaryTrialSourceId(), "primaryTrialSourceId is resolved later, not by the parser");
    }

    @Test
    void parse_shouldMapChildRecords() throws IOException {
        NormalizedTrial normalized = parser.parse(readFixture());

        assertEquals(1, normalized.getArmGroups().size());
        assertEquals("Test Drug Arm", normalized.getArmGroups().get(0).getLabel());

        assertEquals(1, normalized.getInterventions().size());
        assertEquals("Test Drug", normalized.getInterventions().get(0).getName());
        assertEquals("DRUG", normalized.getInterventions().get(0).getType());

        assertEquals(2, normalized.getOutcomes().size());
        assertEquals("PRIMARY", normalized.getOutcomes().get(0).getOutcomeType());
        assertEquals("Overall Response Rate", normalized.getOutcomes().get(0).getMeasure());
        assertEquals("SECONDARY", normalized.getOutcomes().get(1).getOutcomeType());

        assertEquals(1, normalized.getLocations().size());
        var location = normalized.getLocations().get(0);
        assertEquals("Test Cancer Center", location.getFacility());
        assertEquals("Denver", location.getCity());
        assertEquals(0, new BigDecimal("39.7392").compareTo(location.getLatitude()));

        assertEquals(1, normalized.getOverallOfficials().size());
        assertEquals("Jane Doe, MD", normalized.getOverallOfficials().get(0).getName());

        assertEquals(List.of("Advanced Solid Tumor", "Metastatic Cancer"), normalized.getConditionNames());

        assertEquals(2, normalized.getSponsors().size());
        assertEquals("Test Pharma Inc.", normalized.getSponsors().get(0).getName());
        assertEquals("INDUSTRY", normalized.getSponsors().get(0).getOrgClass());
        assertEquals("Test University", normalized.getSponsors().get(1).getName());
    }

    private String readFixture() throws IOException {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("sample-clinicaltrials-study.json")) {
            assertNotNull(in, "Fixture file not found on classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
