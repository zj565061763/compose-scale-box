package com.sd.demo.compose_scale_box

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sd.demo.compose_scale_box.ui.theme.AppTheme
import com.sd.lib.compose.scalebox.ScaleBox

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Sample()
            }
        }
    }
}

@Composable
private fun Sample() {
    ScaleBox(
        modifier = Modifier.fillMaxSize(),
        debug = true,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SampleInPager() {
    val listId = remember {
        listOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
        )
    }

    HorizontalPager(
        pageCount = listId.size,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        ScaleBox(
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
    Log.i("ScaleBox-demo", block())
}