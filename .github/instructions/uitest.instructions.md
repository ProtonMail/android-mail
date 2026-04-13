---
applyTo: "app/src/uiTest/**"
---

# uiTest Robot Pattern Guide

When working on files in the `app/src/uiTest/` module, follow these conventions.

## Architecture

The uiTest module uses the **robot pattern** with KSP code generation,
Compose UI testing, MockWebServer, and Hilt DI.

```
app/src/uiTest/kotlin/.../uitest/
├── di/              <- Hilt test modules (DO NOT MODIFY unless adding new overrides)
├── e2e/             <- End-to-end tests by feature
├── helpers/         <- Navigation, login, network dispatchers
├── models/          <- Data classes for UI state assertions
├── robot/           <- Page object robots (one per screen/section)
├── rule/            <- JUnit rules (DO NOT MODIFY unless adding new rules)
├── screen/          <- Isolated screen composition tests
└── util/            <- Matchers, locators, wait helpers, assertions
```

## Test Types

**E2E tests** extend `MockedNetworkTest`. They navigate the app, mock
the network, and assert via robots.

**Screen tests** extend `HiltInstrumentedTest`. They render Compose
content directly and assert without navigation or network.

## Robots

Robots wrap screens. Sections wrap parts of a screen. KSP annotations
generate the DSL glue code.

```kotlin
@AsDsl
internal class MyRobot : ComposeRobot() {
    private val root = composeTestRule.onNodeWithTag(TestTags.Root)
    init { root.awaitDisplayed() }

    @VerifiesOuter
    inner class Verify {
        fun isShown() { root.assertIsDisplayed() }
    }
}
```

```kotlin
@AttachTo(targets = [MyRobot::class], identifier = "topBar")
internal class MyTopBarSection : ComposeSectionRobot() {
    fun tapBack() { /* ... */ }

    @VerifiesOuter
    inner class Verify {
        fun hasTitle(expected: String) { /* ... */ }
    }
}
```

Rules:
- `@AsDsl` on root robots — generates `myRobot { }` top-level function
- `@AttachTo(targets = [...])` on sections — generates extension on each target
- `@VerifiesOuter` on inner `Verify` classes — generates `verify { }` extension
- Use `identifier` in `@AttachTo` to customize the function name
- Wait for root elements in `init` with `awaitDisplayed()`

## Network Mocking

Build dispatchers with the request DSL:

```kotlin
get("/mail/v4/messages")
    respondWith "/mail/v4/messages/list.json"
    withStatusCode 200

post("/mail/v4/messages/send")
    respondWith "/mail/v4/messages/send/ok.json"
    serveOnce true
    withNetworkDelay 500L
```

Combine in `@Before`:
```kotlin
mockWebServer.dispatcher combineWith myDispatcher()
```

Modifiers: `respondWith`, `withStatusCode`, `ignoreQueryParams`,
`matchWildcards`, `serveOnce`, `withNetworkDelay`, `simulateNoNetwork`,
`withPriority`.

## Navigation

Use `navigator { navigateTo(Destination.Inbox) }` to reach screens.
Available: `Inbox`, `Drafts`, `Archive`, `Spam`, `Trash`, `Composer`,
`MailDetail(pos)`, `EditDraft(pos)`, `SidebarMenu`, `Onboarding`.

## Test Users

- `LoginTestUserTypes.Free.SleepyKoala`
- `LoginTestUserTypes.Paid.FancyCapybara`
- `LoginTestUserTypes.External.StrangeWalrus`
- `LoginTestUserTypes.Deprecated.GrumpyCat` (default)

## Rules

- Always annotate test classes with `@HiltAndroidTest`
- Add a suite annotation: `@RegressionTest`, `@SmokeTest`, etc.
- Use `@TestId("...")` to link to external tracking
- Never hardcode wait durations — use `awaitDisplayed()` defaults
- Never use real network calls — always mock
- Never put assertions outside `Verify` inner classes
- Never create robots without the KSP annotations
- Reuse existing robots and sections before creating new ones

## Running

```bash
# All tests
./gradlew app:connectedAlphaDebugAndroidTest

# Single class
./gradlew app:connectedAlphaDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=ch.protonmail.android.uitest.e2e.composer.ComposerMainTests

# Smoke tests only
./gradlew app:connectedAlphaDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.filter=ch.protonmail.android.uitest.filters.SmokeTestFilter
```
