package ai.outbrief.callservice

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Pushes that wake this phone for a call (ADR 0010 of outbrief-server). The server pushes every
 * device it has a Firebase Cloud Messaging token of; the push names the call, and
 * `CallPushService` fetches it and rings as the stream would. While 设置 → 后台来电 is on the token
 * is registered with the server, otherwise withdrawn. A build without a Firebase project (no
 * OUTBRIEF_FIREBASE_CONFIG) has no pushes.
 */
object Push {
  private const val TAG = "OutBriefPush"
  private const val PREFS = "outbrief.push"
  /** Hash of the server, device token and FCM token last registered; empty: none is. */
  private const val REGISTERED = "registered"
  private const val CONNECT_TIMEOUT_MS = 15_000

  /** Server requests off the caller's thread, one at a time, in order. */
  private val requests = Executors.newSingleThreadExecutor()

  /** This build has a Firebase project, and Firebase started with it. */
  fun available(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

  /**
   * `on`: the server pushes this phone; `pending`: not registered (yet), e.g. the server has no
   * pushes; `unavailable`: this build has none.
   */
  fun status(context: Context): String = when {
    !available(context) -> "unavailable"
    registered(context) -> "on"
    else -> "pending"
  }

  /** The token is registered with the server for the device the page last configured. */
  fun registered(context: Context): Boolean = prefs(context).getString(REGISTERED, "").orEmpty().isNotEmpty()

  /**
   * Registers this phone's push token with the server while background calls are on, or withdraws
   * it. Called after `configure` and whenever Firebase rotates the token.
   */
  fun sync(context: Context, fcmToken: String? = null) {
    val app = context.applicationContext
    if (!available(app)) return
    val config = CallConfig(app)
    if (!config.enabled) {
      withdraw(app, config)
      return
    }
    if (fcmToken != null) {
      requests.execute { register(app, config, fcmToken) }
      return
    }
    FirebaseMessaging.getInstance().token
      .addOnSuccessListener { token -> requests.execute { register(app, config, token) } }
      .addOnFailureListener { e -> Log.w(TAG, "no push token: ${e.message}") }
  }

  private fun register(context: Context, config: CallConfig, fcmToken: String) {
    // For pushing this phone by hand: `adb shell setprop log.tag.OutBriefPush DEBUG`.
    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "push token $fcmToken")
    val key = fingerprint(config.serverUrl, config.token, fcmToken)
    if (prefs(context).getString(REGISTERED, "") == key) return
    val body = JSONObject().put("provider", "fcm").put("token", fcmToken)
    val code = request(config, "PUT", "/v1/devices/me/push", body)
    if (code == 204) {
      prefs(context).edit().putString(REGISTERED, key).apply()
      Log.i(TAG, "push token registered")
    } else {
      // 409 push_unavailable: the server sends no pushes; calls reach this phone on its stream.
      Log.w(TAG, "push token not registered: HTTP $code")
    }
  }

  private fun withdraw(context: Context, config: CallConfig) {
    if (!registered(context)) return
    prefs(context).edit().remove(REGISTERED).apply()
    if (config.serverUrl.isEmpty() || config.token.isEmpty()) return
    requests.execute { request(config, "DELETE", "/v1/devices/me/push", null) }
  }

  /** Tells the server this phone heard of the call, so it pushes no more for it. */
  fun receipt(config: CallConfig, eventId: String, via: String): Future<*> =
    requests.submit {
      request(config, "POST", "/v1/events/$eventId/receipt", JSONObject().put("via", via))
    }

  /** The call `id` as the stream delivers it; null when the server does not have it. */
  fun fetchCall(config: CallConfig, id: String): JSONObject? {
    val conn = open(config, "GET", "/v1/events/$id")
    try {
      if (conn.responseCode != 200) {
        Log.w(TAG, "call $id: HTTP ${conn.responseCode}")
        return null
      }
      return JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
    } finally {
      conn.disconnect()
    }
  }

  /** The response code; -1 when the server could not be reached. */
  private fun request(config: CallConfig, method: String, path: String, body: JSONObject?): Int {
    return try {
      val conn = open(config, method, path)
      try {
        if (body != null) {
          conn.doOutput = true
          conn.setRequestProperty("Content-Type", "application/json")
          conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        conn.responseCode
      } finally {
        conn.disconnect()
      }
    } catch (e: Exception) {
      Log.w(TAG, "$method $path: ${e.message}")
      -1
    }
  }

  private fun open(config: CallConfig, method: String, path: String): HttpURLConnection {
    val conn = URL(config.serverUrl + path).openConnection() as HttpURLConnection
    conn.requestMethod = method
    conn.connectTimeout = CONNECT_TIMEOUT_MS
    conn.readTimeout = CONNECT_TIMEOUT_MS
    conn.setRequestProperty("Authorization", "Bearer ${config.token}")
    return conn
  }

  private fun fingerprint(vararg parts: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(parts.joinToString("\n").toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun prefs(context: Context) =
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
