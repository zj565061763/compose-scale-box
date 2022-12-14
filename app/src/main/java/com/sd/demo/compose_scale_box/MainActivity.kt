package com.sd.demo.compose_scale_box

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
                    PagerSample()
                }
            }
        }
    }
}

@Composable
fun SimpleSample() {
    FScaleBox(
        modifier = Modifier.fillMaxSize(),
        onTap = {
            logMsg { "onTap" }
        }
    ) { scaleModifier ->
        Image(
            painter = painterResource(id = R.drawable.image1),
            modifier = scaleModifier,
            contentDescription = "",
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun PagerSample() {
    val listId = remember {
        listOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
        )
    }

    HorizontalPager(
        count = listId.size,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        FScaleBox(
            modifier = Modifier.fillMaxSize(),
            onTap = { logMsg { "onTap" } }
        ) { scaleModifier ->
            Image(
                painter = painterResource(id = listId[index]),
                modifier = scaleModifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                contentDescription = "",
            )
        }
    }
}

inline fun logMsg(block: () -> String) {
    Log.i("FScaleBox-demo", block())
}