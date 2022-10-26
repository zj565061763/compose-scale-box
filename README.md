# Gradle

[![](https://jitpack.io/v/zj565061763/compose-scale-box.svg)](https://jitpack.io/#zj565061763/comopse-scale-box)

# Demo

![](https://thumbsnap.com/i/7Yo2ZyBd.gif?1026)
```kotlin
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
```