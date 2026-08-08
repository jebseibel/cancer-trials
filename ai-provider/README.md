# AI Provider Module

> **Shelved — not part of the build.** `settings.gradle` has `include 'ai-provider'`
> commented out, so nothing here compiles or runs. See
> `.claude/_archive/ai-processing/ai-provider-module.md` for status and the cleanup needed
> before reviving it.

AI integration module using Spring AI with observability.

## Overview

This module provides a simplified, production-ready AI integration layer that:
- Uses Spring AI for multi-provider support (OpenAI, Anthropic)
- Exposes REST APIs for external integrations (n8n, etc.)
- Includes function/tool calling for dynamic data access
- Provides comprehensive observability (audit logs, metrics, cost tracking)

## Quick Start

### Configuration

Set API keys in application.yaml or environment variables.

### REST API

```bash
# Text chat
POST /api/ai/chat
{
  "prompt": "What is AI?",
  "provider": "openai"
}
```

### Documentation

- Swagger UI: http://localhost:8080/swagger-ui.html
- Metrics: http://localhost:8080/actuator/metrics
- Health: http://localhost:8080/actuator/health

## Features

- Multi-provider support (OpenAI, Anthropic)
- 7 REST endpoints with OpenAPI docs
- 5 AI tools/functions
- Comprehensive metrics and cost tracking
- Audit logging for all operations

See .claude/MIGRATION_COMPLETE.md for full details.
