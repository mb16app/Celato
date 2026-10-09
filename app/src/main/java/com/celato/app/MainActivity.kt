package com.celato.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CelatoTheme {
                CelatoApp()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Filled.Home),
    Explore("Explore", Icons.Filled.Search),
    Create("Create", Icons.Filled.AddCircle),
    Chat("Chat", Icons.Filled.Chat),
    Profile("Profile", Icons.Filled.Person)
}

@Composable
fun CelatoApp() {
    val authVm: AuthViewModel = viewModel()
    val user by authVm.user.collectAsState()

    val currentUser = user
    if (currentUser == null) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) { LoginScreen(authVm) }
    } else {
        MainScaffold(currentUser, onLogout = authVm::logout)
    }
}

@Composable
private fun MainScaffold(user: User, onLogout: () -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var openedProfile by remember { mutableStateOf<User?>(null) }
    val tabs = Tab.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index; openedProfile = null },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tabs[selected]) {
                Tab.Home -> FeedScreen(viewModel(), user, onOpenProfile = { openedProfile = it })
                Tab.Explore -> ExploreScreen(viewModel(), user, onOpenProfile = { openedProfile = it })
                Tab.Create -> CreatePostScreen(user, viewModel(), onPosted = { selected = 0 })
                Tab.Profile -> ProfileScreen(user, user, viewModel(key = "profile_${user.id}"), onLogout = onLogout)
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("${tabs[selected].label} - coming soon")
                }
            }

            openedProfile?.let { target ->
                BackHandler { openedProfile = null }
                Surface(Modifier.fillMaxSize()) {
                    ProfileScreen(
                        user = target,
                        me = user,
                        vm = viewModel(key = "profile_${target.id}"),
                        onBack = { openedProfile = null }
                    )
                }
            }
        }
    }
}
