# Claude Code Session Notes

This document captures the best practices and patterns established during the development of this project. It is the authoritative reference for code style, architecture decisions, and the pre-push checklist.

---

## Project Setup Best Practices

### 1. Use Version Catalogs (TOML)

Always centralise dependency versions in `gradle/libs.versions.toml`:

```toml
[versions]
spring-boot = "3.4.13"
wiremock-spring-boot = "3.10.6"

[libraries]
spring-boot-starter-webflux = { module = "org.springframework.boot:spring-boot-starter-webflux" }
wiremock-spring-boot = { module = "org.wiremock.integrations:wiremock-spring-boot", version.ref = "wiremock-spring-boot" }

[plugins]
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
versions = { id = "com.github.ben-manes.versions", version.ref = "versions" }
```

Benefits:
- Single source of truth for all versions
- No hardcoded versions in build files
- Easy to update dependencies
- Type-safe accessors in IDE

Usage in `build.gradle.kts`:
```kotlin
dependencies {
    implementation(libs.spring.boot.starter.webflux)
    testImplementation(libs.wiremock.spring.boot)
}
```

### 2. Condense JUnit Dependencies

Before (5 lines):
```kotlin
testImplementation(platform("org.junit:junit-bom:6.0.3"))
testImplementation(libs.junit.jupiter.api)
testImplementation(libs.junit.jupiter.params)
testRuntimeOnly(libs.junit.jupiter.engine)
testRuntimeOnly(libs.junit.platform.launcher)
```

After (2 lines):
```kotlin
testImplementation(libs.junit.jupiter)        // aggregates api, params, engine
testRuntimeOnly(libs.junit.platform.launcher)
```

### 3. Add Ben-Manes Versions Plugin

Essential for keeping dependencies up to date:

```kotlin
plugins {
    alias(libs.plugins.versions)
}
```

Usage:
```bash
./gradlew dependencyUpdates  # shows available updates
```

### 4. Use build-logic Convention Plugins

Multi-module Gradle projects must use a `build-logic/` included build with precompiled script convention plugins. This keeps submodule build files minimal and ensures consistent configuration across all modules.

Rules:
- The root `build.gradle.kts` contains only aggregate tasks — no shared configuration (`allprojects {}`, `subprojects {}`)
- Convention plugins live in `build-logic/src/main/kotlin/` and are named `bi.<concern>.gradle.kts`
- Plugin dependencies (Spring Boot, node-gradle, etc.) are declared as `implementation()` deps in `build-logic/build.gradle.kts`
- The version catalog at `gradle/libs.versions.toml` is automatically shared with build-logic — no duplication needed
- Submodule build files should be minimal: just `plugins { id("bi.xxx") }` plus module-specific dependencies

Directory structure:

```
project-root/
├── build-logic/
│   ├── build.gradle.kts          # declares plugin dependencies
│   ├── settings.gradle.kts       # enables version catalog sharing
│   └── src/main/kotlin/
│       ├── bi.java-conventions.gradle.kts
│       ├── bi.spring-boot-app.gradle.kts
│       └── bi.node-conventions.gradle.kts
├── gradle/
│   └── libs.versions.toml        # single source of truth for versions
├── settings.gradle.kts           # includeBuild("build-logic") + include(":api", ":server", ...)
├── build.gradle.kts              # aggregate tasks only
├── api/
│   └── build.gradle.kts          # plugins { id("bi.java-conventions") } + deps
└── server/
    └── build.gradle.kts          # plugins { id("bi.spring-boot-app") } + deps
```

`build-logic/settings.gradle.kts` — shares the root version catalog:

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

`build-logic/build.gradle.kts` — declares plugin dependencies so convention plugins can apply them:

```kotlin
plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.spring.boot.gradle.plugin)
    implementation(libs.spring.dependency.management)
    implementation(libs.node.gradle.plugin)
}
```

A convention plugin (`build-logic/src/main/kotlin/bi.spring-boot-app.gradle.kts`):

```kotlin
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
```

A submodule build file (`server/build.gradle.kts`) — minimal by design:

```kotlin
plugins {
    id("bi.spring-boot-app")
}

dependencies {
    implementation(project(":api"))
    implementation(libs.spring.boot.starter.webflux)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
```

---

## Architectural Decisions

### HttpGraphQlClient.url() Gotcha

`HttpGraphQlClient.Builder.url(String)` calls `webClientBuilder.baseUrl(url)`, which **replaces** the WebClient's base URL. Do not pass a path-only string like `/api/v1/graphql`.

Correct pattern: put the full URL including path in `karat.base-url` in `application.yml`, and do not call `.url()` on the builder:

```java
// In KaratGraphQlConfig:
WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build(); // full URL
return HttpGraphQlClient.builder(webClient).build();                        // no .url() call
```

### WireMock + @DynamicPropertySource Pattern

Start WireMock in a static initialiser (before the Spring context), then register the full URL:

```java
static final WireMockServer wireMock;
static {
    wireMock = new WireMockServer(options().dynamicPort());
    wireMock.start();
}

@DynamicPropertySource
static void registerProperties(DynamicPropertyRegistry registry) {
    registry.add("karat.base-url", () -> wireMock.baseUrl() + "/api/v1/graphql");
}
```

Note: the WireMock Spring Boot integration (`org.wiremock.integrations:wiremock-spring-boot`) lives in package `org.wiremock.spring`. Its `@ConfigureWireMock` uses `baseUrlProperties`, not `property`.

### REST Pagination

Expose a `next` URL field (null when no more pages exist) and accept a `pageToken` query parameter. Never expose internal cursor tokens or GraphQL pagination objects to REST consumers.

```java
// Good — next is an absolute or relative URL the consumer can follow directly
record PetPage(int totalCount, String next, List<Pet> pets) {

    static PetPage from(Page<Pet> page, ServerHttpRequest request) {
        final var pageInfo = page.pageInfo();
        final String next = (pageInfo != null && pageInfo.hasNextPage())
                ? UriComponentsBuilder.fromUri(request.getURI())
                        .replaceQueryParam("pageToken", pageInfo.endCursor())
                        .build().toUriString()
                : null;
        return new PetPage(page.totalCount(), next, page.nodes());
    }
}

// Bad — exposing internal cursor tokens directly
record BadPage(String endCursor, boolean hasNextPage, List<Pet> nodes) {}
```

### OpenAPI Annotations in `*Api` Interfaces

All OpenAPI/Swagger annotations go on the `*Api` interface, not on the controller class. Controllers contain only implementation logic.

```java
// Good — PetApi.java holds all annotations
@Tag(name = "Pets", description = "...")
@RequestMapping("/pets")
interface PetApi {

    @Operation(summary = "List pets", ...)
    @GetMapping(produces = V1_JSON)
    Mono<PetPage> listPets(...);
}

// Good — PetController.java is clean implementation
@RestController
public class PetController implements PetApi {

    @Override
    public Mono<PetPage> listPets(...) {
        // implementation only
    }
}
```

---

## Code Quality Standards

### Use `final` on Local Variables and Fields

Declare local variables and fields `final` wherever they are not reassigned. Apply to parameters too unless the verbosity clearly outweighs the benefit (e.g. long parameter lists).

```java
// Good
public Mono<PetPage> listPets(final String status, final String pageToken) {
    final PetFilter filter = PetFilter.builder().status(status).build();
    return petService.fetchPage(filter, pageToken);
}

// Bad — variables that are never reassigned should be final
public Mono<PetPage> listPets(String status, String pageToken) {
    PetFilter filter = PetFilter.builder().status(status).build();
    return petService.fetchPage(filter, pageToken);
}
```

### No Comments in Tests

Test method names must be self-documenting. Inline comments are noise.

```java
// Bad
@Test
void fetchesAllPets() {
    // Set up the filter
    PetFilter filter = PetFilter.builder().status("AVAILABLE").build();
    // Call the service
    List<Pet> result = service.listAll(filter, null).collectList().block();
    // Check we got results
    assertThat(result).isNotEmpty();
}

// Good
@Test
void fetchesAllPets() {
    final PetFilter filter = PetFilter.builder().status("AVAILABLE").build();
    final List<Pet> result = service.listAll(filter, null).collectList().block();
    assertThat(result).isNotEmpty();
}
```

### No Spurious Comments in Production Code

A comment is spurious if it restates what the code already clearly says. Comments are appropriate only when explaining *why* something non-obvious is done, or documenting a known gotcha (like the `HttpGraphQlClient.url()` behaviour above).

```java
// Bad — the comment adds nothing
// Fetch the page from the API
return graphQlClient.retrieve("pets").toEntity(...);

// Bad — variable name already communicates this
// cursor for the next page
String cursor = pageInfo.endCursor();

// Good — explains a non-obvious decision
// Do NOT call .url() here; it replaces the WebClient base URL rather than appending.
return HttpGraphQlClient.builder(webClient).build();
```

### Package-Private Methods for Testing

Make helper methods package-private (no access modifier) instead of `private` so they can be tested directly from the same package in the test source tree.

```java
// Bad — cannot be tested in isolation
private PetFilter buildFilter(String status, String type) { ... }

// Good — testable from the same package
PetFilter buildFilter(String status, String type) { ... }
```

Then write dedicated tests:
```java
class PetControllerFilterTest {
    @Test
    void buildsFilterFromTypeParam() {
        final PetController controller = new PetController(...);
        final PetFilter filter = controller.buildFilter("AVAILABLE", "DOG");
        assertThat(filter.type()).isEqualTo("DOG");
    }
}
```

### Line Width — 120 Characters

Lines should not exceed 120 characters. This applies to Java source, Kotlin build scripts, and YAML. Wrap long method signatures, builder chains, and string literals at natural break points.

### Always Use Curly Braces

All `if`, `else`, `for`, `while`, and `do-while` statements must use curly braces, even for single-line bodies. This prevents subtle bugs when adding lines later and improves readability.

```java
// Bad
if (exports.contains("users"))
    tasks.add(csvExportService.exportUsers(users).flux());

// Good
if (exports.contains("users")) {
    tasks.add(csvExportService.exportUsers(users).flux());
}
```

### Prefer `forEach` Over Enhanced For-Loops

When a for-each loop simply delegates to a single method call or populates a collection with no early returns, index mutation, or checked exceptions, use `forEach` with a lambda or method reference instead. This is more concise and expressive.

Only use a traditional `for` loop when the body needs `continue`, `break`, `return`, index variables, or must handle checked exceptions.

```java
// Bad — simple delegation
for (final String dashboardId : configLoader.columnConfigIds()) {
    regenerate(dashboardId);
}

// Good — forEach with method reference
configLoader.columnConfigIds().forEach(this::regenerate);

// Bad — simple map population
for (final TableColumns tc : tables) {
    tableColumnsMap.put(tc.table(), tc.columns());
}

// Good — forEach with lambda
tables.forEach(tc -> tableColumnsMap.put(tc.table(), tc.columns()));

// OK — traditional loop is correct here (early return)
for (final String prefix : MEASURE_PREFIXES) {
    if (label.startsWith(prefix)) {
        return label.substring(prefix.length());
    }
}
```

### Single Responsibility Principle

Every class should have exactly one reason to change. The 150-line limit and 50-line target are not arbitrary thresholds — they are proxies for SRP violations. If you need the word "and" to describe what a class does, it has more than one responsibility and should be split.

When splitting, name each resulting class after what it does, not after the original class. A split should produce two classes that each make sense on their own — not a "main" class and a "helper".

```java
// Bad — needs "and" to describe: "loads config AND manages cache AND reloads CSV data"
public class DashboardConfigLoader { ... }

// Good — each class has one job
DashboardStorage       // persists and loads bundles from disk
DashboardConfigLoader  // in-memory registry of dashboard configs
DashboardStartup       // classifies columns and regenerates dashboards on boot
```

### Class Size Limit — 150 Lines Maximum

No class should exceed 150 lines. A class approaching this limit is a signal to split it by responsibility.

### Small, Focused Classes (~50 Lines)

Prefer classes of around 50 lines. A class that grows past roughly 100 lines is a signal to split it.

```java
// Bad — one controller doing everything
public class PetController {
    // filter building, pagination, CSV export,
    // error mapping, OpenAPI annotations ... 300 lines
}

// Good — split by responsibility
PetApi         // OpenAPI contract (interface)
PetController  // REST wiring (implements PetApi)
CsvResponse    // shared CSV ResponseEntity builder
PetPage        // REST response record with from() factory
```

### Favour Polymorphism Over Conditionals

Replace chains of `if`/`else if` that switch on type or kind with polymorphism — enums with behaviour, strategy interfaces, or maps of functions. Long conditional chains are hard to test, extend, and read.

```java
// Bad — chain of conditionals
if (type == "DOUBLE") { return MEASURE; }
else if (isTemporalName(name)) { return TEMPORAL; }
else if (isIdColumn(name)) { return IDENTIFIER; }
// ... more ifs

// Good — ordered list of classification rules
private static final List<ClassificationRule> RULES = List.of(
    (name, type, stats) -> isIdColumn(name) ? IDENTIFIER : null,
    (name, type, stats) -> TEMPORAL_TYPES.contains(type) ? TEMPORAL : null,
    (name, type, stats) -> FRACTIONAL_TYPES.contains(type) ? MEASURE : null
);
```

### Break Down Complex Logic

Prefer many small, named methods over a few large ones. Every extracted method is a candidate for a direct unit test.

```java
// Bad — one large method
public Mono<Page<Pet>> fetchAll(PetFilter filter, String search) {
    // 80 lines combining pagination, filtering, error handling
}

// Good — composed from small, named methods
public Flux<Pet> listAll(PetFilter filter, String search) {
    return Flux.expand(page -> fetchNextPage(page, filter, search))
               .concatMap(page -> Flux.fromIterable(page.nodes()));
}

Mono<Page<Pet>> fetchNextPage(Page<Pet> previous, PetFilter filter, String search) {
    return hasMore(previous)
            ? client.fetchPage(filter, search, previous.pageInfo().endCursor())
            : Mono.empty();
}

boolean hasMore(Page<Pet> page) {
    return page.pageInfo() != null && page.pageInfo().hasNextPage();
}
```

### Prefer Immutable Objects

Use Java records, `final` fields, and immutable collections. Avoid setters, mutable state, and `Optional.set`.

```java
// Bad — mutable, setter-based object
public class PetFilter {
    private String status;
    public void setStatus(String status) { this.status = status; }
}

// Good — immutable record (or Lombok @Value with builder)
public record PetFilter(String id, String status, String type) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        // Lombok @Builder achieves the same result
    }
}
```

### Fail Fast — No Null Checks as Control Flow

Never use `if (x != null)` to branch logic, guard method calls, or silently provide defaults. This includes `Map.get()` followed by a null check — use `Map.getOrDefault()`, `Map.computeIfAbsent()`, or let it fail. The only acceptable uses of null checks are: (1) `Objects.requireNonNull` at API boundaries, (2) Jackson `@JsonInclude(NON_NULL)` for optional response fields.

If a value is required, let the NullPointerException happen or use `Objects.requireNonNull`. If a value is genuinely optional, use `Optional` in the API contract — never return null from a method.

```java
// Bad — silently skips saving, caller never knows
if (config != null && table != null && columns != null) {
    storage.persistBundle(config, table, columns);
}

// Bad — null check as branching logic
final String label = map.get(key);
if (label != null) {
    return label;
}
return fallback;

// Good — fails immediately with a clear message
Objects.requireNonNull(config, "config");
Objects.requireNonNull(table, "table");
storage.persistBundle(config, table, columns);

// Good — no null check needed
return map.getOrDefault(key, fallback);

// Good — Optional for genuinely optional values
Optional<ColumnConfig> findColumn(String name);
```

### Never Swallow Exceptions

Do not catch exceptions just to log and continue. If an operation fails, the caller needs to know. Catch-log-and-continue hides bugs and leaves the system in an inconsistent state. Only catch exceptions at well-defined boundaries (REST controllers, event listeners) where you can return a meaningful error response.

```java
// Bad — caller thinks save succeeded
try {
    storage.persistBundle(config, table, columns);
} catch (final IOException e) {
    log.warn("Failed to persist", e);
}

// Good — let it propagate
storage.persistBundle(config, table, columns); // throws IOException

// Good — at a REST boundary, convert to an error response
try {
    uploadService.processUpload(title, files);
} catch (final IOException e) {
    throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Upload failed", e);
}
```

### Never Catch Generic Exceptions in Controllers

Do not wrap controller logic in `try { ... } catch (Exception e)` to convert exceptions to HTTP status codes. Use `@RestControllerAdvice` with specific `@ExceptionHandler` methods instead. This centralises error mapping, eliminates repeated try/catch boilerplate, and ensures consistent error responses across all endpoints.

```java
// Bad — repeated in every controller method
try {
    final String query = queryBuilder.build(table, dimension, measure);
    return new WidgetData("query", executor.execute(query, connection));
} catch (final Exception e) {
    throw new ResponseStatusException(BAD_REQUEST, e.getMessage());
}

// Good — controller method has no try/catch
final String query = queryBuilder.build(table, dimension, measure);
return new WidgetData("query", executor.execute(query, connection));

// Good — centralised in ApiErrorHandler
@ExceptionHandler(IllegalArgumentException.class)
ResponseEntity<ApiError> handleBadRequest(final IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage()));
}

@ExceptionHandler(DataAccessException.class)
ResponseEntity<ApiError> handleQueryError(final DataAccessException exception) {
    return ResponseEntity.badRequest().body(new ApiError(400, exception.getMessage()));
}
```

### Prefer Non-Static Code

Avoid static utility methods scattered across the codebase. Prefer instance methods on well-named classes that can be injected and tested.

```java
// Bad
public class CsvUtils {
    public static ResponseEntity<byte[]> buildCsvResponse(byte[] bytes, String filename) { ... }
}

// Good — a small class with a clear purpose, usable as a Spring bean or static factory
public final class CsvResponse {
    public static ResponseEntity<byte[]> of(byte[] bytes, String filename) { ... }
    private CsvResponse() {}
}
```

### Static Imports for Readability

Use static imports when they make call sites clearer, particularly for constants, assertions, and factory methods. Do not use them when the originating class provides important context.

```java
// Good — assertion methods are clearly assertion methods
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

assertAll(
    () -> assertThat(page.totalCount()).isEqualTo(10),
    () -> assertThat(page.next()).isNull()
);

// Good — media type constants
import static org.fifties.housewife.server.MediaTypes.CSV;
import static org.fifties.housewife.server.MediaTypes.V1_JSON;

// Bad — static import removes important context
import static org.example.server.PetController.buildFilter; // unclear at call site
```

### No Magic Hard-Coded Strings

Extract repeated string literals into named constants. A string that appears more than once, or whose meaning is not self-evident, must be a constant. Name constants after the value they represent — do not add type prefixes like `VAR_`, `STR_`, `KEY_`.

```java
// Bad — magic strings inline
client.variable("first", 50);
client.variable("after", cursor);

// Bad — type prefix adds nothing
private static final String VAR_FIRST = "first";

// Good — name describes the value, no prefix
public static final String FIRST = "first";
public static final String AFTER = "after";

client.variable(FIRST, props.pageSize());
client.variable(AFTER, cursor);
```

### Prefer Monadic Functions

Prefer functions that take a single well-typed argument over functions that take a string label to describe the argument. When a function takes a type/label string parameter to vary its behaviour, consider splitting it into multiple clearly-named methods instead.

```java
// Bad — label parameter is a stringly-typed dispatch
SqlNames.validate(tableName, "table");
SqlNames.validate(dimension, "dimension");
SqlNames.validate(measure, "measure");

// Good — each method name communicates what it validates
SqlNames.validateTable(tableName);
SqlNames.validateDimension(dimension);
SqlNames.validateMeasure(measure);
```

### Meaningful Variable Names — No Abbreviations

Names must communicate intent clearly to a human reader. Abbreviations are only acceptable when they are universally understood in context (e.g. `id`, `url`, `csv`).

```java
// Bad
WebClient wc = WebClient.builder().baseUrl(props.baseUrl()).build();
ByteArrayOutputStream baos = new ByteArrayOutputStream();
PageInfo pi = page.pageInfo();

// Good
WebClient webClient = WebClient.builder().baseUrl(props.baseUrl()).build();
ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
PageInfo pageInfo = page.pageInfo();
```

### Meaningful Business Nouns in REST Responses

REST response fields must use meaningful business nouns, not generic container names.

```java
// Bad — generic field names leak implementation details
record Page(int totalCount, String next, List<Pet> items) {}
record Page(int totalCount, String next, List<Pet> nodes) {}
record Page(int totalCount, String next, List<Pet> results) {}

// Good — the field name tells you what it contains
record PetPage(int totalCount, String next, List<Pet> pets) {}
record OrderPage(int totalCount, String next, List<Order> orders) {}
record CustomerPage(int totalCount, String next, List<Customer> customers) {}
```

### Extract Shared Utilities into Named Classes

When the same pattern appears across more than one controller or service, extract it. Name the class after what it produces, not what it does.

```java
// Bad — repeated in every controller that produces CSV
ResponseEntity.ok()
    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
    .contentType(MediaType.parseMediaType("text/csv"))
    .body(bytes);

// Good — extracted into CsvResponse
public final class CsvResponse {
    public static ResponseEntity<byte[]> of(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(MediaTypes.CSV))
                .body(bytes);
    }
    private CsvResponse() {}
}

// Usage
return csvExportService.petsToCsvBytes(flux)
        .map(bytes -> CsvResponse.of(bytes, "pets.csv"));
```

### Class and Method Naming

Names must be clear to a human reader. Avoid jargon terms that describe role rather than purpose:

- No `*Helper`, `*Util`, `*Manager`, `*Processor` — name classes after what they represent or produce
- No method names that start with `handle`, `process`, `do`, `perform` — name methods after what they return or what they change

```java
// Bad
class PaginationHelper { ... }
void handlePetFilter(...) { ... }

// Good
class Pages { ... }             // follows JDK convention: Collections, Files, Paths, Pages
void buildFilter(...) { ... }   // returns a PetFilter
void fetchPage(...) { ... }     // returns a Mono<Page<Pet>>
```

### Logging

Use Lombok `@Slf4j` for all logging. Never use `System.out.println` or `e.printStackTrace()`.

```java
// Bad
System.out.println("Fetching page: " + cursor);
catch (Exception e) { e.printStackTrace(); }

// Good
@Slf4j
public class PetClient {
    log.debug("Fetching page (cursor={}, pageSize={})", cursor, pageSize);
    log.error("GraphQL request failed", exception);
}
```

### Use `java.nio.file` — Not Legacy `java.io.File`

Always use `java.nio.file.Path` and `java.nio.file.Files` for file operations. Do not use `java.io.File`, `FileInputStream`, `FileOutputStream`, or other legacy `java.io` file APIs.

```java
// Bad — legacy file API
File file = new File("/tmp/data.csv");
FileInputStream fis = new FileInputStream(file);

// Good — NIO
Path path = Path.of("/tmp/data.csv");
InputStream stream = Files.newInputStream(path);
```

### Use JUnit `@TempDir` for Temporary Files in Tests

Never create temporary files or directories manually in tests. Use JUnit's `@TempDir` annotation, which handles cleanup automatically.

```java
// Bad — manual temp directory management
Path tempDir = Files.createTempDirectory("test");
// ... must remember to clean up

// Good — JUnit manages lifecycle
@TempDir
Path tempDir;

@Test
void processesFile() throws IOException {
    Files.writeString(tempDir.resolve("data.csv"), "id,name\n1,Bella\n");
    // tempDir cleaned up automatically after test
}
```

---

## Test Standards

### No Underscores in Test Names, No Word "test"

Test method names are plain English camelCase sentences describing the behaviour under test.

```java
// Bad
@Test void test_fetchPage_returnsResults() {}
@Test void testFetchPageReturnsResults() {}

// Good
@Test void fetchesPageOfPets() {}
@Test void returnsEmptyPageWhenNoPetsMatch() {}
@Test void propagatesErrorWhenUpstreamFails() {}
```

### Use `assertAll` for Multiple Assertions

When asserting multiple properties of the same object, wrap them in `assertAll` so all failures are reported together.

```java
// Bad — stops at first failure, hides subsequent problems
assertThat(page.totalCount()).isEqualTo(10);
assertThat(page.pets()).hasSize(10);
assertThat(page.next()).isNull();

// Good — all three are reported even if the first fails
assertAll(
    () -> assertThat(page.totalCount()).isEqualTo(10),
    () -> assertThat(page.pets()).hasSize(10),
    () -> assertThat(page.next()).isNull()
);
```

### Tests Are Self-Documenting

A reader must understand what behaviour is being tested without reading the implementation. Structure tests as: arrange, act, assert — no comments needed.

```java
// Bad — requires reading implementation to understand intent
@Test
void returnsPage() {
    wireMock.stubFor(post(urlEqualTo("/api/v1/graphql")).willReturn(okJson(RESPONSE)));
    var result = client.fetchPage(PetFilter.builder().build(), null, null).block();
    assertThat(result).isNotNull();
}

// Good — intent is clear from names and structure alone
@Test
void returnsPageWithCorrectNodeCountAndCursor() {
    wireMock.stubFor(post(urlEqualTo("/api/v1/graphql")).willReturn(okJson(petResponse(10, "cursor_abc"))));
    final Page<Pet> page = client.fetchPage(PetFilter.builder().build(), null, null).block();
    assertAll(
        () -> assertThat(page.nodes()).hasSize(10),
        () -> assertThat(page.pageInfo().endCursor()).isEqualTo("cursor_abc")
    );
}
```

### No Disabled Tests or Commented-Out Code

A disabled test is a lie — it looks like coverage but provides none. Delete it or fix it. Commented-out code belongs in git history, not in source files.

```java
// Bad
@Disabled("TODO: fix this later")
@Test void fetchesPets() { ... }

// Bad
// @Test void fetchesOrders() { ... }

// Good — fix it, or delete it and recover from git history if needed
```

---

