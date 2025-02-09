package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavOptions
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.example.myapplication.ui.theme.MyApplicationTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import javax.inject.Inject

@AndroidEntryPoint
class MainActivityHiltViewModel : ComponentActivity() {

    private val flow = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SampleNavHost(flow)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        flow.tryEmit(intent)
    }
}

@Composable
private fun GreetingScreen(
    viewModel: GreetingViewModel,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Hello ${viewModel.name}!",
                modifier = modifier
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onClickBack
            ) {
                Text("Back")
            }
        }

    }
}

@Composable
private fun HomeScreen(
    onNavigateToGreeting: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onNavigateToGreeting,
        ) {
            Text("Greet")
        }
    }
}

@Serializable
private object HomeHilt

@Serializable
private data class GreetingHilt(val name: String)

@Composable
private fun SampleNavHost(intentFlow: Flow<Intent>) {
    val navController = rememberNavController()

    LaunchedEffect(intentFlow) {
        intentFlow.collectLatest {
            it.data?.let { uri ->
                val request = NavDeepLinkRequest.Builder
                    .fromUri(uri)
                    .build()

                navController.navigate(
                    request,
                    navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
                )
            }
        }
    }

    NavHost(navController = navController, startDestination = HomeHilt) {
        composable<HomeHilt> {
            HomeScreen(
                onNavigateToGreeting = { navController.navigate(GreetingHilt("Compose Champion")) }
            )
        }
        composable<GreetingHilt>(
            deepLinks = listOf(
                navDeepLink<GreetingHilt>(basePath = "test://navigation/greetingHilt")
            )
        ) { backStackEntry ->
            val greeting = backStackEntry.toRoute<GreetingHilt>()
            GreetingScreen(
                viewModel = hiltViewModel(key = greeting.toString()),
                onClickBack = { navController.navigateUp() },
            )
        }
    }
}

@HiltViewModel
private class GreetingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val greeting = savedStateHandle.toRoute<GreetingHilt>()
    val name = greeting.name
}