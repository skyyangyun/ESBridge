# ESBridge
现代化的 Android JSBridge，支持 Kotlin 挂起函数，支持 JavaScript 异步函数与事件分发

## 安装
首先需要添加 jitpack 的源
```kts
repositories {
    maven { url = uri("https://jitpack.io") }
}
```
引入依赖
```kts
implementation("com.github.skyyangyun:ESBridge:1.0.1")
```

## 初始化
```kotlin
import name.yangyun.esbridge.ESBridge

val bridge = ESBridge(webview, 'ESBridge') // 默认注册名称为 ESBridge
setContentView(webview)
// 注册页面 onPageStared 事件，以便 webview 在刷新之后能正常工作
webview.webViewClient = object : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        bridge.onPageStarted()
    }
}

```

## Android注册同步函数
```kotlin
bridge.registerCall("plus") { dict ->
    val a = dict.optInt("a")
    val b = dict.optInt("b")
    JSONObject().put("result", a + b)
}
```
调用方式
```javascript
const { result } = ESbridge.plus({ a: 1, b: 2}) // result=3
```
