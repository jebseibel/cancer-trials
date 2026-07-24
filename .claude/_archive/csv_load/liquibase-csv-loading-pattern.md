# Liquibase CSV Data Loading Pattern

## Overview

This document describes how to implement the Liquibase CSV data loading pattern for this project. This pattern allows you to load reference data from CSV files into your database during application startup via Liquibase migrations.

## Quick Start

### 1. Create Your CSV File

Place your CSV file in `database/src/main/resources/db/data/` directory.

**Example:** `company.csv`

```csv
code,name,description,contact_name,contact_email,contact_phone,domain,portal_contact,product_name,estimated_mwh,program,service_level,bulk_product_id
AATEST,AA Test,Test company for development,Eric Arnold,eric@test.com,,test.com,eric@test.com,Test Product,1000,Green-e Energy,Standard,
COMPANY2,Company Two,Description here,John Doe,john@company.com,555-1234,company.com,john@company.com,Product Name,5000,Green-e Energy,Standard,
```

**Best Practices:**
- Use UTF-8 encoding
- Use comma separators
- Use double quotes for cell values with commas or special characters
- Include a header row with column names matching your database table columns
- Put empty values as blank (nothing between commas), not quoted empty strings (`""`)

### 2. Create Your Liquibase Changeset

Create a YAML file in `database/src/main/resources/db/changelog/changes/` (e.g., `101-import-init-data.yaml`):

```yaml
databaseChangeLog:
  - changeSet:
      id: load-init-data
      author: your_name
      labels: load_csv_data
      changes:
        # Load Company data
        - loadData:
            tableName: company
            file: db/data/company.csv
            relativeToChangelogFile: false
            encoding: UTF-8
            separator: ','
            quotchar: '"'
            columns:
              - column: { name: code, type: string }
              - column: { name: name, type: string }
              - column: { name: description, type: string }
              - column: { name: contact_name, type: string }
              - column: { name: contact_email, type: string }
              - column: { name: contact_phone, type: string }
              - column: { name: domain, type: string }
              - column: { name: portal_contact, type: string }
              - column: { name: product_name, type: string }
              - column: { name: estimated_mwh, type: string }
              - column: { name: program, type: string }
              - column: { name: service_level, type: string }
              - column: { name: bulk_product_id, type: string }
```

**Key Settings:**
- `id` - Unique changeset identifier (required)
- `author` - Author name (required)
- `labels` - Tag for grouping related changesets (useful: `load_csv_data`)
- `tableName` - Database table name to insert data into
- `file` - Path to CSV file
- `relativeToChangelogFile: false` - Path is relative to classpath root
- `encoding: UTF-8` - File encoding
- `separator: ','` - CSV delimiter
- `quotchar: '"'` - Character for quoting fields with special characters
- `columns` - List of columns with their types

### 3. Include in Master Changelog

If your master changelog uses `includeAll`, it will automatically pick up the new changeset file:

```yaml
# db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - includeAll:
      path: db/changelog/changes/
```

If you use explicit includes, add your changeset:

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/101-import-init-data.yaml
```

### 4. Create Database Table (if needed)

Ensure the target table exists. This is typically done in an earlier changeset that creates the schema.

**Example:** `001-init.yaml`

```yaml
databaseChangeLog:
  - changeSet:
      id: create-company-table
      author: your_name
      changes:
        - createTable:
            tableName: company
            columns:
              - column:
                  name: id
                  type: bigint
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true }, defaultValueComputed: "(UUID())" }
              - column:
                  name: code
                  type: varchar(8)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: name
                  type: varchar(120)
                  constraints:
                    nullable: false
                    unique: true
              - column:
                  name: description
                  type: varchar(255)
              - column:
                  name: contact_name
                  type: varchar(255)
              - column: { name: created_at, type: datetime, constraints: { nullable: false }, defaultValueComputed: CURRENT_TIMESTAMP }
              - column: { name: updated_at, type: datetime }
              - column: { name: deleted_at, type: datetime }
              - column: { name: active, type: int, defaultValueNumeric: 1 }
              # ... other columns
```

## Base Columns (extid, created_at, updated_at, active)

When using CSV loading with this project's `BaseDb` pattern, you **don't need to include base columns in your CSV file or changeset**. They are automatically populated by database DEFAULT values defined in the table schema.

### How It Works

**Table Creation (001-init.yaml):**
```yaml
- column: { name: extid, type: varchar(36), constraints: { nullable: false, unique: true },
            defaultValueComputed: "(UUID())" }
- column: { name: created_at, type: datetime, constraints: { nullable: false },
            defaultValueComputed: CURRENT_TIMESTAMP }
- column: { name: updated_at, type: datetime }
- column: { name: active, type: int, defaultValueNumeric: 1 }
```

**CSV Loading (101-import-init-data.yaml):**
```yaml
- loadData:
    tableName: company
    file: db/data/company.csv
    columns:
      - column: { name: code, type: string }
      - column: { name: name, type: string }
      # ... ONLY your data columns, not base columns ...
```

**What Happens:**
1. Liquibase reads each row from the CSV and inserts it with only the specified columns
2. The database automatically applies DEFAULT values to unmapped columns
3. Result: All base columns are populated without needing to be in the CSV

| Column | Default Value | Behavior |
|--------|---------------|----------|
| `id` | AUTO_INCREMENT | Auto-increments for each new row |
| `extid` | UUID() | Generates a random UUID |
| `created_at` | CURRENT_TIMESTAMP | Sets to current date/time |
| `updated_at` | NULL | Optional, for tracking updates |
| `deleted_at` | NULL | Optional, for soft deletes |
| `active` | 1 | Defaults to active (true) |

### Why This Works

Liquibase's `loadData` bypasses JPA entirely and inserts data directly via SQL. When a column isn't included in the INSERT statement, MySQL applies the `DEFAULT` value automatically. This is a pure database feature, not code.

The JPA entity (`BaseDb`) is only used for application runtime—it's not involved in CSV loading at all.

### CSV File Format

Your CSV files should **only contain the data columns**, not the base columns:

```csv
code,name,description,contact_name,contact_email,contact_phone,domain,portal_contact,product_name,estimated_mwh,program,service_level,bulk_product_id
AATEST,AA Test,Test company for development,Eric Arnold,eric@test.com,,test.com,eric@test.com,Test Product,1000,Green-e Energy,Standard,
COMPANY2,Company Two,Description here,John Doe,john@company.com,555-1234,company.com,john@company.com,Product Name,5000,Green-e Energy,Standard,
```

Notice: No `extid`, `created_at`, `updated_at`, `active`, or `id` columns in the CSV—they're handled by defaults.

## Important Considerations

### Empty String Handling

**Critical:** Liquibase CSV loading bypasses JPA entirely and inserts data directly via SQL. This means:

- Empty CSV values are inserted as **empty strings (`""`)**, NOT NULL
- If your entity expects `nullable=false` but the CSV has empty values, the data will load but may not match entity constraints
- To handle empty strings, either:
  1. **Option A:** Make columns nullable in your entity if empty strings are expected
  2. **Option B:** Use a Liquibase stored procedure to clean empty strings after loading (see below)
  3. **Option C:** Pre-clean the CSV file to have only valid values

### Optional Pattern: Stored-Procedure String Cleanup

If you want a reusable cleanup step, create a stored procedure that cleans empty strings:

```yaml
- changeSet:
    id: create-clean-empty-strings-procedure
    author: your_name
    changes:
      - sql:
          sql: |
            CREATE PROCEDURE clean_empty_strings(IN table_name VARCHAR(255))
            BEGIN
              -- Stored procedure logic to convert empty strings to NULL
              -- (no equivalent procedure currently exists in this project)
            END
```

Then call it after loading:

```yaml
- loadData:
    tableName: company
    file: db/data/company.csv
    # ... configuration ...

- sql:
    sql: CALL clean_empty_strings('company')
```

### Data Type Handling

In the `columns` section, use these Liquibase types:
- `string` - Text/VARCHAR
- `numeric` - Numbers
- `boolean` - TRUE/FALSE
- `date` - Date values
- `datetime` - Timestamp values

**Note:** The `type: string` in the changeset tells Liquibase how to interpret the CSV data, not the database column type. The actual column type is defined in your table creation changeset.

### Changeset Idempotency

Liquibase tracks executed changesets in the `databasechangelog` table. Once a changeset runs successfully:
- It won't run again, even if the CSV changes
- To reload data, you must either:
  1. Create a new changeset with a different ID
  2. Manually delete the changeset entry from `databasechangelog`
  3. Rebuild the database

### File Paths

- `relativeToChangelogFile: false` - Path is relative to classpath root (`src/main/resources/`)
- `relativeToChangelogFile: true` - Path is relative to the changelog file location

Always use `false` and put CSV files in `db/data/` for consistency.

## File Organization

```
database/
├── src/main/resources/
│   └── db/
│       ├── changelog/
│       │   ├── db.changelog-master.yaml          # Master changelog
│       │   └── changes/
│       │       ├── 001-init.yaml                 # Create tables
│       │       ├── 002-types.yaml                # Create type tables
│       │       └── 101-import-init-data.yaml     # Load CSV data ← YOUR FILE HERE
│       └── data/
│           ├── company.csv                       # Your CSV file
│           ├── company_types.csv
│           └── ...other reference data...
└── build.gradle
```

## Troubleshooting

### Issue: "File not found"
- Check file path in `file:` setting
- Verify file is in `src/main/resources/db/data/`
- Ensure path uses forward slashes, not backslashes

### Issue: "Column not found" or constraint violations
- Verify CSV column names exactly match database column names
- Check for extra/missing columns in the changeset
- Ensure the table exists before the loadData changeset runs

### Issue: Data doesn't load
- Check that the changeset ID is unique
- Verify the changeset isn't already marked as executed in `databasechangelog`
- Check application logs for Liquibase errors
- Ensure CSV file is valid and properly formatted

### Issue: Empty string vs NULL mismatch
- If your entity has `nullable=false` but CSV has empty values, either:
  - Make the column nullable in your entity
  - Update CSV to not have empty values
  - Add string cleanup logic (see String Cleanup section above)

## Example in Context

Here's how this project does it:

**1. Table created in:** `001-init.yaml` (contains CREATE TABLE company)

**2. CSV file:** `database/src/main/resources/db/data/company.csv` (48 companies)

**3. Changeset:** `database/src/main/resources/db/changelog/changes/101-import-init-data.yaml`

**4. Entity:** `database/src/main/java/com/seibel/cancer/database/db/entity/CustomerDb.java`

**5. When app starts:**
- Liquibase reads master changelog
- Finds all changesets in `changes/` directory
- Checks `databasechangelog` table
- Runs any unexecuted changesets
- 101-import-init-data runs and loads company.csv
- Data is inserted into company table

## Next Steps

1. Create your CSV file in `db/data/`
2. Create your changeset YAML in `db/changelog/changes/`
3. Ensure your table exists or create it in an earlier changeset
4. Start your application
5. Liquibase automatically loads the data on first run

That's it! Liquibase handles the rest.
