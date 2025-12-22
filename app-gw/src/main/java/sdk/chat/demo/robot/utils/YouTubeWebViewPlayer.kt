package sdk.chat.demo.robot.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import org.json.JSONObject
import java.lang.ref.WeakReference

class YouTubeWebViewPlayer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var webView: WebView
    private var currentVideoId: String? = null
    private var isPlayerReady = false

    // 播放状态监听器
    interface PlayerStateListener {
        fun onPlayerReady()
        fun onPlayerStateChanged(state: Int)
        fun onPlayerError(errorCode: Int)
        fun onPlaybackProgress(currentTime: Float, duration: Float)
    }

    private var playerStateListener: PlayerStateListener? = null

    fun setPlayerStateListener(listener: PlayerStateListener) {
        this.playerStateListener = listener
    }

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView = WebView(context)
        webView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        addView(webView)

        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // 启用硬件加速
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        }

        // 添加 JavaScript 接口
        webView.addJavascriptInterface(WebAppInterface(WeakReference(this)), "AndroidBridge")

        // 设置 WebView 客户端
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Toast.makeText(context, "加载失败: ${error?.description}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 加载 YouTube 视频
     * @param videoId YouTube 视频ID
     */
    fun loadVideo(videoId: String) {
        currentVideoId = videoId

        val html = getYouTubeHtml()
            .replace("VIDEO_ID_PLACEHOLDER", videoId)

        webView.loadDataWithBaseURL(
            "https://www.youtube.com",
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    /**
     * 播放视频
     */
    fun play() {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:playVideo()", null)
        }
    }

    /**
     * 暂停视频
     */
    fun pause() {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:pauseVideo()", null)
        }
    }

    /**
     * 停止视频
     */
    fun stop() {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:stopVideo()", null)
        }
    }

    /**
     * 跳转到指定时间
     * @param seconds 秒数
     */
    fun seekTo(seconds: Float) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:seekTo($seconds)", null)
        }
    }

    /**
     * 静音
     */
    fun mute() {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:mute()", null)
        }
    }

    /**
     * 取消静音
     */
    fun unMute() {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:unMute()", null)
        }
    }

    /**
     * 设置音量
     * @param volume 0-100
     */
    fun setVolume(volume: Int) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:setVolume($volume)", null)
        }
    }

    /**
     * 获取当前播放时间
     */
    fun getCurrentTime(callback: (Float) -> Unit) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:getCurrentTime()") { result ->
                val time = result.removeSurrounding("\"").toFloatOrNull() ?: 0f
                callback(time)
            }
        } else {
            callback(0f)
        }
    }

    /**
     * 获取视频总时长
     */
    fun getDuration(callback: (Float) -> Unit) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:getDuration()") { result ->
                val duration = result.removeSurrounding("\"").toFloatOrNull() ?: 0f
                callback(duration)
            }
        } else {
            callback(0f)
        }
    }

    /**
     * 设置播放速度
     * @param rate 播放速度 (0.25, 0.5, 1, 1.5, 2)
     */
    fun setPlaybackRate(rate: Float) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:setPlaybackRate($rate)", null)
        }
    }

    /**
     * 获取视频信息
     */
    fun getVideoData(callback: (videoId: String?, title: String?, author: String?) -> Unit) {
        if (isPlayerReady) {
            webView.evaluateJavascript("javascript:getVideoData()") { result ->
                try {
                    val json = JSONObject(result.removeSurrounding("\""))
                    val videoId = json.optString("videoId")
                    val title = json.optString("title")
                    val author = json.optString("author")
                    callback(videoId, title, author)
                } catch (e: Exception) {
                    callback(null, null, null)
                }
            }
        } else {
            callback(null, null, null)
        }
    }

    /**
     * 重新加载当前视频
     */
    fun reload() {
        currentVideoId?.let { loadVideo(it) }
    }

    /**
     * 清除 WebView
     */
    fun cleanup() {
        webView.removeJavascriptInterface("AndroidBridge")
        webView.loadData("", "text/html", "UTF-8")
        isPlayerReady = false
    }

    // WebView 的 JavaScript 接口
    private class WebAppInterface(playerRef: WeakReference<YouTubeWebViewPlayer>) {
        private val playerRef: WeakReference<YouTubeWebViewPlayer> = playerRef

        @JavascriptInterface
        fun onPlayerReady() {
            playerRef.get()?.let { player ->
                player.isPlayerReady = true
                player.playerStateListener?.onPlayerReady()
            }
        }

        @JavascriptInterface
        fun onPlayerStateChange(state: Int) {
            playerRef.get()?.let { player ->
                player.playerStateListener?.onPlayerStateChanged(state)

                // 定期获取播放进度
                if (state == 1) { // 播放中
                    player.startProgressTracking()
                } else {
                    player.stopProgressTracking()
                }
            }
        }

        @JavascriptInterface
        fun onPlayerError(errorCode: Int) {
            playerRef.get()?.let { player ->
                player.playerStateListener?.onPlayerError(errorCode)
            }
        }
    }

    private var progressTrackingJob: android.os.Handler? = null

    private fun startProgressTracking() {
        stopProgressTracking()

        val handler = android.os.Handler()
        progressTrackingJob = handler

        val runnable = object : Runnable {
            override fun run() {
                getCurrentTime { currentTime ->
                    getDuration { duration ->
                        playerStateListener?.onPlaybackProgress(currentTime, duration)
                    }
                }
                handler.postDelayed(this, 1000) // 每秒更新一次
            }
        }

        handler.post(runnable)
    }

    private fun stopProgressTracking() {
        progressTrackingJob?.removeCallbacksAndMessages(null)
        progressTrackingJob = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopProgressTracking()
        cleanup()
    }

    companion object {
        private fun getYouTubeHtml(): String {
            return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { margin: 0; padding: 0; background: #000; }
                    #player { width: 100%; height: 100%; }
                </style>
            </head>
            <body>
                <div id="player"></div>
                <script>
                    var player;
                    
                    function onYouTubeIframeAPIReady() {
                        player = new YT.Player('player', {
                            height: '100%',
                            width: '100%',
                            videoId: 'VIDEO_ID_PLACEHOLDER',
                            playerVars: {
                                'playsinline': 1,
                                'controls': 1,
                                'rel': 0,
                                'modestbranding': 1,
                                'showinfo': 0
                            },
                            events: {
                                'onReady': onPlayerReady,
                                'onStateChange': onPlayerStateChange,
                                'onError': onPlayerError
                            }
                        });
                    }
                    
                    // JavaScript 函数定义...
                </script>
                <script src="https://www.youtube.com/iframe_api"></script>
            </body>
            </html>
            """.trimIndent()
        }
    }
}