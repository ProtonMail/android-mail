/*
 * Copyright (c) 2025 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailfeatureflags.domain.model

data object DebugInspectDbEnabled : FeatureFlagDefinition(
    key = "debug_observe_db_enabled",
    name = "Attach to DB for debug inspection",
    category = FeatureFlagCategory.Global,
    description = "(Only on debuggable builds) Enables attaching AS DB inspector (read only) to rust DB",
    defaultValue = false
)

data object UpsellingEnabled : FeatureFlagDefinition(
    key = "MailAndroidV7Upselling",
    name = "Enable Feature Upsell",
    category = FeatureFlagCategory.Upselling,
    description = "Makes the upsell flow available for all supported entry points",
    defaultValue = false
)

data object OnboardingUpsellingEnabled : FeatureFlagDefinition(
    key = "MailAndroidV7OnboardingUpselling",
    name = "Enable Onboarding Upsell",
    category = FeatureFlagCategory.Upselling,
    description = "Makes the upsell flow available during the onboarding",
    defaultValue = true
)

data object MailBlackFriday2025Enabled : FeatureFlagDefinition(
    key = "MailBlackFriday2025",
    name = "Enable BF (Wave 1)",
    category = FeatureFlagCategory.Upselling,
    description = "Enables BF offers.",
    defaultValue = false
)

data object MailBlackFriday2025Wave2Enabled : FeatureFlagDefinition(
    key = "MailBlackFriday2025Wave2",
    name = "Enable BF (Wave 2)",
    category = FeatureFlagCategory.Upselling,
    description = "Enables BF offers.",
    defaultValue = false
)

data object SpringOffer2026Enabled : FeatureFlagDefinition(
    key = "MailAndroidV7SpringOffer2026",
    name = "Enable Spring Offer 26 (Wave 1)",
    category = FeatureFlagCategory.Upselling,
    description = "Enables Spring 26 wave 1.",
    defaultValue = false
)

data object SpringOffer2026Wave2Enabled : FeatureFlagDefinition(
    key = "MailAndroidV7SpringOffer2026Wave2",
    name = "Enable Spring Offer 26 (Wave 2)",
    category = FeatureFlagCategory.Upselling,
    description = "Enables Spring 26 wave 2.",
    defaultValue = false
)

data object ComposerAutoCollapseQuotedText : FeatureFlagDefinition(
    key = "MailAndroidV7ComposerAutoCollapsedText",
    name = "Auto collapse composer quoted text",
    category = FeatureFlagCategory.Composer,
    description = "Inject CSS to auto-collapse quoted text in Composer",
    defaultValue = false
)

data object ConversationDetailAutoExpandLastMessageEnabled : FeatureFlagDefinition(
    key = "MailAndroidV7LastMessageAutoExpand",
    name = "Auto expand last message in convo details",
    category = FeatureFlagCategory.Details,
    description = "Automatically expands the last non-draft message in the conversation details",
    defaultValue = false
)

data object ConversationDetailWebViewDarkModeFallbackEnabled : FeatureFlagDefinition(
    key = "MailAndroidV7WebViewDarkModeFallback",
    name = "Apply WebView dark mode fallback for unsupported devices",
    category = FeatureFlagCategory.Details,
    description = "Disables CSS media-query-based dark mode rendering if it is not supported (WebView < v100)",
    defaultValue = false
)

data object InjectDetailCssOverrideEnabled : FeatureFlagDefinition(
    key = "MailAndroidV7CssHeightOverride",
    name = "Inject CSS override in HTML messages",
    category = FeatureFlagCategory.Details,
    description = "Fix 0-height webview measurements by injecting additional CSS",
    defaultValue = false
)

data object RestrictMessageWebViewHeight : FeatureFlagDefinition(
    key = "MailAndroidV7RestrictMessageWebViewHeight",
    name = "Restrict message WebView height",
    category = FeatureFlagCategory.Details,
    description = "Restrict the height of the message web view in order to avoid crashes.",
    defaultValue = false
)

data object ShowRatingBoosterEnabled : FeatureFlagDefinition(
    key = "RatingAndroidMail",
    name = "Show rating booster",
    category = FeatureFlagCategory.Rating,
    description = "Show the rating app store dialog",
    defaultValue = false
)

data object PrivacyBundle2601 : FeatureFlagDefinition(
    key = "MailAndroidV7PrivacyBundle2601",
    name = "Enable Privacy bundle v1",
    category = FeatureFlagCategory.Details,
    description = "Enable padlocks + blocked trackers in details/composer.",
    defaultValue = false
)

data object FeatureSpotlight : FeatureFlagDefinition(
    key = "MailAndroidV7FeatureSpotlight",
    name = "Enable the feature spotlight",
    category = FeatureFlagCategory.Details,
    description = "Show a what's new screen on startup when available.",
    defaultValue = false
)
