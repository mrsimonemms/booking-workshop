# Temporal x Spring Boot Workshop

Hands-on Temporal + Spring Boot workshop with
11 progressive exercises for client training.

See [README.md](README.md) for full documentation.

## Tech stack

- Java 21
- Spring Boot 4.0
- Temporal Java SDK 1.35
- Maven (wrapper per exercise)

## Build & run

Each exercise is self-contained in its own
directory with `exercise/` and `solution/`
subfolders. Only some exercises include a
`pom.xml` — others build on prior solutions.

```bash
# Run an exercise (from its directory)
cd <exercise>/exercise   # or solution
./mvnw spring-boot:run

# Run with a Spring profile
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=<profile>

# Run tests
./mvnw test

# Run a specific test
./mvnw test -Dtest=<TestClass>

# Build without tests
./mvnw package -DskipTests

# Test all solutions in parallel (requires Task)
task test

# Test a specific solution
task test:<name>

# Check retry metrics (requires running app)
curl -s http://localhost:3030/actuator/prometheus
# Then search the output for activity_retry lines.
```

## Modules

- **run-a-simple-workflow** — basic workflow
  and activity pattern
- **introduce-interceptors** — retry
  observability via `WorkerInterceptor`
- **use-interceptor-to-handle-auth-failure** —
  auth token propagation + local activity refresh
- **applying-best-practices** — code
  organisation guidelines
- **spring-boot-integration** —
  auto-registration of workers via Spring Boot
- **testing** — unit testing with
  `TestWorkflowEnvironment` and replay tests
- **worker-versioning** — pinned vs unpinned
  workflow migration
- **priority-and-fairness** — priority queues
  and fair share processing
- **saga-pattern-implementation** — scopes
  and compensation steps
- **dynamic-workflows-and-dsl** — dynamic
  workflow construction from DSL definitions

## Agents

Use the following agents (from the
[skillbox](https://github.com/alexandreroman/skillbox)
plugin) for all code tasks:

- **code-writer** — for ANY task that writes,
  modifies, or refactors code. This includes
  one-line fixes, import changes, visibility
  tweaks, and adding assertions. Never use
  the Edit or Write tools directly on source
  files — always delegate to this agent.
- **code-reviewer** — for read-only code review
  before merging or when investigating issues.

## Memory

At the start of every conversation, read
`.claude/project-memory/MEMORY.md` to load
project context from previous conversations.

Use the **project-memory** skill (from the
[skillbox](https://github.com/alexandreroman/skillbox)
plugin) proactively — without being asked — whenever
the conversation reveals project decisions, deadlines,
team context, external references, workflow preferences,
or corrective feedback worth persisting across
conversations.

**Important:** Always use the **project-memory**
skill to persist information. Never use the built-in
auto-memory system (`~/.claude/projects/.../memory/`)
for project decisions or context — it is local and
not shared with the team.

## Conventions

- Line length limits for readability:
  - Text / Markdown: 80 columns max
  - Code: 120 columns max
- Follow standard Markdown conventions: blank line
  before and after headings, blank line before and
  after lists, fenced code blocks with a language tag
- Always use the latest LTS or stable version of
  languages, frameworks, and libraries. Check the
  official documentation or use available tools
  (e.g. context7) to verify current versions before
  choosing a dependency.
- Temporal server must run locally for integration
  tests: `temporal server start-dev`
- Each exercise is independent — do not create
  shared parent POMs or cross-exercise dependencies
- Never use compound shell commands (`;`, `&&`,
  `|`) in Bash tool calls — this applies to
  every Bash tool invocation Claude makes during
  a conversation, not just code in documentation.
  Each command must be a separate Bash tool call.
  Common violations to watch for: `cd dir &&
  ./mvnw test`, `curl ... | grep ...`,
  `./mvnw test 2>&1 | tail -20`. Split every
  pipeline or chain into individual calls.
