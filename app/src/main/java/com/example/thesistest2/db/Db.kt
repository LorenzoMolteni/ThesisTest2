package com.example.thesistest2.db

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.json.JSONArray
import org.json.JSONObject

object Db {
    private val TAG = "MyDbManager"
    fun createUserIfNotExists(){
        val auth = Firebase.auth
        val user = auth.currentUser
        if(user == null)
            return
        Log.d(TAG, "currentUser: $user")
        val db = Firebase.firestore
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener {
                if(!it.exists()){
                    //user does not exist, create entry
                    Log.d(TAG, "Creating user ${user.email}")
                    db.collection("users").document(user.uid).set(
                        hashMapOf(
                            "email" to user.email
                        )
                    )
                }
                else {
                    //user already exists
                    Log.d(TAG, "Already existing user ${user.email}")
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "Error getting user ${user.uid}, exception: $it")
            }
    }

    fun incrementClicksOnOpen(){
        val uid = Firebase.auth.currentUser?.uid
        if(uid != null){
            Firebase.firestore.collection("users").document(uid).update("clicksOnOpen", FieldValue.increment(1))
        }
        else {
            Log.e(TAG, "User is no more logged")
        }
    }
    fun incrementClicksOnBack(){
        val uid = Firebase.auth.currentUser?.uid
        if(uid != null){
            Firebase.firestore.collection("users").document(uid).update("clicksOnBack", FieldValue.increment(1))
        }
        else {
            Log.e(TAG, "User is no more logged")
        }
    }

    fun saveStatistics(
        lastNudgeDates: Array<String>,
        lastCurrentDate: String,
        millisFacebook: String,
        millisInstagram: String,
        millisTiktok: String,
        tiktokConstant: Int,
        dailyFacebookPulls: String,
        dailyInstagramPulls: String,
        dailyTiktokPulls: String,
        importantFacebookEvents: MutableList<Pair<String, Long>>,
        importantInstagramEvents: MutableList<Pair<String, Long>>,
        importantTiktokEvents: MutableList<Pair<String, Long>>
    ) {
        val uid = Firebase.auth.currentUser?.uid
        if(uid != null){
            //TODO change query
            val lastNudgeDateFacebook = lastNudgeDates[0]
            val lastNudgeDateInstagram = lastNudgeDates[1]
            val lastNudgeDateTiktok = lastNudgeDates[2]

            Firebase.firestore.collection("users").document(uid).set(
                hashMapOf(
                    "lastNudgeDateFacebook" to lastNudgeDateFacebook,
                    "lastNudgeDateInstagram" to lastNudgeDateInstagram,
                    "lastNudgeDateTiktok" to lastNudgeDateTiktok,
                    "lastCurrentDate" to lastCurrentDate,
                    "millisFacebook" to millisFacebook,
                    "millisInstagram" to millisInstagram,
                    "millisTiktok" to millisTiktok,
                    "tiktokConstant" to tiktokConstant,
                    "dailyFacebookPulls" to dailyFacebookPulls,
                    "dailyInstagramPulls" to dailyInstagramPulls,
                    "dailyTiktokPulls" to dailyTiktokPulls,
                ), SetOptions.merge()
            )
            //save list of important events in a collection. Created needs to disambiguate in case of multiple calls on the same
            val jsonFacebookEvents = JSONArray()
            importantFacebookEvents.forEach { event ->
                jsonFacebookEvents.put(
                    JSONObject().apply {
                        put("eventType", event.first)
                        put("timestamp", event.second)
                    }
                )
            }
            val jsonInstagramEvents = JSONArray()
            importantInstagramEvents.forEach { event ->
                jsonInstagramEvents.put(
                    JSONObject().apply {
                        put("eventType", event.first)
                        put("timestamp", event.second)
                    }
                )
            }
            val jsonTiktokEvents = JSONArray()
            importantTiktokEvents.forEach { event ->
                jsonTiktokEvents.put(
                    JSONObject().apply {
                        put("eventType", event.first)
                        put("timestamp", event.second)
                    }
                )
            }
            Firebase.firestore.collection("users").document(uid).collection("importantFacebookEvents").document().set(
                hashMapOf(
                    "date" to lastCurrentDate,
                    "created" to  FieldValue.serverTimestamp(),
                    "events" to jsonFacebookEvents.toString()
                )
            )
            Firebase.firestore.collection("users").document(uid).collection("importantInstagramEvents").document().set(
                hashMapOf(
                    "date" to lastCurrentDate,
                    "created" to  FieldValue.serverTimestamp(),
                    "events" to jsonInstagramEvents.toString()
                )
            )
            Firebase.firestore.collection("users").document(uid).collection("importantTiktokEvents").document().set(
                hashMapOf(
                    "date" to lastCurrentDate,
                    "created" to  FieldValue.serverTimestamp(),
                    "events" to jsonTiktokEvents.toString()
                )
            )
        }
        else {
            Log.e(TAG, "User is no more logged")
        }
    }
}