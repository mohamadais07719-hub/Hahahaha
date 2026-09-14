package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.MainViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.screens.SimulatorScreen
import com.example.ui.theme.FbBlue
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // Check permissions whenever user returns to the app
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.checkPermissions(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .testTag("bottom_navigation_bar")
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                                label = { Text("الرئيسية") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FbBlue,
                                    selectedTextColor = FbBlue,
                                    indicatorColor = FbBlue.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "التعليقات") },
                                label = { Text("التعليقات") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FbBlue,
                                    selectedTextColor = FbBlue,
                                    indicatorColor = FbBlue.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_sessions")
                            )

                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = { Icon(Icons.Default.Science, contentDescription = "المحاكي") },
                                label = { Text("المحاكي") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = FbBlue,
                                    selectedTextColor = FbBlue,
                                    indicatorColor = FbBlue.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag("nav_simulator")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> HomeScreen(viewModel = viewModel)
                            1 -> SessionsScreen(viewModel = viewModel)
                            2 -> SimulatorScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

