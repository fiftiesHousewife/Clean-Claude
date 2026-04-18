# cleancode-refactoring MCP server

A stdio MCP server that exposes the Clean Code plugin's refactoring
recipes and common Gradle verification steps as structured tools. It
exists to cut the per-call token cost of the rework flow: instead of
the agent shelling to `./gradlew :refactoring:extractMethod …` via its
Bash tool and dumping 30–200 lines of Gradle output into context, the
agent calls a typed MCP tool and gets a structured reply.

## Tools

| Tool | Backed by | Returns |
|---|---|---|
| `extract_method(file, startLine, endLine, newMethodName)` | `ExtractMethodRecipe` in-process | `extracted <name> from lines N-M of <file>` or an error with the rejection reason |
| `verify_build(module)` | `./gradlew :<module>:compileJava` | `build OK` or the first few compiler error lines |
| `run_tests(module, testClass?)` | `./gradlew :<module>:test` (optional `--tests <class>`) | `tests: all passed` or the first few failed test names |
| `format(module)` | `./gradlew :<module>:spotlessApply` | `format applied` or the error output |

`move_method` is not yet exposed as a tool; tracked in the backlog as
the natural next addition. See `docs/plan-next-session.md`.

## Build

```bash
./gradlew :mcp:jar
```

Produces a self-contained fat jar at `mcp/build/libs/mcp-1.0-SNAPSHOT.jar`
with all OpenRewrite runtime dependencies bundled.

## Register with Claude Code

From the project root:

```bash
claude mcp add cleancode-refactoring java -jar $(pwd)/mcp/build/libs/mcp-1.0-SNAPSHOT.jar
```

Or, for per-project registration, drop the following into `.mcp.json` at
the project root:

```json
{
  "mcpServers": {
    "cleancode-refactoring": {
      "command": "java",
      "args": ["-jar", "mcp/build/libs/mcp-1.0-SNAPSHOT.jar"]
    }
  }
}
```

The server's working directory is the directory Claude Code is launched
from; keep that the project root so `./gradlew` resolves.

## Local verification

```bash
./gradlew :mcp:test
```

Unit tests cover the JSON-RPC envelope, the tool registry, and each tool
individually (with an injected fake Gradle runner for the
Gradle-backed tools).

## Why bespoke, not an MCP SDK

The server implements JSON-RPC 2.0 over stdio by hand rather than
pulling in Anthropic's Java MCP SDK. Four reasons:

1. **Size.** The full MCP protocol is generous — we need a fraction: initialize, tools/list, tools/call, and `notifications/initialized`. ~180 lines covers it.
2. **Dependency footprint.** An SDK pulls Netty or similar; keeps the fat jar small.
3. **Transparency.** When a client misbehaves, reading our own 180 lines is faster than triangulating an SDK bug.
4. **Latency.** Server cold start is roughly 300ms on a warm JVM — adding an SDK would make it noticeably slower for tools the agent calls dozens of times per session.
