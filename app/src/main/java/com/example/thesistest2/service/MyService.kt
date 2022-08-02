package com.example.thesistest2.service

import android.accessibilityservice.AccessibilityService
import android.app.*
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import com.example.thesistest2.R
import com.example.thesistest2.activity.MainActivity
import com.example.thesistest2.activity.ActivityNudge1
import com.example.thesistest2.widget.WidgetNudge2
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.random.Random.Default.nextInt


class MyService : AccessibilityService() {
    private val TAG = "MYSERVICE"                   //debug
    private val CHANNEL_ID = "NudgeApp"             //notification
    private val ONGOING_NOTIFICATION_ID = 1111      //notification
    private var isThisServiceRunning = false        //debugging
    private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
    private var currentDate: String = ""

    //VARIABLES FOR NUDGE 1
    //this is the minimum time that has to pass between two nudges to be displayed
    private val minMillisInterval = 60000    //1 min debugging       // 1 hour = 3600000
    //this is the minimum time that a user has to spent on a social network before the nudge is shown
    private val minMillisSpent = 30000       //30sec debugging       //30 min  = 1800000
    //this is used for detecting when an app is reopened
    private val thresholdValueAppOpening = 120000                              //2min = 120000 millis

    private var lastOpenedApp = ""
    private var lastOpeningTimestamp: Long = 0
    private var lastInteractionTimestamp: Long = 0
    private var lastNudgeDisplayedTimestamp: Long = 0

    //these arrays contain info for social -> Facebook at pos 0, Instagram at pos 1, TikTok at pos 2
    //values are updated when the service recognizes that a new app is opened, that the same has been opened from bg, or before being killed
    private var dailyMillisSpent: Array<Long> = arrayOf(0, 0, 0)
    private var averageMillisSpent: MutableList<Long> = mutableListOf(0, 0, 0)
    private var lastNudgeDate: Array<String> = arrayOf("", "", "")



    //VARIABLES FOR NUDGE 2
    private var screenHeight: Int = 0
    private val minSwipeLength = 100        //ignore small scrolls/pulls
    private val minScrollsToEnableNudge: Int = 10       //after 10 scrolls, start showing nudge if necessary
    private val scrollingTimeWindow: Int = 30000        //time window of 30sec
    private val scrollingThresholdReallyFast = 10       //10 scrolls in time window are considered really fast
    private val scrollingThresholdFast = 5              //5 scrolls in time window are considered fast
    private val maxDurationWidget: Long = 3000                //3seconds of max duration for widget scrolling low
    private var startedScrolling = false
    private var scrollingNudgeEnabled = false
    private var gestureAmountScrolled: Int = 0
    private var lastTimestampScrollingLow : Long = Long.MIN_VALUE
    private val scrollingEvents: MutableList<Pair<Long, Int>> = mutableListOf()         //list containing time and length of scroll
    private var debugPxScrolled : Int = 0
    private var startedPulling = false

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

        //set display metrics

        val display = (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay
        screenHeight = display.height
        //screenHeight = displayMetrics?.heightPixels ?: 0

        Log.d(TAG, "Screen height: $screenHeight")

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isThisServiceRunning = true
        currentDate = LocalDateTime.now().format(formatter)

        //load info from shared preferences
        loadData()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isThisServiceRunning = false
        Log.d(TAG, "onUnbind")

        //service is going to be killed. Before this happen, update dailyMillis for the last opened app
        if (lastOpenedApp!= ""){
            val index = packageNamesSocialNetworks.indexOf(lastOpenedApp)
            dailyMillisSpent[index] += (lastInteractionTimestamp - lastOpeningTimestamp)
        }

        persistData()
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val date = LocalDateTime.now().format(formatter)
        val packageName = event.packageName
        val eventType = event.eventType
        val eventTime = event.eventTime
        val index = packageNamesSocialNetworks.indexOf(packageName)
        if(index == -1){
            Log.e(TAG, "Event arrived from wrong app")
            return
        }

        //IMPORTANT NOTE: event.eventTime is relative to boot time and does not include sleep time
        //this is not important for this app purpose, since we need only the time passed between two events, that is the same

        if(eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val isSwipeUp = event.scrollDeltaY > 0
            val length = event.scrollDeltaY
            debugPxScrolled += length

            //update array
            if(isSwipeUp){
                //ignore small scrolls
                startedScrolling = length >= minSwipeLength
                if (startedScrolling){
                    //update amount scrolled
                    gestureAmountScrolled += length
                }
                else {
                    //the amount scrolled is becoming less than threshold -> suppose the gesture is ending
                    if(gestureAmountScrolled > 0){
                        Log.d(TAG, "Amount scrolled: $gestureAmountScrolled")
                        scrollingEvents.add(Pair(eventTime, gestureAmountScrolled))
                        if(scrollingEvents.size >= minScrollsToEnableNudge){
                            //app has collected many scrolling events and can start displaying nudge if necessary
                            Log.d(TAG, "Enabling nudge")
                            scrollingNudgeEnabled = true
                        }

                        if(scrollingNudgeEnabled){
                            //check if a nudge has to be displayed
                            //consider a time window of scrollingTimeWindow
                            var count = countScrollsInTimeWindow(eventTime)

                            if(count >= scrollingThresholdReallyFast){
                                //show widget for really fast
                                lastTimestampScrollingLow = Long.MIN_VALUE
                                WidgetNudge2.fillWidget(this, true, 2)
                            }
                            else if (count >= scrollingThresholdFast){
                                lastTimestampScrollingLow = Long.MIN_VALUE
                                WidgetNudge2.fillWidget(this, true, 1)
                            }
                            else if(lastTimestampScrollingLow == Long.MIN_VALUE){
                                //set widget for scrolling low only if it is not already set
                                lastTimestampScrollingLow = eventTime
                                WidgetNudge2.fillWidget(this, true, 0)
                                MainScope().launch {
                                    delay(maxDurationWidget)
                                    //hide widget only if still present
                                    if(lastTimestampScrollingLow != Long.MIN_VALUE)
                                        WidgetNudge2.hideWidget()
                                }
                            }
                        }

                    }
                    gestureAmountScrolled = 0
                }

            }
            else{
                //Log.d(TAG, "Pulling of ${event.scrollDeltaY}")
            }

        }
        if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            if(packageName != lastOpenedApp){
                //opening a new app
                if (lastOpenedApp != "") {
                    //update information about the usage of the last opened app
                    dailyMillisSpent[index] += (lastInteractionTimestamp - lastOpeningTimestamp)
                    Log.d(TAG, "updated info for $lastOpenedApp, daily millis = ${dailyMillisSpent[index]}")
                }

                //reset info
                lastOpenedApp = packageName.toString()
                lastOpeningTimestamp = eventTime
                lastInteractionTimestamp = eventTime

                Log.d(TAG, "Opening app ${packageName} at time ${lastOpeningTimestamp}")

                val nudge = displayNudgeOneForApp(packageName.toString(), eventTime)
                if(nudge != null) {
                    //start activity
                    val intent = Intent(applicationContext, ActivityNudge1::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.putExtra("nudge", nudge)
                    this.startActivity(intent)
                }
            }
        }
        if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            //this is useful in case of videos, because window content changes without user interaction
            if(lastOpenedApp == packageName){
                if(eventTime - lastInteractionTimestamp > thresholdValueAppOpening){
                    //this event has arrived after 2 minutes of inactivity from the previously opened app
                    // -> suppose that app has been reopened from background
                    dailyMillisSpent[index] += (lastInteractionTimestamp - lastOpeningTimestamp)
                    Log.d(TAG, "App $packageName has been reopened, updated daily millis = ${dailyMillisSpent[index]}")

                    //reset info
                    lastOpeningTimestamp = eventTime
                    lastInteractionTimestamp = eventTime

                    val nudge = displayNudgeOneForApp(packageName.toString(), eventTime)
                    if(nudge != null) {
                        //start activity
                        val intent = Intent(applicationContext, ActivityNudge1::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        intent.putExtra("nudge", nudge)
                        this.startActivity(intent)
                    }
                }
                else
                    lastInteractionTimestamp = eventTime
            }
        }


        //date of the new event is after current date
        if(currentDate != date){
            //save data in shared preferences
            persistData()       //this function also updates averageMillisSpent

            //reset all data and change currentDate
            currentDate = date
            lastOpenedApp = ""
            lastOpeningTimestamp = 0
            lastInteractionTimestamp = 0
            lastNudgeDisplayedTimestamp = 0
            dailyMillisSpent = arrayOf(0, 0, 0)
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

    private fun displayNudgeOneForApp(packageName:String, eventTime: Long) : String? {
        val index = packageNamesSocialNetworks.indexOf(packageName)
        val rand = nextInt(0,3) // 0 <= rand <= 3
        val now = LocalDateTime.now()
        val today = now.format(formatter)

        //nudge is not displayed if it has already been displayed, even if for another app, within the previous 60 minutes
        if (lastNudgeDisplayedTimestamp > 0 && eventTime - lastNudgeDisplayedTimestamp < minMillisInterval) {
            Log.d(TAG, "Nudge1 not displayed because already displayed in previous $minMillisInterval millis")
            return null
        }

        //last nudge date == today -> Nudge 1 can appear at most once a day per each application
        if(lastNudgeDate[index] == today) {
            Log.d(TAG, "Nudge1 has already been displayed today for $packageName")
            return null
        }

        //Nudge 1 is displayed at most twice a day
        var count = 0
        lastNudgeDate.forEach { if(it == today) count++ }
        if(count > 1) {
            Log.d(TAG, "Nudge1 has already been displayed $count today")
            return null
        }

        //Nudge 1 is displayed for application X, only if the user has already spent more than 30 minutes inside the social network X that day
        if(dailyMillisSpent[index] < minMillisSpent) {
            Log.d(TAG, "User has spent less than $minMillisSpent millis today on $packageName")
            return null
        }

        //When entering one of the targeted apps, the probability that Nudge 1 is shown is 25%.
        //If the user has spent more time than his daily average on the social networks in consideration,
        //the probability that Nudge 1 is shown is increased to 50%.
        if(dailyMillisSpent[index] > averageMillisSpent[index]){
            //50% probability
            if(rand != 1 && rand != 2){
                //0<rand<4 -> suppose rand == 1 || rand == 2 is the condition for which the nudge is shown
                Log.d(TAG, "P = 50% but Nudge1 not shown because rand = $rand")
                return null
            }
        }
        else {
            //25% probability
            if(rand != 1){
                //0<rand<4 -> suppose rand == 1 is the condition for which the nudge is shown
                Log.d(TAG, "P = 25% but Nudge1 not shown because rand = $rand")
                return null
            }
        }

        /*if arrived here, all conditions are met
        * I can chose among 4 different textual nudges (2 for scrolling and 2 for pull to refresh)
        * they are scroll_nudge1_1_facebook/instagram/tiktok, scroll_nudge1_2, pull_nudge1_1_facebook/instagram/tiktok, pull_nudge1_2
        * the id of the string to retrieve depends also on the social in consideration
         */

        lastNudgeDisplayedTimestamp = eventTime         //update the timestamp of the last shown nudge
        lastNudgeDate[index] = today                    //set that today the nudge has been displayed for the app
        val rand2 = nextInt(0,3)              // generate a new random to choose the textual nudge 0 <= rand2 <= 3

        Log.d(TAG, "Nudge1 displayed. rand = $rand, rand2 = $rand2, count = $count")

        if(rand2 == 1)
            return resources.getString(R.string.scroll_nudge1_2) //this textual nudge do not depend on the social

        if(rand2 == 3)
            return resources.getString(R.string.pull_nudge1_2)  //this textual nudge do not depend on the social

        //here rand == 0 || rand == 2
        return when(index){
            0 -> {
                if(rand == 0)
                    resources.getString(R.string.scroll_nudge1_1_facebook)
                else
                    resources.getString(R.string.pull_nudge1_1_facebook)
            }
            1 -> { //instagram
                if(rand == 0)
                    resources.getString(R.string.scroll_nudge1_1_instagram)
                else
                    resources.getString(R.string.pull_nudge1_1_instagram)
            }
            2 -> { //tiktok
                if(rand == 0)
                    resources.getString(R.string.scroll_nudge1_1_tiktok)
                else
                    resources.getString(R.string.pull_nudge1_1_tiktok)
            }
            else -> null //error case, should not happen
        }
    }

    private fun countScrollsInTimeWindow(eventTime: Long): Int {
        var count = 0
        var pixelsScrolled = 0
        //iterate list backwards to avoid exceptions
        for(i in scrollingEvents.indices.reversed()) {
            if(eventTime - scrollingEvents[i].first > scrollingTimeWindow){
                //this scrolling event s is out of timewindow
                scrollingEvents.removeAt(i)
            }
            else {
                //count amount scrolled
                count++
                pixelsScrolled += scrollingEvents[i].second
            }
        }
        Log.d(TAG, "ScrollingEvents.size = ${scrollingEvents.size} -> pixelsScrolled: $pixelsScrolled")

        return count
    }

    //PERFORMED ONCE A DAY, WHEN THE FIRST ACCESSIBILITY_EVENT OF THE DAY IS FIRED
    //OR IN CASE OF UNBIND
    private fun persistData() {
        //get shared preferences
        val sharedPref = getSharedPreferences(getString(R.string.system_preference_file_key), Context.MODE_PRIVATE)

        //LOAD NEW AVERAGE MILLIS
        //get arrays with millis from preferences
        val millisSpentFacebook = sharedPref.getString("millisSpentFacebook", "")
        val millisSpentInstagram = sharedPref.getString("millisSpentInstagram", "")
        val millisSpentTiktok = sharedPref.getString("millisSpentTiktok", "")
        val millisFacebook: MutableList<Long>
        val millisInstagram: MutableList<Long>
        val millisTiktok: MutableList<Long>

        //get lastCurrentDate
        val lastCurrentDate = sharedPref.getString("lastCurrentDate", "")

        //facebook
        if(millisSpentFacebook.isNullOrBlank()){
            millisFacebook = mutableListOf(dailyMillisSpent[0])
            averageMillisSpent[0] = dailyMillisSpent[0]
        }
        else {
            millisFacebook = millisSpentFacebook.split(";").map { it.toLong() }.toMutableList()
            //check if this is the second write of the day
            if(lastCurrentDate == currentDate){
                //today data was already persisted, need to remove last element
                millisFacebook.removeLast()
            }
            millisFacebook.add(dailyMillisSpent[0])
            averageMillisSpent[0] = millisFacebook.average().toLong()
        }
        //instagram
        if(millisSpentInstagram.isNullOrBlank()){
            millisInstagram = mutableListOf(dailyMillisSpent[1])
            averageMillisSpent[1] = dailyMillisSpent[1]
        }
        else {
            millisInstagram = millisSpentInstagram.split(";").map { it.toLong() }.toMutableList()
            //check if this is the second write of the day
            if(lastCurrentDate == currentDate){
                //today data was already persisted, need to remove last element
                millisInstagram.removeLast()
            }
            millisInstagram.add(dailyMillisSpent[1])
            averageMillisSpent[1] = millisInstagram.average().toLong()
        }
        //tiktok
        if(millisSpentTiktok.isNullOrBlank()){
            millisTiktok = mutableListOf(dailyMillisSpent[2])
            averageMillisSpent[2] = dailyMillisSpent[2]
        }
        else {
            millisTiktok = millisSpentTiktok.split(";").map { it.toLong() }.toMutableList()
            //check if this is the second write of the day
            if(lastCurrentDate == currentDate){
                //today data was already persisted, need to remove last element
                millisTiktok.removeLast()
            }
            millisTiktok.add(dailyMillisSpent[2])
            averageMillisSpent[2] = millisTiktok.average().toLong()
        }

        //prepare strings containing millis to be written in shared preferences
        val s0 = millisFacebook.joinToString(separator = ";")
        val s1 = millisInstagram.joinToString(separator = ";")
        val s2 = millisTiktok.joinToString(separator = ";")

        //write to shared preferences
        with (sharedPref.edit()) {
            //write lastNudgeDate to sharedPreferences
            lastNudgeDate.forEachIndexed { i, it ->
                putString("lastNudgeDate$i", it)
            }

            //write last currentDate
            putString("lastCurrentDate", currentDate)

            //write millis
            putString("millisSpentFacebook", s0)
            putString("millisSpentInstagram", s1)
            putString("millisSpentTiktok", s2)
            apply()
        }

        //TODO if needed, save on Firebase
        Log.d(TAG, "Data persisted: lastNudgeDates: ${lastNudgeDate[0]} ${lastNudgeDate[1]} ${lastNudgeDate[2]}\t millis: $s0 $s1 $s2")
    }

    //USED FOR DEBUG
    private fun clearServicePreferences() {
        //get shared preferences
        val sharedPref = getSharedPreferences(getString(R.string.system_preference_file_key), Context.MODE_PRIVATE)

        //write to shared preferences
        with (sharedPref.edit()) {
            //write lastNudgeDate to sharedPreferences
            lastNudgeDate.forEachIndexed { i, it ->
                putString("lastNudgeDate$i", "")
            }
            putString("millisSpentFacebook", "")
            putString("millisSpentInstagram", "")
            putString("millisSpentTiktok", "")
            apply()
        }
    }

    //THIS FUNCTION IS CALLED FROM onServiceConnected
    private fun loadData() {
        //get shared preferences
        val sharedPref = getSharedPreferences(getString(R.string.system_preference_file_key), Context.MODE_PRIVATE)

        //LOAD lastNudgeDate from preferences
        lastNudgeDate.forEachIndexed{ i, it ->
            lastNudgeDate[i] = sharedPref.getString("lastNudgeDate$i", "") ?: ""
        }

        //get lastCurrentDate from preferences
        val lastCurrentDate = sharedPref.getString("lastCurrentDate", "")
        //get arrays with millis from preferences
        val millisSpentFacebook = sharedPref.getString("millisSpentFacebook", "")
        val millisSpentInstagram = sharedPref.getString("millisSpentInstagram", "")
        val millisSpentTiktok = sharedPref.getString("millisSpentTiktok", "")
        var millisFacebook: Long = 0
        var millisInstagram: Long = 0
        var millisTiktok: Long = 0

        //need to check if lastCurrentDate is today or not, in order to correctly update average and, in case is today, daily millis
        if(!millisSpentFacebook.isNullOrBlank()) {
            if(lastCurrentDate == currentDate){
                //update dailyMillis of Facebook and calculate average after having removed last element
                val list = millisSpentFacebook.split(";").map { it.toLong() }.toMutableList()
                dailyMillisSpent[0] = list.removeLast()
                millisFacebook = list.average().toLong()
            }
            else {
                //just calculate average, since this is a new day
                millisFacebook = millisSpentFacebook.split(";").map { it.toLong() }.toMutableList().average().toLong()
            }
        }
        if(!millisSpentInstagram.isNullOrBlank()) {
            if(lastCurrentDate == currentDate){
                val list = millisSpentInstagram.split(";").map{it.toLong()}.toMutableList()
                dailyMillisSpent[1] = list.removeLast()

                millisInstagram = list.average().toLong()
            }
            else{
                //just calculate average, since this is a new day
                millisInstagram = millisSpentInstagram.split(";").map { it.toLong() }.toMutableList().average().toLong()
            }
        }
        if(!millisSpentTiktok.isNullOrBlank()){
            if(lastCurrentDate == currentDate) {
                val list = millisSpentTiktok.split(";").map{it.toLong()}.toMutableList()
                dailyMillisSpent[2] = list.removeLast()
                Log.d(TAG, "Loaded daily millis = [${dailyMillisSpent[0]},${dailyMillisSpent[1]},${dailyMillisSpent[2]}]")

                millisTiktok = list.average().toLong()
            }
            else{
                //just calculate average, since this is a new day
                millisTiktok = millisSpentTiktok.split(";").map { it.toLong() }.toMutableList().average().toLong()
            }
        }

        averageMillisSpent[0] = millisFacebook
        averageMillisSpent[1] = millisInstagram
        averageMillisSpent[2] = millisTiktok

        Log.d(TAG, "LoadData -> dates: ${lastNudgeDate[0]}, ${lastNudgeDate[1]}, ${lastNudgeDate[2]} avgs: $averageMillisSpent")
    }

}