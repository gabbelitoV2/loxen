package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupObsSettingsView

@Composable
fun StreamWizardObsSettingsView(
    model: Model,
    createStreamWizard: CreateStreamWizard,
) {
    LaunchedEffect(Unit) {
        createStreamWizard.platform = CreateStreamWizard.Platform.obs
        createStreamWizard.name = makeUniqueName(
            name = localized("OBS"),
            existingNames = model.database.streams,
        )
    }
    StreamWizardNetworkSetupObsSettingsView(
        model = model,
        createStreamWizard = createStreamWizard,
    )
}
