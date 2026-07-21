# Retirement Certificate Types

**Note:** This document is intended for client use and provides a user-friendly overview of the retirement certificate upload process.

---

## Overview

The Viro system allows you to upload retirement certificate PDFs from various tracking systems (WREGIS, MRETS, NAR, ERCOT).
The system automatically extracts data from your PDFs and displays the imported records for verification.

## Document Type Processing Methods

| Tracking System | Report Type          | Extraction Process       | Model                       | Notes |
|-----------------|---|---|---|---|
| ERCOT           | Email                | Apache POI               | N/A                         | Extracts TSV data from .msg files |
| ERCOT           | Screenshot           | Gemini AI (Vision)       | gemini-2.5-flash            | AI-powered vision extraction, takes several minutes |
| ERCOT           | Trans Detail         | Tabula                   | N/A                         | Table extraction with multi-page continuation table support |
| MRETS           | Cert Quant           | Tabula                   | N/A                         | Table extraction |
| MRETS           | Trans Confirm        | Gemini AI                | gemini-2.5-flash            | Text extraction + structured parsing |
| MRETS           | Trans Details        | Tabula                   | N/A                         | Table extraction |
| NAR             | Cert Subacct         | Gemini AI                | gemini-2.5-flash            | Text extraction + structured parsing |
| NAR             | Retire Cert          | Gemini AI                | gemini-2.5-flash            | Text extraction + structured parsing |
| NAR             | Vol Comply           | Gemini AI                | gemini-2.5-flash            | Full text extraction + structured parsing |
| WREGIS          | Cert Quant           | Tabula                   | N/A                         | Table extraction |
| WREGIS          | Trans Confirm        | Gemini AI                | gemini-2.5-flash            | Text extraction + structured parsing |
| WREGIS          | Trans Details        | Tabula                   | N/A                         | Table extraction |

---

## AI Processing Models

The system uses one Gemini model for AI-powered document extraction:

- **Gemini Flash 2.5 (`gemini-2.5-flash`):** Used for all 6 AI-based document types
  - ERCOT Screenshot (vision-based)
  - MRETS Transaction Confirmation
  - NAR Certificate Subaccount
  - NAR Retirement Certificate
  - NAR Voluntary Compliance
  - WREGIS Transaction Confirmation

**AI prompts are stored in the database** (`ai_prompt` table) and managed through the AiPrompt admin UI.

`PromptLoaderService` looks them up at request time via `AiPromptDbService.findByUniqueId(...)`, keyed by the prompt's `uniqueId`. There are no YAML prompt files on disk.

**Total AI-Powered Types:** 6 of 12 document types (50%)

