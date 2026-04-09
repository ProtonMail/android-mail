package ch.protonmail.android.e2e.screenplay

/** Default wait for UI elements to appear. */
const val DEFAULT_WAIT_MS = 10_000L
/** Extended wait for screens that load after network calls (e.g. login -> inbox). */
const val LONG_WAIT_MS = 60_000L
/** Short wait for optional or transient UI elements (e.g. onboarding). */
const val SHORT_WAIT_MS = 5_000L
/** Wait for elements on screens we expect to already be loaded. */
const val MEDIUM_WAIT_MS = 15_000L
