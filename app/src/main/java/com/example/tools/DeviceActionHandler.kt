package com.example.tools

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build

class DeviceActionHandler(private val context: Context) {

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    fun openWebsite(rawUrl: String): String {
        return try {
            val url = if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
                "https://$rawUrl"
            } else {
                rawUrl
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Successfully opened website: $url"
        } catch (e: Exception) {
            "Failed to open website: ${e.localizedMessage}"
        }
    }

    fun searchWeb(query: String): String {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            "Searching the web for '$query'"
        } catch (e: Exception) {
            openWebsite("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun toggleFlashlight(enable: Boolean): String {
        return try {
            val cm = cameraManager ?: return "Flashlight not supported on this device"
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return "No flash camera found"

            cm.setTorchMode(cameraId, enable)
            if (enable) "Flashlight turned ON" else "Flashlight turned OFF"
        } catch (e: Exception) {
            "Flashlight error: ${e.localizedMessage}"
        }
    }

    fun openApp(appName: String): String {
        val target = appName.lowercase()
        val intent = when {
            "youtube" in target -> {
                context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com"))
            }
            "map" in target -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=restaurants"))
            }
            "spotify" in target -> {
                context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
            }
            "camera" in target -> {
                Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            }
            "calc" in target -> {
                val calcIntent = Intent().apply {
                    setClassName("com.android.calculator2", "com.android.calculator2.Calculator")
                }
                calcIntent
            }
            "setting" in target -> {
                Intent(android.provider.Settings.ACTION_SETTINGS)
            }
            else -> {
                // Fallback to web search
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(appName)}"))
            }
        }

        return try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            "Opened $appName"
        } catch (e: Exception) {
            "Couldn't open $appName directly: ${e.localizedMessage}"
        }
    }

    fun getBatteryInfo(): String {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            if (level >= 0) {
                "Battery level is $level%"
            } else {
                "Battery status normal"
            }
        } catch (e: Exception) {
            "Battery status unavailable"
        }
    }
}
