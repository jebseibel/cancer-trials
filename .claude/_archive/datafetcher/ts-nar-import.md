# NAR (North American Renewables Registry) Import Process

## Overview

The NAR import fetches renewable energy facility data from the North American Renewables Registry public report and stores it in the `ts_nar` table.

## Data Source

- URL: `https://narenewables2.apx.com/myModule/rpt/myrpt.asp?r=111`
- Format: HTML table with pagination
- Records: ~1,150+ facilities across 24 pages (50 per page)

## Import Architecture

### Components

- **TsNarHtmlImporter** (datafetcher module) - Parses HTML and maps to domain objects
- **TsNarLoadingService** (datafetcher module) - Orchestrates the import process
- **TsNarDbService** (database module) - Handles database operations
- **TsNarController** (app module) - Exposes the fetch endpoint

### Import Flow

1. User triggers fetch via frontend button or API endpoint
2. Service deletes all existing NAR records (clean slate approach)
3. Importer fetches the first page and captures session cookies
4. Importer extracts total page count from HTML
5. Importer iterates through all pages, passing session cookies with each request
6. Each page's HTML table is parsed and records are saved to database
7. Results summary is returned to the user

## HTML Parsing Challenges

### Nested Table Structure

The NAR website uses nested tables within a form. The actual data table is identified by its background color attribute (`bgcolor="#F3F3ED"`). The selector targets this specific table to avoid capturing rows from wrapper tables.

### Header Row Detection

The header row contains more cells than data rows due to column spanning. The importer skips rows where the first cell contains "Account Holder" (header text).

### Session-Based Pagination

Pagination requires maintaining session cookies from the initial request. Without passing cookies, subsequent page requests return HTTP 500 errors. The importer captures cookies from the first response and includes them in all pagination requests.

### Rate Limiting

A 1-second delay is added between page requests to avoid overwhelming the NAR server and prevent rate limiting.

## Data Model

Each NAR record includes:
- Facility identification (NAR ID, facility name, account holder)
- Location (state, country)
- Technical details (resource type, nameplate capacity, ownership type)
- Operational info (first operation date, co-firing indicator)
- State eligibility flags (MO, NC, KS, NY, IL, PR, ME classes)
- Certifications (EPA GPP eligible, LIHI certified)

## Unique Identifier

Records are uniquely identified by: `"NAR " + trackingSystemId`

Example: `NAR GEN2550`

## Refresh Strategy

The import uses a "delete all, then insert" strategy rather than upsert. This ensures:
- Removed facilities are properly deleted
- No orphaned records remain
- Each import provides a complete, current snapshot

## Frontend Integration

The NAR page (`/ts-nar`) displays all imported records with:
- Search/filter functionality
- Pagination
- Fetch button to trigger new import
- Download to CSV option
