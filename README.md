[![](https://jitpack.io/v/zj565061763/compose-scale-box.svg)](https://jitpack.io/#zj565061763/compose-scale-box)

# Demo

![](https://thumbsnap.com/i/7Yo2ZyBd.gif?1026)
```kotlin
@Composable
fun SimpleSample() {
    ScaleBox(
        modifier = Modifier.fillMaxSize(),
        onTap = {
            logMsg { "onTap" }
        }
    ) {
        Image(
            painter = painterResource(id = R.drawable.image1),
            contentDescription = "",
        )
    }
}
```