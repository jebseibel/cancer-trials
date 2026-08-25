#!/usr/bin/env node
/**
 * Local (stdio) entry point — for Claude Desktop / Claude Code running on this machine,
 * launching this file as a child process. Single user, single static token, per
 * .claude/mcp-server/MCP_PRACTICE_PLAN.md Phase 1 scope.
 *
 * For the remote, multi-user entry point see httpServer.ts.
 */

import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { buildServer } from "./buildServer.js";

async function main() {
  const server = buildServer();
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("cancer-trial-mcp running on stdio");
}

main().catch((err) => {
  console.error("Fatal error starting cancer-trial-mcp:", err);
  process.exit(1);
});
