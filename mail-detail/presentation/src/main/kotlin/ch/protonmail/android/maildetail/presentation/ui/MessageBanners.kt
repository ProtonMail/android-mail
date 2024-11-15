package ch.protonmail.android.maildetail.presentation.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.NO_CONTENT_DESCRIPTION
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.string
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageBannersUiModel
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallInverted
import me.proton.core.compose.theme.defaultSmallWeak

@Composable
fun MessageBanners(messageBannersUiModel: MessageBannersUiModel) {
    Column {
        if (messageBannersUiModel.shouldShowPhishingBanner) {
            MessageBanner(
                icon = R.drawable.ic_proton_hook,
                iconTint = ProtonTheme.colors.iconInverted,
                text = TextUiModel.TextRes(R.string.message_phishing_banner_text),
                textStyle = ProtonTheme.typography.defaultSmallInverted,
                backgroundColor = ProtonTheme.colors.notificationError,
                borderColorIsBackgroundColor = true
            )
        }
        if (messageBannersUiModel.expirationBannerText != null) {
            MessageBanner(
                modifier = Modifier.fillMaxWidth(),
                icon = R.drawable.ic_proton_hourglass,
                iconTint = ProtonTheme.colors.iconInverted,
                text = messageBannersUiModel.expirationBannerText,
                textStyle = ProtonTheme.typography.defaultSmallInverted,
                backgroundColor = ProtonTheme.colors.notificationError,
                borderColorIsBackgroundColor = true
            )
        }
        if (messageBannersUiModel.autoDeleteBannerText != null) {
            MessageAutoDeleteBanner(
                text = messageBannersUiModel.autoDeleteBannerText,
                textStyle = ProtonTheme.typography.defaultSmallWeak,
                backgroundColor = ProtonTheme.colors.backgroundSecondary
            )
        }
    }
}

@Composable
fun MessageBanner(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    iconTint: Color,
    text: TextUiModel,
    textStyle: TextStyle,
    backgroundColor: Color,
    borderColorIsBackgroundColor: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .testTag(MessageBodyTestTags.MessageBodyBanner)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
            )
            .border(
                width = MailDimens.DefaultBorder,
                color = if (borderColorIsBackgroundColor) backgroundColor else ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.medium
            )
            .background(color = backgroundColor, shape = ProtonTheme.shapes.medium)
            .padding(ProtonDimens.DefaultSpacing)
    ) {
        Row {
            Icon(
                modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerIcon),
                painter = painterResource(id = icon),
                contentDescription = NO_CONTENT_DESCRIPTION,
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(ProtonDimens.SmallSpacing))
            Text(
                modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerText),
                text = text.string(),
                style = textStyle
            )
        }

        content()
    }
}

@Composable
fun MessageAutoDeleteBanner(
    modifier: Modifier = Modifier,
    text: TextUiModel,
    textStyle: TextStyle,
    backgroundColor: Color
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(MessageBodyTestTags.MessageBodyBanner)
            .padding(
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.SmallSpacing + ProtonDimens.ExtraSmallSpacing
            )
            .border(
                width = MailDimens.DefaultBorder,
                color = ProtonTheme.colors.separatorNorm,
                shape = ProtonTheme.shapes.medium
            )
            .background(color = backgroundColor, shape = ProtonTheme.shapes.medium)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            modifier = Modifier.testTag(MessageBodyTestTags.MessageBodyBannerText),
            text = text.string(),
            style = textStyle
        )
    }
}

@Preview(
    name = "Main settings screen light mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    name = "Main settings screen dark mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PreviewMessageBanners() {
    ProtonTheme {
        MessageBanners(
            MessageBannersUiModel(
                shouldShowPhishingBanner = true,
                expirationBannerText = TextUiModel("This message will expire in 1 day, 2 hours, 3 minutes"),
                autoDeleteBannerText = TextUiModel("This message will be automatically deleted in 1 day")
            )
        )
    }
}
