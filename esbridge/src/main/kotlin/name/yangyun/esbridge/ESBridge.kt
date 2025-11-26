package name.yangyun.esbridge

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.suspendCoroutine

/**
 * 用于注册供JS调用的Android挂起函数
 */
typealias Handler = suspend (dict: JSONObject) -> JSONObject?
/**
 * 用于注册供JS调用的Android同步函数
 */
typealias SyncHandler = (dict: JSONObject) -> JSONObject?
/**
 * 在调用JS函数后，接收返回值的回调函数
 */
typealias Callback = (result: Result<JSONObject>) -> Unit

class ESBridge(
    val webview: WebView,
    val name: String = "ESBridge"
) {
    private val sync: MutableMap<String, SyncHandler> =  mutableMapOf()
    private val async: MutableMap<String, Handler> =  mutableMapOf()
    private val callbacks: MutableMap<String, Callback> = mutableMapOf()
    private val scope = (webview.context as AppCompatActivity).lifecycleScope

    init {
        webview.addJavascriptInterface(this, name)
        webview.post {
            webview.evaluateJavascript("""
// 构建回调
$name._callbacks = {} // 用于接受从Android异步返回的数据
$name._ci=0n // 回调计数器
$name['${'$'}list']=()=>JSON.parse($name._list())

// 构建函数钩子
$name.${'$'}hooks = {} // 提供给Android调用函数
$name._hi=0n // 钩子计数器
$name._promises = {} // 用于异步返回Android的承诺
$name._pi=0n // 承诺计数器

// 构建事件流
$name.${'$'}emitter = new EventTarget()
$name.ESBridgeEvent = class ESBridgeEvent extends CustomEvent{
    constructor(type, json) {
        super(type, { detail: json });
        Object.defineProperty(this, 'detail', {
            value: JSON.parse(json),
            writable: false,
            enumerable: true,
            configurable: false
        });
    }
}
"""
            ) {
                Log.i("ESBridge", "注入执行终了 $it")
            }
        }
    }

    /**
     * 同步调用接口
     */
    @JavascriptInterface
    fun _call(name: String, json: String): String {
        Log.d("ESBridge", "同步调用： [$name]: $json")
        val handler = sync[name]
        if (handler == null) {
            Log.e("ESBridge", "同步函数不存在")
            return ""
        }

        val result = handler(JSONObject(json))
        Log.d("ESBridge", "返回：$result")
        return result.toString()
    }

    /**
     * 异步调用接口
     */
    @JavascriptInterface
    fun _suspend(name: String, json: String, callback: String) {
        Log.d("ESBridge", "异步调用： [$name]($callback): $json")

        val handler = async[name]
        if (handler == null) {
            Log.e("ESBridge", "异步函数不存在")
            return
        }

        scope.launch {
            val result: JSONObject? = handler(JSONObject(json))
            val output = if (result == null) "" else JSONObject.quote(result.toString())
            webview.post {
                webview.evaluateJavascript("${this@ESBridge.name}._callbacks['$callback']($output)") {
                    Log.d("ESBridge", "返回： $result $it")
                }
            }
        }
    }

    /**
     * 为JS提供返回Promise结果的接口
     */
    @JavascriptInterface
    fun _promise(name: String, json: String) {
        Log.d("ESBridge", "回调调用： [$name]: $json")
        val handler = callbacks[name]
        if (handler != null) {
            return handler(Result.success(JSONObject(json)))
        }

        Log.e("ESBridge", "回调函数不存在")
    }

    /**
     * 列出函数接口
     */
    @JavascriptInterface
    fun _list(): String = JSONArray(sync.keys.map { "$it()" } + async.keys.map { "async $it()" }).toString()

    /**
     * 注册一个同步函数
     */
    fun registerCall(name: String, handler: SyncHandler) {
        sync.put(name, handler)
        webview.post {
            webview.evaluateJavascript("""${this.name}['$name'] = function(dict) {
const input = JSON.stringify(dict) ?? '{}'
const output = ${this.name}._call('$name',input)
return JSON.parse(output)}""") {}
        }
    }
    fun register(name: String, handler: SyncHandler) = registerCall(name, handler)

    /**
     * 注册一个挂起函数
     */
    fun registerSuspend(name: String, handler: Handler) {
        async.put(name, handler)
        webview.post {
            webview.evaluateJavascript("""${this.name}['$name'] = async function(dict) {
const id = 'c' + this._ci++
return new Promise((resolve, reject) => {
    const input = JSON.stringify(dict) ?? '{}'
    this._callbacks[id] = resolve
    this._suspend('$name', input, id)
    setTimeout(() => reject('timeout'), 5000)
}).finally(() => delete this._callbacks[id]).then(JSON.parse)
}""") {}
        }
    }
//    fun register(name: String, handler: Handler) = registerSuspend(name, handler)

    /**
     * 调用JS已注册的函数
     */
    suspend fun call(name: String, data: JSONObject? = null): JSONObject = suspendCoroutine { continuation ->
        call(name, data) {
            continuation.resumeWith(it)
        }
    }

    /**
     * 调用JS已注册的函数
     */
    fun call(name: String, data: JSONObject? = null, callback: Callback) {
        Log.d("ESBridge", "请求JS函数：[$name]$data")
        webview.post {
            webview.evaluateJavascript("""(() => {
const result = ${this.name}.${'$'}hooks['$name'](${JSONObject.quote((data ?: "{}").toString())})
if(!(result instanceof Promise)) return JSON.stringify(result)
const id = 'p' + ${this.name}._pi++
${this.name}._promises[id]=result
return id
})()""") { raw ->
                Log.d("ESBridge", "call返回结果：$raw")
                val result = JSONTokener(raw).nextValue() as String
                if (!result.startsWith("p")) return@evaluateJavascript callback(Result.success(JSONObject(result)))

                val timeout = scope.launch {
                    delay(3000)
                    callback(Result.failure(Error("ESBridge 超时")))
                }
                callbacks.put(result) {
                    callbacks.remove(result)
                    timeout.cancel()
                    callback(it)
                }

                webview.evaluateJavascript("${this.name}._promises['$result'].then(result => ${this.name}._promise('$result', JSON.stringify(result)))") {
                    Log.v("ESBridge", "承诺处理完毕 $it")
                }
            }
        }
    }

    /**
     * 向JS分发事件
     */
    fun dispatchEvent(event: ESBridgeEvent) {
        webview.post {
            webview.evaluateJavascript("${this.name}.${'$'}emitter.dispatchEvent(new ${this.name}.ESBridgeEvent('${event.type}', ${JSONObject.quote(event.detail.toString())}))") {
                Log.v("ESBridge", "事件分发完毕")
            }
        }
    }
}

class ESBridgeEvent(
    val type: String,
    val detail: JSONObject,
)
