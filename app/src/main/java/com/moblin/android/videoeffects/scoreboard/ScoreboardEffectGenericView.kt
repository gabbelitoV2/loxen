package com.moblin.android.videoeffects.scoreboard

import androidx.compose.ui.graphics.Color
import com.moblin.android.common.various.format
import com.moblin.android.platform.swiftui.layout.EdgeSet
import com.moblin.android.platform.swiftui.layout.Font
import com.moblin.android.platform.swiftui.layout.HorizontalAlignment
import com.moblin.android.platform.swiftui.layout.VerticalAlignment
import com.moblin.android.platform.swiftui.layout.View
import com.moblin.android.platform.swiftui.layout.ViewBuilder
import com.moblin.android.platform.swiftui.layout.background
import com.moblin.android.platform.swiftui.layout.clipShape
import com.moblin.android.platform.swiftui.layout.font
import com.moblin.android.platform.swiftui.layout.foregroundStyle
import com.moblin.android.platform.swiftui.layout.frame
import com.moblin.android.platform.swiftui.layout.monospacedDigit
import com.moblin.android.platform.swiftui.layout.padding
import com.moblin.android.various.settings.SettingsWidgetGenericScoreboard

fun ViewBuilder.ScoreboardEffectGenericView(
    textColor: Color,
    primaryBackgroundColor: Color,
    secondaryBackgroundColor: Color,
    generic: SettingsWidgetGenericScoreboard,
    scale: Double,
): View = VStack(alignment = HorizontalAlignment.leading, spacing = 0.0) {
    HStack {
        Text(generic.title)
        Spacer()
        Text(generic.clock.format())
            .monospacedDigit()
            .font(Font.system(size = 25 * scale))
    }
        .font(Font.system(size = 25 * scale))
        .padding(5 * scale)
        .background(secondaryBackgroundColor)
    HStack(alignment = VerticalAlignment.center, spacing = 6 * scale) {
        VStack(alignment = HorizontalAlignment.leading) {
            VStack(alignment = HorizontalAlignment.leading) {
                Spacer(minLength = 0.0)
                Text(generic.home.uppercase())
                Spacer(minLength = 0.0)
            }
            VStack(alignment = HorizontalAlignment.leading) {
                Spacer(minLength = 0.0)
                Text(generic.away.uppercase())
                Spacer(minLength = 0.0)
            }
        }
            .font(Font.system(size = 25 * scale))
        Spacer()
        VStack {
            TeamScoreView(score = generic.score.home)
            TeamScoreView(score = generic.score.away)
        }
            .font(Font.system(size = scoreboardScoreFontSize * scale))
            .frame(width = scoreboardScoreFontSize * 1.33 * scale)
    }
        .padding(EdgeSet.horizontal, 5 * scale)
        .background(primaryBackgroundColor)
    PoweredByMoblinView(backgroundColor = secondaryBackgroundColor, scale = scale)
}
    .clipShape(RoundedRectangle(cornerRadius = 5 * scale))
    .foregroundStyle(textColor)
