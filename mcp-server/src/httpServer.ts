#!/usr/bin/env node
/**
 * Remote (Streamable HTTP) entry point — Phase 1 of MCP_PRACTICE_PLAN.md's production path.
 *
 * This proves the network transport works for more than one connected client. It is
 * DELIBERATELY still single-token: every session forwards the same CANCER_API_TOKEN to the
 * backend, so every caller acts as the one seeded user regardless of who connects. That is
 * fine for "does the plumbing work" and NOT fine for "a family member logs in as themselves" —
 * Phase 2 replaces the static token with a per-session credential before this is handed to
 * anyone but the person who set up CANCER_API_TOKEN.
 *
 * One MCP session = one McpServer + one StreamableHTTPServerTransport pair, keyed by the
 * `Mcp-Session-Id` header the SDK generates on the initialize request. This mirrors the
 * MCP TypeScript SDK's own documented multi-session HTTP example — McpServer.connect() is
 * 1:1 with a transport, so a shared server instance cannot serve concurrent sessions safely.
 */

import { randomUUID } from "node:crypto";
import http from "node:http";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { buildServer } from "./buildServer.js";

const PORT = Number(process.env.MCP_HTTP_PORT ?? 3939);
const HOST = process.env.MCP_HTTP_HOST ?? "127.0.0.1";

interface Session {
  transport: StreamableHTTPServerTransport;
}

const sessions = new Map<string, Session>();

async function createSession(): Promise<StreamableHTTPServerTransport> {
  const server = buildServer();
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: () => randomUUID(),
    onsessioninitialized: (sessionId) => {
      sessions.set(sessionId, { transport });
      console.error(`session initialized: ${sessionId} (${sessions.size} active)`);
    },
    onsessionclosed: (sessionId) => {
      sessions.delete(sessionId);
      console.error(`session closed: ${sessionId} (${sessions.size} active)`);
    },
  });
  transport.onclose = () => {
    if (transport.sessionId) sessions.delete(transport.sessionId);
  };
  await server.connect(transport);
  return transport;
}

const httpServer = http.createServer(async (req, res) => {
  if (req.url !== "/mcp") {
    res.writeHead(404).end("Not found. MCP endpoint is POST/GET/DELETE /mcp");
    return;
  }

  try {
    const sessionId = req.headers["mcp-session-id"];
    const existing = typeof sessionId === "string" ? sessions.get(sessionId) : undefined;

    if (existing) {
      await existing.transport.handleRequest(req, res);
      return;
    }

    // No known session: only a POST initialize request may start one.
    if (req.method !== "POST") {
      res.writeHead(400).end("No active session for this request.");
      return;
    }

    const chunks: Buffer[] = [];
    for await (const chunk of req) chunks.push(chunk as Buffer);
    const bodyText = Buffer.concat(chunks).toString("utf8");
    const parsedBody = bodyText ? JSON.parse(bodyText) : undefined;

    const transport = await createSession();
    await transport.handleRequest(req, res, parsedBody);
  } catch (err) {
    console.error("Request handling error:", err);
    if (!res.headersSent) {
      res.writeHead(500, { "Content-Type": "application/json" }).end(
        JSON.stringify({ error: "Internal server error" }),
      );
    }
  }
});

httpServer.listen(PORT, HOST, () => {
  console.error(`cancer-trial-mcp listening on http://${HOST}:${PORT}/mcp`);
  console.error(
    "⚠️  Phase 1: every session shares CANCER_API_TOKEN — do not point anyone but the token's " +
      "own owner at this until per-session auth (Phase 2) lands.",
  );
});
