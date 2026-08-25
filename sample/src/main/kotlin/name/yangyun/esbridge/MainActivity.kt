package name.yangyun.esbridge

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webview = WebView(this).apply {
            settings.javaScriptEnabled = true
        }

        val bridge = ESBridge(webview)
        setContentView(webview)
        bridge.registerCall("plus") { dict ->
            val a = dict.getInt("a")
            val b = dict.getInt("b")
            JSONObject().apply { put("result", a + b) }
        }

        webview.loadUrl("example.com")

        webview.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                test(webview)
            }
        }

        bridge.registerSuspend("delay") { dict ->
            val time = dict.getLong("time")
            delay(time)
            JSONObject().apply { put("time", time) }
        }
    }
}

fun test(webview: WebView) {
    webview.evaluateJavascript("ESBridge.plus({a: 1, b: 2});") { result ->
        println(result)
    }
    webview.evaluateJavascript("ESBridge.delay({time: 1000}).then(({time}) => document.body.textContent = time)") { result ->
        println(result)
    }
}
