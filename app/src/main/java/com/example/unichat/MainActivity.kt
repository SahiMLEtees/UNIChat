package com.example.unichat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.unichat.ui.theme.UNIChatTheme
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UNIChatTheme {
                BackgroundImageTheme {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun BackgroundImageTheme(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        val background: Painter = painterResource(id = R.drawable.background)
        Image(
            painter = background,
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        content()
    }
}

@Composable
fun MainScreen() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(3000) // Show splash screen for 3 seconds
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        PermissionRequestScreen()
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val logoImage: Painter = painterResource(id = R.drawable.logo)
        Image(
            painter = logoImage,
            contentDescription = "App Logo",
            modifier = Modifier.size(350.dp)
        )
    }
}

@Composable
fun PermissionRequestScreen() {
    val context = LocalContext.current
    var allPermissionsGranted by remember { mutableStateOf(false) }

    val requiredPermissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // Launcher to request permissions
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        allPermissionsGranted = permissions.all { it.value }
        if (allPermissionsGranted) {
            navigateToLogin(context)
        } else {
            Toast.makeText(context, "Please grant all permissions to continue.", Toast.LENGTH_LONG).show()
        }
    }

    // Check permissions initially
    LaunchedEffect(Unit) {
        allPermissionsGranted = requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        }
        if (allPermissionsGranted) {
            navigateToLogin(context)
        }
    }

    if (!allPermissionsGranted) {
        // UI prompting user to grant permissions
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "UNIChat requires the following permissions to function properly:",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text("• Camera\n• Location", style = MaterialTheme.typography.bodyMedium)

                Button(onClick = { permissionsLauncher.launch(requiredPermissions.toTypedArray()) }) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

fun navigateToLogin(context: android.content.Context) {
    val intent = Intent(context, LoginActivity::class.java)
    context.startActivity(intent)
    (context as? ComponentActivity)?.finish()
}
