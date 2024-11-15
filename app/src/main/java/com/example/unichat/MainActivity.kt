package com.example.unichat

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.unichat.ui.theme.UNIChatTheme
import kotlinx.coroutines.delay

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
            contentScale = ContentScale.Crop // Ensures background image covers the screen
        )
        content() // Display actual screen content on top of the background
    }
}

@Composable
fun MainScreen() {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(12000)
        showSplash = false
    }

    if (showSplash) {
        SplashScreen()
    } else {
        MainContent()
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
fun MainContent() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
        (context as? ComponentActivity)?.finish()
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    UNIChatTheme {
        BackgroundImageTheme {
            SplashScreen()
        }
    }
}
