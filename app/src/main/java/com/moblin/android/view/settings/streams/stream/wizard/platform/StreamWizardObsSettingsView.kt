package com.moblin.android.view.settings.streams.stream.wizard.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.moblin.android.localized
import com.moblin.android.various.model.CreateStreamWizard
import com.moblin.android.various.model.Model
import com.moblin.android.various.model.WizardPlatform
import com.moblin.android.various.utils.makeUniqueName
import com.moblin.android.view.settings.streams.stream.wizard.networksetup.StreamWizardNetworkSetupObsSettingsView
import com.moblin.android.LocalModel

@Composable
fun StreamWizardObsSettingsView(
    model: Model = LocalModel.current,
    createStreamWizard: CreateStreamWizard,
) {
    DisposableEffect(Unit) {
        createStreamWizard.platform = WizardPlatform.obs
        createStreamWizard.name = makeUniqueName(
            name = localized("OBS"),
            existingNames = model.database.streams,
        )
        onDispose {}
    }
    StreamWizardNetworkSetupObsSettingsView(
        model = model,
        createStreamWizard = createStreamWizard,
    )
}
