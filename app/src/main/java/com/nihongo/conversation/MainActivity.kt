package com.nihongo.conversation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.nihongo.conversation.data.local.SettingsDataStore
import com.nihongo.conversation.presentation.navigation.NihongoNavHost
import com.nihongo.conversation.presentation.theme.NihongoTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val userSettings by settingsDataStore.userSettings.collectAsState(
                initial = com.nihongo.conversation.domain.model.UserSettings()
            )

            NihongoTheme(
                textSizePreference = userSettings.textSize,
                contrastMode = userSettings.contrastMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NihongoNavHost()
                }
            }
        }
    }
}
