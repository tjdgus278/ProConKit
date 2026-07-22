package com.example.proconkit

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.proconkit.ui.main.MainScreen

@Composable
fun MainNavigation() {
  MainScreen(modifier = Modifier.safeDrawingPadding().padding(20.dp))
}
