package com.sd.demo.compose_scale_box

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.sd.demo.compose_scale_box.ui.theme.AppTheme
import com.sd.lib.compose.scalebox.ScaleBox

class SampleDefault : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Content()
            }
        }
    }
}

@Composable
private fun Content() {
    ScaleBox(
        modifier = Modifier.fillMaxSize(),
        debug = true,
        onTap = {
            logMsg { "onTap" }
        },
    ) {
        Image(
            painter = painterResource(id = R.drawable.image1),
            contentDescription = "",
        )
    }
}