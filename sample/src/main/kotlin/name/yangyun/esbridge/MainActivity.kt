package name.yangyun.esbridge

import android.graphics.Bitmap
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.delay
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webview = WebView(this)
        val bridge = ESBridge(webview)
        setContentView(webview)
        webview.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                bridge.onPageStarted()
            }
        }

        bridge.registerCall("plus") { dict ->
            val a = dict.optInt("a")
            val b = dict.optInt("b")
            JSONObject().put("result", a + b)
        }

        bridge.registerSuspend("delay") { dict ->
            val time = dict.optLong("time")
            delay(time)
            JSONObject("")
        }
    }
}