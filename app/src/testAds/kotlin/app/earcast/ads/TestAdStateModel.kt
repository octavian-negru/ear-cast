package app.earcast.ads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.earcast.BuildConfig
import app.earcast.assist.LiveAudioController
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
        preferences: PreferenceStorage,
        audio: LiveAudioController,
    ) : ViewModel() {
        val mayRequestAds =
            combine(
                preferences.observeConsentAccepted(),
                audio.sessionStatus,
            ) { accepted, session ->
                TestAdPolicy(
                    enabled = BuildConfig.DEBUG,
                    safetyAccepted = accepted,
                    session = session.state,
                ).mayRequestAds
            }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }
