const { Client } = require("@modelcontextprotocol/sdk/client/index.js");
const { SSEClientTransport } = require("@modelcontextprotocol/sdk/client/sse.js");

async function run() {
  const transport = new SSEClientTransport(new URL("http://localhost:8003/sse"));
  
  const client = new Client({
    name: "test-client",
    version: "1.0.0"
  }, {
    capabilities: {}
  });

  try {
    console.log("Connecting...");
    await client.connect(transport);
    console.log("Connected successfully!");
    const response = await client.request({ method: "initialize", params: { protocolVersion: "2024-11-05", capabilities: {}, clientInfo: { name: "test", version: "1.0" } } }, Object);
    console.log("Response:", response);
  } catch (err) {
    console.error("Error:", err);
  } finally {
    await transport.close();
  }
}

run();
