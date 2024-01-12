package com.christophroyer.avybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.christophroyer.avybuddy.ui.theme.AvyBuddyTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvyBuddyTheme {
                // A surface container using the 'background' color from the theme
                MeasureButtons()
            }
        }
    }
}

@Preview
@Composable
fun MeasureButtons(

) {
    val context = LocalContext.current
    var measurementInProgress by remember { mutableStateOf(false) }
    val soundMeasurement = remember { SoundMeasurement() }

    if (measurementInProgress) {
        LaunchedEffect(soundMeasurement) {
            coroutineScope {
                launch {
                    soundMeasurement.runMeasurement(context)
                }
            }
            measurementInProgress = false
        }
    }

    Column (modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally){
        Row {
            Button(onClick = {measurementInProgress = true}) {
                Text("Start calibration")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = {
                soundMeasurement.stopMeasurement()
            }) {
                Text("Start measurement")
            }
        }
        if (soundMeasurement.measurementRunning) {
            Text("Measurement in Progress")
        }
    }
}
