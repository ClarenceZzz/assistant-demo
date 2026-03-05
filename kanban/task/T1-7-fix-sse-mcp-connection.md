# Goal
Fix the SSE MCP connection issue between Antigravity and the local Spring AI MCP Server. The Antigravity client is failing to initialize the connection, reporting: `Error: calling "initialize": sending "initialize": failed to connect (session ID: ): session not found.`

# Subtasks
- [ ] Reproduce the behavior using the official `@modelcontextprotocol/sdk` in Node.js.
- [ ] Understand how Spring AI MCP server parses the `sessionId` from the initialization POST request.
- [ ] Analyze the HTTP interaction (SSE event and POST initialization) between client and server.
- [ ] Identify if the URL is constructed incorrectly by the client or if the server is rejecting it.
- [ ] Propose and implement a fix in either the Spring configuration or server implementation.

# Developer
- Complexity: Medium
- Priority: High

# Acceptance Criteria
- [ ] The official MCP client can successfully establish an SSE connection and receive the initialization response.
- [ ] The Antigravity client can successfully connect to the local Spring AI MCP server.

# Test Cases
- Execute `test.mjs` (Node MCP client) and verify it logs `Connected successfully!`.

# QA
_Pending_

# Related Files / Design Docs
- `d:\code\Code\assistant-demo\mcp\mcp-server-sse\src\main\resources\application.yml`
- `d:\code\Code\assistant-demo\mcp\mcp-server-sse\src\main\java\com\example\mcp\McpServerSseApplication.java`

# Dependencies
- None.

# Notes & Updates
_Investigation started._
