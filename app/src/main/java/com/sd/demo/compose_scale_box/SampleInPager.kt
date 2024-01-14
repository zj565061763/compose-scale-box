package com.sd.demo.compose_scale_box

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.sd.demo.compose_scale_box.ui.theme.AppTheme
import com.sd.lib.compose.scalebox.ScaleBox

class SampleInPager : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Content() {
    val listId = remember {
        listOf(
            R.drawable.image1,
            R.drawable.image2,
            R.drawable.image3,
        )
    }

    val state = rememberPagerState { listId.size }

    HorizontalPager(
        state = state,
        modifier = Modifier.fillMaxSize(),
    ) { index ->
        ScaleBox(
            modifier = Modifier.fillMaxSize(),
            debug = true,
        ) {
            Image(
                painter = painterResource(id = listId[index]),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth,
                contentDescription = "",
            )
        }
    }
}
