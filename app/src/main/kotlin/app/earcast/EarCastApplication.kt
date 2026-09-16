package app.earcast

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point; Hilt's dependency graph is rooted here. */
@HiltAndroidApp
class EarCastApplication : Application()
