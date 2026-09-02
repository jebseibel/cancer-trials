#!/usr/bin/env node
/**
 * Remote (Streamable HTTP) entry point — Phase 2 of MCP_PRACTICE_PLAN.md's production path.
 *
 * Each session is bound to the JWT the caller presented on their initialize request's
 * Authorization header — logged into the existing POST /api/auth/login, same as the web app.
 * The server never holds a shared token: it forwards each caller's own JWT to the backend, so
 * CurrentUserService's existing UserPatient grants and AccessLevel checks are what actually
 * decide what that caller can see. A missing/malformed Authorization header is rejected before
 * a session is created — fail fast, matching how the REST API itself behaves, rather than
 * connecting successfully and failing on the first tool call.
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
import { CancerApiClient } from "./apiClient.js";

const PORT = Number(process.env.MCP_HTTP_PORT ?? 3939);
const HOST = process.env.MCP_HTTP_HOST ?? "127.0.0.1";

interface Session {
  transport: StreamableHTTPServerTransport;
}

const sessions = new Map<string, Session>();

/** Pulls the bearer token out of `Authorization: Bearer <jwt>`, or undefined if absent/malformed. */
function extractBearerToken(req: http.IncomingMessage): string | undefined {
  const header = req.headers.authorization;
  if (!header) return undefined;
  const match = /^Bearer\s+(.+)$/i.exec(header);
  return match?.[1];
}

async function createSession(callerToken: string): Promise<StreamableHTTPServerTransport> {
  const api = new CancerApiClient(callerToken);
  const server = buildServer(api);
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

    const callerToken = extractBearerToken(req);
    if (!callerToken) {
      res
        .writeHead(401, { "Content-Type": "application/json" })
        .end(JSON.stringify({ error: "Missing Authorization: Bearer <jwt>. Log in via POST /api/auth/login first." }));
      return;
    }

    const chunks: Buffer[] = [];
    for await (const chunk of req) chunks.push(chunk as Buffer);
    const bodyText = Buffer.concat(chunks).toString("utf8");
    const parsedBody = bodyText ? JSON.parse(bodyText) : undefined;

    const transport = await createSession(callerToken);
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
    "Each connecting client must send Authorization: Bearer <jwt> from its own " +
      "POST /api/auth/login — the server forwards that token as-is and holds no shared credential.",
  );
});
