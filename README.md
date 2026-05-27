# ESBridge
现代化的 Android JSBridge。

- ✅️ 支持 Kotlin 挂起函数。
- ✅️ 支持 JavaScript 异步函数
- ✅️ 支持 JavaScript 事件分发
- 自动JSON序列化/反序列化

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
// kotlin
bridge.registerCall("plus") { dict ->
    val a = dict.optInt("a")
    val b = dict.optInt("b")
    JSONObject().put("result", a + b)
}
```
调用方式
```javascript
// javascript
const { result } = ESBridge.plus({ a: 1, b: 2}) // result=3
```

## Android注册挂起函数
```kotlin
// kotlin
bridge.registerSuspend("delay") { dict ->
    val time = dict.getLong("time")
    delay(time)
    JSONObject().apply { put("time", time) }
}
```
调用方式
```javascript
// javascript
const { time } = await ESBridge.delay({ time: 100 }) // time=100
```

## 调用JavaScript钩子
首先在 JavaScript 中向 `ESBridge.$hooks` 注册钩子函数，支持同步或异步（返回 Promise）函数：
```javascript
// javascript
ESBridge.$hooks['greet'] = function(dict) {
    const { name } = dict
    return { message: 'Hello, ' + name }
}

// 也支持 async 函数
ESBridge.$hooks['fetchData'] = async function(dict) {
    const res = await fetch('/api/data')
    return await res.json()
}
```
### 回调形式调用
```kotlin
// kotlin
bridge.call("greet", JSONObject().put("name", "World")) { result ->
    result.onSuccess { json ->
        val message = json.getString("message") // "Hello, World"
    }
}
```

### 挂起函数形式调用
与回调形式等价，但以 Kotlin 挂起函数的方式调用，适合在协程中使用：
```kotlin
// kotlin
val result = bridge.call("greet", JSONObject().put("name", "World"))
val message = result.getString("message") // "Hello, World"
```

## 分发事件
在 JavaScript 中通过 `ESBridge.$emitter` 监听事件：
```javascript
// javascript
ESBridge.$emitter.addEventListener('userLogin', (event) => {
    const { userId, name } = event.detail
    console.log(`用户登录: ${name} (${userId})`)
})
```
在 Kotlin 中通过 `dispatchEvent` 分发事件：
```kotlin
// kotlin
bridge.dispatchEvent(
    ESBridgeEvent("userLogin", JSONObject().put("userId", 123).put("name", "Alice"))
)
```
