package app.earcast.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.BuildConfig
import app.earcast.assist.LiveAudioController
import app.earcast.billing.ProBilling
import app.earcast.core.audio.StreamPhase
import app.earcast.data.PreferenceStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class TestAdStateModel
    @Inject
    constructor(
        billing: ProBilling,
        preferences: PreferenceStorage,
        audio: LiveAudioController,
    ) : ViewModel() {
        val mayRequestAds =
            combine(
                billing.state,
                preferences.observeConsentAccepted(),
                audio.sessionStatus,
            ) { pro, accepted, session ->
                TestAdPolicy(
                    debugBuild = BuildConfig.DEBUG,
                    safetyAccepted = accepted,
                    purchaseLoaded = pro.loaded,
                    proOwned = pro.owned,
                    listening = session.state == StreamPhase.CONNECTING || session.state == StreamPhase.RUNNING,
                ).mayRequestAds
            }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }
