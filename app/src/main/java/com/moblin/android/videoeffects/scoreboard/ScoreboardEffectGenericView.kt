package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moblin.android.common.various.format
import com.moblin.android.platform.swiftui.SwiftUIFonts
import com.moblin.android.platform.swiftui.monospacedDigit
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard

@Composable
fun ScoreboardEffectGenericView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    generic: SettingsWidgetGenericScoreboard,
    scale: Double,
) {
    CompositionLocalProvider(LocalContentColor provides textColor) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.clip(RoundedCornerShape((5 * scale).dp)),
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides SwiftUIFonts.system(size = 25 * scale),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(secondaryBackgroundColor)
                        .padding((5 * scale).dp),
                ) {
                    Text(generic.title)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = generic.clock.format(),
                        style = SwiftUIFonts.system(size = 25 * scale).monospacedDigit(),
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
                modifier = Modifier
                    .background(primaryBackgroundColor)
                    .padding(horizontal = (5 * scale).dp),
            ) {
                CompositionLocalProvider(
                    LocalTextStyle provides SwiftUIFonts.system(size = 25 * scale),
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Spacer(Modifier.weight(1f))
                            Text(generic.home.uppercase())
                            Spacer(Modifier.weight(1f))
                        }
                        Column(horizontalAlignment = Alignment.Start) {
                            Spacer(Modifier.weight(1f))
                            Text(generic.away.uppercase())
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                CompositionLocalProvider(
                    LocalTextStyle provides SwiftUIFonts.system(size = scoreboardScoreFontSize * scale),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width((scoreboardScoreFontSize * 1.33 * scale).dp),
                    ) {
                        TeamScoreView(score = generic.score.home)
                        TeamScoreView(score = generic.score.away)
                    }
                }
            }
            PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
        }
    }
}
