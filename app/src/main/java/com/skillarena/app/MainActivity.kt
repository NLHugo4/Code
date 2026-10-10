package com.skillarena.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skillarena.app.ui.GameScreen
import com.skillarena.app.ui.GameViewModel
import com.skillarena.app.ui.MenuScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                var inGame by rememberSaveable { mutableStateOf(false) }
                val vm: GameViewModel = viewModel()
                if (inGame) {
                    GameScreen(vm, onExit = { inGame = false })
                } else {
                    MenuScreen(
                        rating = vm.rating,
                        onPlay = {
                            vm.startMatch()
                            inGame = true
                        },
                    )
                }
            }
        }
    }
}
