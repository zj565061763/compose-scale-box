package com.sd.demo.compose_scale_box

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.sd.demo.compose_scale_box.ui.theme.AppTheme
import com.sd.lib.compose.scalebox.FScaleBox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Content()
                }
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun Content() {
    HorizontalPager(
        count = 10,
        modifier = Modifier.fillMaxSize(),
    ) { page ->
        FScaleBox(
            modifier = Modifier.fillMaxSize(),
            debug = page == 0,
            onTap = {
                logMsg { "onTap" }
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.water),
                contentDescription = "",
                modifier = it,
            )
        }
    }
}

@Composable
private fun TestImage(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.water),
            contentDescription = "",
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer(
                    scaleX = 2f,
                    scaleY = 2f,
                    transformOrigin = TransformOrigin(0f, 0.5f),
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AppTheme {
        Content()
    }
}

fun logMsg(block: () -> String) {
    Log.i("compose-scale-box-demo", block())
}