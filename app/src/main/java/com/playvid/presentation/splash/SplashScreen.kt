package com.playvid.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.playvid.R
import kotlinx.coroutines.delay

/** The launch splash is on screen for exactly this long. */
const val SPLASH_DURATION_MS = 2_000L

/** Set once the splash has run in this process, so a rotation or recreate doesn't show it again. */
private var splashAlreadyShown = false

/**
 * Shows the splash for [SPLASH_DURATION_MS] on a cold start, then [content]. The app's screens are
 * not composed until the splash is gone, so nothing competes with it for the first two seconds.
 */
@Composable
fun SplashGate(content: @Composable () -> Unit) {
    var showSplash by remember { mutableStateOf(!splashAlreadyShown) }

    LaunchedEffect(Unit) {
        if (showSplash) {
            delay(SPLASH_DURATION_MS)
            splashAlreadyShown = true
            showSplash = false
        }
    }

    if (showSplash) SplashContent() else content()
}

@Composable
private fun SplashContent() {
    val brand = colorResource(R.color.ic_launcher_background)
    Box(
        Modifier.fillMaxSize().background(brand),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = "PlayVid logo",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(24.dp)).background(brand)
            )
            Text(
                "PlayVid",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
        }
    }
}
