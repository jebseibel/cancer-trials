# AI Processing for Retirement Certificates

## Overview

6 of 12 retirement certificate document types use AI-powered extraction (50%). The remaining 6 use traditional table extraction (Tabula) or email parsing (Apache POI).

## AI-Based Document Types (Gemini Extraction)

| Tracking System | Document Type | Processing Time | Method |
|---|---|---|---|
| ERCOT   | **Screenshot** | Several minutes | Vision-based extraction |
| MRETS   | **Transaction Confirmation** | ~1 minute | Text + structured parsing |
| NAR     | **Certificate Subaccount** | ~1 minute | Text + structured parsing |
| NAR     | **Retirement Certificate** | ~1 minute | Text + structured parsing |
| NAR     | **Voluntary Compliance** | Variable (1-5 minutes) | Batch processing with duplicate prevention |
| WREGIS  | **Transaction Confirmation** | ~1 minute | Text + structured parsing |

### Why AI vs Traditional Parsing?

**AI-Powered Extraction:** Used for complex layouts with:
- Mixed content (headers/footers + tables)
- Unstructured formats
- Metadata scattered throughout document
- Variable page layouts

**Traditional Parsing (Tabula/Email):** Used for:
- Clean tabular data with consistent structure
- Well-defined table layouts

## AI Processing Flow

### Standard AI Documents (5 types)
**Examples:** WREGIS TransConfirm, MRETS TransConfirm, NAR CertSubacct, NAR RetireCert

1. Extract PDF text using PdfTextExtractor
2. Send entire text to Gemini with structured extraction prompt
3. Gemini returns JSON-formatted records
4. Parse JSON response and save records to database
5. All records created with status = "NEW"

### NAR Voluntary Compliance (Special Case)
**File:** `RetNarVolComplyService.java`

Unique because it uses **duplicate prevention**:

1. Extract full PDF text using PdfTextExtractor
2. Process in **batches of 5 rows**
3. Send batch prompt to Gemini with list of already-extracted serial numbers
4. Gemini filters duplicates and returns 5 unique rows
5. Save unique rows immediately to database
6. Track Certificate Serial Numbers to prevent re-extraction
7. Continue until **2 consecutive batches return no new rows**
8. Uses `gemini` AI provider (`gemini-2.5-flash`). Prompts are loaded from the database via `AiPromptDbService` / `PromptLoaderService` (keyed by `uniqueId`).

**Benefits:**
- Duplicate prevention prevents re-extraction of same data
- Incremental saving reduces memory usage and enables recovery
- Handles multi-page PDFs (typical file has 9 pages)
- Returns accurate record counts in upload response

**Restored:** December 29, 2025 (switched from Tabula back to AI after Tabula approach failed)

### ERCOT Screenshot (Vision-Based)
**File:** Determined by service routing logic

- **Extraction Method:** Gemini Vision (AI visual analysis, `gemini-2.5-flash`)
- **Processing Time:** 1-5 minutes (potentially longer depending on image quality)
- **Data Extraction:** Vision model analyzes screenshot image and extracts structured data
- **Columns Extracted:** Year, Qtr, REC Type, Facility ID, Serial Start, Serial End, Quantity, Action, From, To, Date, Retire Reason, Compliance Year, Memo
- **Special Note:** Most time-consuming AI extraction type due to image processing overhead

### Frontend Warnings for Slow AI Processing

Two document types show red warning text to users:

1. **ERCOT Screenshot:**
   - Warning: "Screenshot processing uses AI and may take several minutes to complete"
   - Location: Recent Uploads panel on upload page

2. **NAR Voluntary Compliance:**
   - Warning: "Voluntary Compliance processing uses AI and may take several minutes to complete. ***This is the longest running and least accurate.***"
   - Displayed in red, semibold text
   - Location: Recent Uploads panel on upload page

## Service Architecture

### Service Routing
Single `/api/retirement/upload` endpoint routes based on `trackingSystem` parameter:
- **RetWregisService** - WREGIS documents (uses AI for TransConfirm)
- **RetMretsService** - MRETS documents (uses AI for TransConfirm)
- **RetNarService** - NAR documents (uses AI for all 3 types)
- **RetErcotService** - ERCOT documents (uses AI for Screenshot)

### AI Provider Configuration
- **Default Provider:** Google Gemini
- **Prompts:** Stored in the database (`ai_prompt` table) and loaded at runtime by `PromptLoaderService` via `AiPromptDbService.findByUniqueId(...)`
- **Models Used:** gemini-2.5-flash
- **OpenRouter:** Also available as a provider; default model `google/gemini-2.5-flash` via OpenRouter's
  OpenAI-compatible API. Requires `OPENROUTER_API_KEY` env var. Provides access to models from OpenAI,
  Anthropic, Google, and others through a single unified endpoint.

## Processing Time Comparison

| Extraction Method | Processing Time | Examples |
|---|---|---|
| **Tabula (Fast)** | < 10 seconds | TransDetails, CertQuant, ERCOT TransDetail |
| **Email Parser** | < 10 seconds | ERCOT Email |
| **AI (Standard)** | ~1 minute | TransConfirm, CertSubacct, RetireCert |
| **AI (Vision)** | 1-5 minutes | ERCOT Screenshot |
| **AI (Batch)** | 1-5 minutes | NAR VolComply (multi-page) |

## Data Preservation

AI extraction preserves all values from the PDF:
- "N/A" values are kept (not filtered)
- Empty strings become null
- Whitespace is normalized
- All extracted data is retained as-is

## Record Status After AI Extraction

When a record is extracted from a PDF via AI, its status is set to **"NEW"** to indicate:
- Record has been extracted but not yet reviewed
- User must approve (promote) the record for use
- Can be edited, promoted, or rejected

## API Response Flow

### Upload Request
**POST /api/retirement/upload**
```
Parameters:
- trackingSystem (WREGIS, MRETS, NAR, ERCOT)
- docType (varies by system)
- customer
- year
- file (PDF or .msg)
```

### Upload Response
1. PDF stored to disk with hierarchical path
2. RetirementUpload record created with status="Processing"
3. Parser (Tabula, Email, or AI) extracts data
4. Detail records created with status="NEW"
5. Upload status changed to "Completed"
6. Response includes: batchUuid, recordCount, status

### Retrieval by Batch
**GET /api/ret-{system}-{doctype}/by-upload/{batchId}**

Returns all records from that batch with:
- All extracted fields
- Status (always "NEW" after fresh extraction)
- Line numbers (tracking where in PDF)
- Unique IDs linking to original PDF

## Frontend Display

### Recent Uploads Panel
Shows only the most recent completed uploads (max 12):
- Displays filename, tracking system, document type, record count
- Shows AI processing warnings for slow document types
- Users can click to review records before promotion

### Detail Pages
When viewing AI-extracted records:
- Shows status "NEW" (blue badge) for fresh extractions
- All extracted fields visible in sortable table
- Users can edit individual fields if corrections needed
- Promote or reject buttons to approve/disapprove extraction

## Known Limitations

### NAR Voluntary Compliance
- **Longest processing time:** Multi-page documents (9+ pages) with batch processing
- **Least accurate:** Marked in UI as warning - may require manual review
- **Batch size:** 5 rows per batch
- **Duplicate prevention:** Essential for multi-page documents
- **Recovery:** Incremental saving enables recovery if interrupted

### ERCOT Screenshot
- **Slow:** Vision-based extraction takes 1-5 minutes
- **Image quality dependent:** Poor quality screenshots may yield incomplete data
- **User warning:** Required to inform users of processing time

## Future Improvements

Potential enhancements for AI processing:
1. **Parallel processing** - Process multiple batches concurrently for NAR VolComply
2. **Caching** - Cache extraction results for identical documents
3. **Streaming responses** - Send partial results to frontend as extraction progresses
4. **Accuracy metrics** - Track extraction accuracy per document type
5. **Manual validation** - Allow users to flag extraction errors for ML training
6. **Custom models** - Fine-tune Claude models on historical extraction patterns

## Testing AI-Powered Documents

To test AI extraction:

1. **Upload document** for one of the 6 AI-based types
2. **Monitor processing** - Allow sufficient time for extraction
3. **Review extracted records** - Check if data looks accurate
4. **Compare with source PDF** - Download and verify against original
5. **Edit if needed** - Make corrections before promoting
6. **Promote records** - Approve for use once verified

### Test Documents Available
- Various sample PDFs in test data folder
- Real tracking system documents (WREGIS, MRETS, NAR, ERCOT formats)
- Multi-page NAR documents for batch processing testing
