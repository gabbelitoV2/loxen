package com.moblin.android.videoeffects.scoreboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moblin.android.common.various.format
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard

@Composable
fun ScoreboardEffectGenericView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    generic: SettingsWidgetGenericScoreboard,
    scale: Double,
) {
    val title by generic.title.collectAsState()
    val clock by generic.clock.collectAsState()
    val home by generic.home.collectAsState()
    val away by generic.away.collectAsState()
    val score by generic.score.collectAsState()

    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier
            .clip(RoundedCornerShape((5 * scale).dp)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding((5 * scale).dp)
                .background(secondaryBackgroundColor),
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = (25 * scale).sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = clock.format(),
                color = textColor,
                fontSize = (25 * scale).sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((6 * scale).dp),
            modifier = Modifier
                .padding(horizontal = (5 * scale).dp)
                .background(primaryBackgroundColor),
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = home.uppercase(),
                        color = textColor,
                        fontSize = (25 * scale).sp,
                    )
                    Spacer(Modifier.height(0.dp))
                }
                Column(
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(Modifier.height(0.dp))
                    Text(
                        text = away.uppercase(),
                        color = textColor,
                        fontSize = (25 * scale).sp,
                    )
                    Spacer(Modifier.height(0.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .width((scoreboardScoreFontSize * 1.33 * scale).dp),
            ) {
                TeamScoreView(score = score.home)
                TeamScoreView(score = score.away)
            }
        }
        PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
    }
}
