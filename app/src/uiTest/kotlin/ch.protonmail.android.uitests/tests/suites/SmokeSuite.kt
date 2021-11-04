package ch.protonmail.android.uitests.tests.suites

import ch.protonmail.android.uitests.tests.login.LoginTests
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.experimental.categories.Categories
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Categories::class)
@Categories.IncludeCategory(SmokeTest::class)
@Suite.SuiteClasses(
    LoginTests::class
)
class SmokeSuite
