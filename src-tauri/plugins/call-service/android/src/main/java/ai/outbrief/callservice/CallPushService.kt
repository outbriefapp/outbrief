package ai.outbrief.callservice

import android.os.PowerManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.TimeUnit

/**
 * Receives the pushes of `Push`. A call push names the call only (the server never sends its
 * content); the call is fetched and rung like one from the stream, even when the app was swiped away
 * or the phone dozes: a high-priority push starts the process and lets it show the full-screen call.
 */
class CallPushService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    Push.sync(this, token)
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val data = message.data
    if (data["type"] != "call") return
    val id = data["eventId"].orEmpty().ifEmpty { return }
    val config = CallConfig(this)
    if (!config.enabled) return
    val wakeLock = getSystemService(PowerManager::class.java)
      .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "outbrief:push")
    wakeLock.acquire(20_000)
    try {
      Log.i(TAG, "call $id pushed (sent ${message.sentTime}, priority ${message.priority})")
      val relayed = Push.fetchCall(config, id) ?: return
      if (relayed.optString("status") != "received" || relayed.has("handledBy")) return
      CallInbox.arrived(this, relayed)
      CallAlerts.incoming(this, config, relayed)
      // Sent before returning: the process may be stopped soon after a push was handled.
      Push.receipt(config, id, "push").get(20, TimeUnit.SECONDS)
    } catch (e: Exception) {
      Log.w(TAG, "call $id: ${e.message}")
    } finally {
      wakeLock.release()
    }
  }

  private companion object {
    const val TAG = "OutBriefPush"
  }
}
