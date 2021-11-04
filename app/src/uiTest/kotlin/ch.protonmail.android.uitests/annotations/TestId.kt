package ch.protonmail.android.uitests.testsHelper.annotations

/**
 * Use this annotation to annotate test cases with TestRail test case id.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class TestId(val id: String)
