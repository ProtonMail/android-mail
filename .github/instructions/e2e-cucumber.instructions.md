---
applyTo: "e2e/**"
---

# E2E Cucumber Screenplay Pattern Guide

When working on files in the `e2e/` module, follow these conventions.

## Architecture

The e2e module uses the **Screenplay pattern** with Cucumber BDD,
Compose UI testing, and Arrow `Either` for error handling.

```
e2e/src/main/kotlin/.../e2e/
├── screenplay/    <- Core framework (DO NOT MODIFY)
├── adapters/      <- Port implementations (DO NOT MODIFY)
├── targets/       <- Top-level UI element vals per screen
├── tasks/         <- Performable actions (grouped by feature)
├── questions/     <- Answerable assertions (grouped by feature)
├── steps/         <- Cucumber step definitions
└── runner/        <- Test runner config
```

## Targets

Top-level vals, no wrapping object. One file per screen.

```kotlin
package ch.protonmail.android.e2e.targets

import ch.protonmail.android.e2e.screenplay.Target

val ToField = Target.ByText("To")
val SendButton = Target.ByDescription("Send")
```

## Tasks (Performable)

Resolve drivers via reified generics. Never pass UIDriver as a
constructor parameter.

```kotlin
class EnterRecipient(private val email: String) : Performable {
    override fun performAs(actor: Actor): Either<ScreenplayError, Unit> {
        val ui = actor.abilityTo<UIDriver>()
        ui.waitUntilVisible(ToField, DEFAULT_WAIT_MS)
            .onLeft { return it.left() }
        return ui.enterText(ToField, email)
    }
}
```

Use top-level factory functions for variants:
```kotlin
fun loginWithActorCredentials() = Performable { actor ->
    LoginWithCredentials(actor.credentials.username, actor.credentials.password)
        .performAs(actor)
}
```

## Questions (Answerable)

Return `Either<ScreenplayError, Unit>`, never Boolean.

```kotlin
class IsMessageSent : Answerable<Unit> {
    override fun answeredBy(actor: Actor): Either<ScreenplayError, Unit> =
        actor.abilityTo<UIDriver>()
            .waitUntilVisible(InboxLabel, LONG_WAIT_MS)
            .mapLeft {
                ScreenplayError.AssertionError.ConditionNotMet(
                    "Expected to return to inbox after sending"
                )
            }
}
```

## Step Definitions

Wire Cucumber steps to the Screenplay layer. Use `.getOrFail()` only.

```kotlin
class ComposeSteps {
    private lateinit var actor: Actor

    @Given("{string} is logged in")
    fun actorIsLoggedIn(actorName: String) {
        actor = Actor(actorName, TestUsers.credentialsFor(actorName))
            .can(
                ComposeUIDriver(CucumberComposeRule.rule),
                AndroidAppDriver()
            )
        actor.attemptsTo(
            LaunchApp(),
            loginWithActorCredentials()
        ).getOrFail()
    }
}
```

## Timeouts

Use the top-level constants from `Timeouts.kt`:
- `SHORT_WAIT_MS` (5s) — optional/transient elements
- `DEFAULT_WAIT_MS` (10s) — standard UI waits
- `MEDIUM_WAIT_MS` (15s) — already-loaded screens
- `LONG_WAIT_MS` (60s) — after network calls

## Rules

- Resolve drivers via `actor.abilityTo<UIDriver>()` (reified generic)
- Never hardcode timeout values
- Never return `Either<Error, Boolean>` from Questions
- Never use `.fold()` in step definitions — use `.getOrFail()`
- Never hardcode credentials — use `TestUsers.credentialsFor(name)`
- Never wrap targets in an `object` — use top-level vals
- Step classes must be in `ch.protonmail.android.e2e.steps` package

## Running

```bash
./gradlew :e2e:runE2e
```
