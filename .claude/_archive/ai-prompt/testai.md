# TestAi Page

## Location
`frontend/src/pages/TestAi.tsx`

## Purpose
A developer tool page for testing AI models via OpenRouter. Allows selecting a model, optionally uploading a document, entering a prompt, running it, and downloading the result.

## Features

### Model Selection
- Fetches all available OpenRouter models on load via `testAiApi.getOpenRouterModels()`
- Default model: `anthropic/claude-haiku-4.5`
- Search box filters by model id or name
- **Modality filter pills** (All, text, image, audio, video, file) — derived from `input_modalities` field on each model
- Clicking a pill filters the list to models that support that input modality; clicking again deselects
- Shows selected model metadata: description, context length, pricing (in/out per token)
- **OpenRouter Models** link below the list opens `https://openrouter.ai/models` in a new tab

### Document Upload (optional)
- Drag-and-drop or click-to-select file input
- Accepts: `.pdf`, `.txt`, `.png`, `.jpg`, `.jpeg`, `.gif`, `.webp`, `.msg`
- If no file is uploaded, the instruction text is wrapped in a synthetic `prompt.txt` File and sent as the document

### Instruction / Prompt
- Resizable textarea for the prompt/instruction
- Required — submit is disabled until non-empty

### Run
- Button top-right of the Model card
- Disabled until modelId + instruction are present
- Calls `testAiApi.analyzeDocument(file, instruction, modelId)`
- Shows spinner + "Running..." during request

### Response
- Read-only resizable textarea showing `extractedText` from the API response
- Processing time displayed in seconds (top-right of card)
- **Download button** — saves the result as a file
  - Format selector: `.txt` / `.md` / `.json`
  - Uses `downloadBlob()` from `frontend/src/utils/download.ts`
  - Both selector and button are disabled until there is a result

## API

### Frontend service (`frontend/src/services/api.ts`)
```ts
testAiApi.getOpenRouterModels()
// GET /api/ai/openrouter/models
// Returns: { id, name?, description?, contextLength?, pricing?, input_modalities? }[]

testAiApi.analyzeDocument(file, instruction, modelId)
// POST /api/ai/analyze-document  (multipart/form-data)
// Form fields: file, instruction, provider="openrouter", model
// Returns: { extractedText, provider, model, processingTimeMs }
```

### Backend
- Controller: `ai-provider/src/main/java/com/seibel/cancer/aiprovider/controller/AiController.java`
- Model service: `ai-provider/src/main/java/com/seibel/cancer/aiprovider/service/OpenRouterModelService.java`
  - Models fetched from `https://openrouter.ai/api/v1/models` at startup and cached
  - `input_modalities` mapped from `architecture.input_modalities` on the OpenRouter response
- Model DTO: `ai-provider/src/main/java/com/seibel/cancer/aiprovider/dto/OpenRouterModel.java`
  - Fields: `id`, `name`, `description`, `contextLength`, `pricing`, `supportsVision`, `inputModalities`

## OpenRouter Notes
- `input_modalities` is returned per-model in the `/api/v1/models` response under `architecture.input_modalities`
- No separate categories/modalities API — filtering is done client-side
- OpenRouter supports `response_format` (json_object / json_schema) for structured output — this controls what the *model outputs*, not the download format
