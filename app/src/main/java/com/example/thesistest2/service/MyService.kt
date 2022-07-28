package com.example.thesistest2.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.thesistest2.R
import com.example.thesistest2.activity.MainActivity
import com.example.thesistest2.activity.TextualNudge1
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt


class MyService : AccessibilityService() {
    private val TAG = "MYSERVICE"                   //debug
    private val CHANNEL_ID = "NudgeApp"             //notification
    private val ONGOING_NOTIFICATION_ID = 1111      //notification
    private var isThisServiceRunning = false        //debugging
    private val minMillisInterval = 60000           // 1 hour = 3600000
    private var lastOpenedApp = ""
    private var lastOpeningTimestamp: Long = 0
    private var lastInteractionTimestamp: Long = 0
    private var lastNudgeDisplayedTimestamp: Long = 0

    //these arrays contain info for social -> Facebook at pos 0, Instagram at pos 1, TikTok at pos 2
    private var dailyMillisSpent: Array<Long> = arrayOf(0, 0, 0)
    private var lastNudgeDate: Array<String> = arrayOf("", "", "")


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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isThisServiceRunning = true
        Log.d(TAG, "onServiceConnected")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isThisServiceRunning = false
        Log.d(TAG, "Service not running")
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName
        val eventType = event.eventType

        //IMPORTANT NOTE: event.eventTime is relative to boot time and does not include sleep time
        //this is not important for this app purpose, since we need only the time passed between two events, that is the same

        if(eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && packageNamesSocialNetworks.contains(packageName)) {
            //that's the right event (scrolling) and the right app
            val isSwipeUp = event.scrollDeltaY > 0
            if(isSwipeUp)
                Log.d(TAG, "Scrolling")
            else
                Log.d(TAG, "pulling")
        }
        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && packageNamesSocialNetworks.contains(packageName)){
            if(packageName != lastOpenedApp){
                //opening a new app
                if (lastOpenedApp != "") {
                    //update information about the usage of the last opened app
                    val index = packageNamesSocialNetworks.indexOf(lastOpenedApp)
                    dailyMillisSpent[index] += (lastInteractionTimestamp - lastOpeningTimestamp)
                    Log.d(TAG, "updated info for $lastOpenedApp, daily millis = ${dailyMillisSpent[index]}")
                }

                //reset info
                lastOpenedApp = packageName.toString()
                lastOpeningTimestamp = event.eventTime
                lastInteractionTimestamp = event.eventTime

                Log.d(TAG, "Opening app ${packageName} at time ${lastOpeningTimestamp}")

                val nudge = displayNudgeForApp(packageName.toString(), event.eventTime)
                if(nudge != null) {
                    //start activity
                    val intent = Intent(applicationContext, TextualNudge1::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("nudge", nudge)
                    this.startActivity(intent)
                }
            }
        }
        if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && packageNamesSocialNetworks.contains(packageName)) {
            if(lastOpenedApp == packageName)
                lastInteractionTimestamp = event.eventTime
        }
    }

    fun isThisServiceRunning() :Boolean{
        return isThisServiceRunning
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

    private fun displayNudgeForApp(packageName:String, eventTime: Long) : String? {
        val index = packageNamesSocialNetworks.indexOf(packageName)
        val rand = (Math.random() * 4).roundToInt() // 0 <= rand < 4
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val now = LocalDateTime.now()
        val today = now.format(formatter)

        Log.d(TAG, "displayNudgeForApp $rand")

        //nudge is not displayed if it has already been displayed, even if for another app, within the previous 60 minutes
        if (lastNudgeDisplayedTimestamp > 0 && lastNudgeDisplayedTimestamp-eventTime < minMillisInterval)
            return null

        //TODO continue from here
        return when(index){
            0 -> { //facebook
                if(lastNudgeDate[0] != today) //last nudge date != today
                    "Facebook"
                else
                    null
            }
            1 -> { //instagram
                "Instagram"

            }
            2 -> { //tiktok
                "TikTok"

            }
            else -> null
        }
    }

}