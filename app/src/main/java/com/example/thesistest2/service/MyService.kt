package com.example.thesistest2.service

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.thesistest2.R
import com.example.thesistest2.activity.MainActivity


class MyService : AccessibilityService() {
    private val TAG = "MYSERVICE"                   //debug
    private var lastOpenedApp = ""
    private val CHANNEL_ID = "NudgeApp"             //notification
    private val ONGOING_NOTIFICATION_ID = 1111      //notification


    //this packageNames are already set in accessibility_service_config file, it's a double check
    private val packageNamesSocialNetworks =
        listOf(
            "com.facebook.katana", //facebook
            "com.instagram.android", //instagram
            "com.zhiliaoapp.musically" //tiktok
        )


    //onCreate is called before onServiceConnected
    override fun onCreate() {
        super.onCreate()

        //creating and displaying notification
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE)
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getText(R.string.notification_title))
            .setContentText(getText(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker(getText(R.string.notification_ticker_text))
            .build()

        createNotificationChannel()

        // Notification ID cannot be 0.
        startForeground(ONGOING_NOTIFICATION_ID, notification)

    }
    override fun onServiceConnected() {
        Log.d(TAG, "onServiceConnected")
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        var packageName = event.packageName
        var eventType = event.eventType

        if(eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && packageNamesSocialNetworks.contains(packageName)) {
            //that's the right event (scrolling) and the right app
            var isSwipeUp = event.scrollDeltaY > 0
            if(isSwipeUp)
                Log.d(TAG, "Scrolling")
            else
                Log.d(TAG, "pulling")
        }
        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            if(packageName != lastOpenedApp){
                //opening a new app
                lastOpenedApp = packageName.toString()
                Log.d(TAG, "Opening app ${packageName}")

                //start activity
                val intent = Intent(applicationContext, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.putExtra("test", "test")
                this.startActivity(intent)

            }
        }
    }


    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

    }



}