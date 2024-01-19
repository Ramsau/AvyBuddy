package com.christophroyer.avybuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.christophroyer.avybuddy.ui.theme.AvyBuddyTheme
import com.himanshoe.charty.common.ChartDataCollection
import com.himanshoe.charty.line.CurveLineChart
import com.himanshoe.charty.line.config.CurvedLineChartColors
import com.himanshoe.charty.line.config.LineConfig
import com.himanshoe.charty.line.model.LineData
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AvyBuddyTheme {
                // A surface container using the 'background' color from the theme
                MeasurePage()
            }
        }
    }
}

@Preview
@Composable
fun MeasurePage(

) {
    val context = LocalContext.current
    var measurementInProgress by remember { mutableStateOf(false) }
    val soundMeasurement = remember { SoundMeasurement(context) }

    if (measurementInProgress) {
        LaunchedEffect(soundMeasurement) {
            coroutineScope {
                launch {
                    soundMeasurement.startMeasurement()
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
        Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Button(onClick = { soundMeasurement.playDebug() }) {
                Text("Play back")
            }
        }
        if (soundMeasurement.measurementRunning) {
            Text("Measurement in Progress")
        } else {
            CurveLineChart(
                dataCollection = ChartDataCollection(listOf(
                    LineData(1F, 1),
                    LineData(2F, 2),
                    LineData(5F, 3),
                    LineData(3F, 4)
                )),
                radiusScale = 0F,
                chartColors = CurvedLineChartColors(
                    contentColor = listOf(
                        Color(0xffffaaff),
                        Color(0xffffaaff)
                    ),
                    dotColor = listOf(
                        Color(0xffffaaff),
                        Color(0xffffaaff)
                    ),
                    backgroundColors = listOf(
                        Color.White,
                        Color.White,
                    )
                ),
                lineConfig = LineConfig(false, false, 0F)
            )
        }
    }
}
