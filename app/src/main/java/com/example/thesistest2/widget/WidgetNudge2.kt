package com.example.thesistest2.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.example.thesistest2.R
import de.hdodenhof.circleimageview.CircleImageView
import kotlin.random.Random

@SuppressLint("StaticFieldLeak")
object WidgetNudge2 {
    private val TAG = "MYWIDGETNUDGE2"

    //Random
    private val rand = Random(654321)
    private var frameLayout: FrameLayout? = null
    private var frameLayoutTextualNudge: FrameLayout? = null
    private var isScroll: Boolean? = null
    private var type: Int? = null
    private var currentTextualNudge: String? = null
    private var isTextualNudgeShown: Boolean = false

    fun fillWidget(context: Context, isScroll: Boolean, type: Int) {
        if(frameLayout == null){
            val mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            frameLayout = FrameLayout(context)
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.widget_nudge_2, frameLayout)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, //can use also TYPE_APPLICATION_OVERLAY
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            //Specify the view position -> starting from bottom right, move up of y and left of x
            params.gravity =
                Gravity.BOTTOM or Gravity.RIGHT
            params.x = context.resources.getDimension(R.dimen.widget_margin_end).toInt()
            params.y = context.resources.getDimension(R.dimen.widget_margin_bottom).toInt()

            setImageResource(context, isScroll, type)

            mWindowManager.addView(frameLayout, params)
        }
        else {
            setImageResource(context, isScroll, type)
            frameLayout?.visibility = View.VISIBLE
        }
    }

    private fun setImageResource(context: Context, isScroll: Boolean, type: Int){
        if(this.isScroll == isScroll && this.type == type){
            //image won't change, return and don't waste cpu cycles
            return
        }
        //hide textual nudge since image is changing
        hideTextualNudge()

        //update state variables
        this.isScroll = isScroll
        this.type = type

        //generate random number to decide the string to get from resources
        val rand = rand.nextInt(0, 2) // 0 <= rand <= 1

        //change image depending on isScroll and type
        val image = frameLayout!!.findViewById<CircleImageView>(R.id.widget_image)
        if(isScroll){
            //type = 0 -> normal, 1 -> fast, 2 -> really fast
            when(type){
                0 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_low_1, context.theme))
                    this.currentTextualNudge = null         //when showing scrolling low, disable textual nudge
                }
                1 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_mid_1, context.theme))
                    //get textual nudge from resources depending on random number
                    if(rand == 0)
                        this.currentTextualNudge = context.getString(R.string.scroll_nudge2_1_fast)
                    else this.currentTextualNudge = context.getString(R.string.scroll_nudge2_2_fast)
                }
                else -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_scrolling_high_1, context.theme))
                    //get textual nudge from resources depending on random number
                    if(rand == 0)
                        this.currentTextualNudge = context.getString(R.string.scroll_nudge2_1_really_fast)
                    else this.currentTextualNudge = context.getString(R.string.scroll_nudge2_2_really_fast)
                }
            }
        }
        else {
            //type = 0 -> normal, 1 -> frequent, 2 -> really frequent
            when(type){
                0 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_low_1, context.theme))
                }
                1 -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_mid_1, context.theme))
                }
                else -> {
                    image.setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.nudge2_pulling_high_1, context.theme))
                }
            }
            //in case of pull, textual nudges do not depend on type (normal, frequent or really frequent)
            //get textual nudge from resources depending on random number
            if(rand == 0)
                this.currentTextualNudge = context.getString(R.string.pull_nudge2_1)
            else this.currentTextualNudge = context.getString(R.string.pull_nudge2_2)
        }

        //when clicking on the widget, the explanation is shown/hidden
        image.setOnClickListener {
            if(this.isTextualNudgeShown)
                hideTextualNudge()
            else
                showTextualNudge(context)
        }


    }

    private fun showTextualNudge(context: Context) {
        if(this.currentTextualNudge == null)
            return

        if(frameLayoutTextualNudge == null){
            val mWindowManager = context.getSystemService(WINDOW_SERVICE) as WindowManager
            frameLayoutTextualNudge = FrameLayout(context)
            val inflater = LayoutInflater.from(context)
            inflater.inflate(R.layout.widget_nudge_2_text, frameLayoutTextualNudge)

            updateTextualNudge()

            //hide textual nudge when clicked
            val layout = frameLayoutTextualNudge!!.findViewById<RelativeLayout>(R.id.layout_textual_nudge)
            layout.setOnClickListener { hideTextualNudge() }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, //can use also TYPE_APPLICATION_OVERLAY
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )
            //Specify the view position -> starting from bottom right, move up of y and left of x
            params.gravity =
                Gravity.BOTTOM or Gravity.RIGHT
            params.x = context.resources.getDimension(R.dimen.textual_widget_margin_end).toInt()
            params.y = context.resources.getDimension(R.dimen.textual_widget_margin_bottom).toInt()

            mWindowManager.addView(frameLayoutTextualNudge, params)
        }
        else {
            updateTextualNudge()

            //hide textual nudge when clicked
            val layout = frameLayoutTextualNudge!!.findViewById<RelativeLayout>(R.id.layout_textual_nudge)
            layout.setOnClickListener { hideTextualNudge() }

            //show text
            frameLayoutTextualNudge?.visibility = View.VISIBLE
        }
        this.isTextualNudgeShown = true
    }

    private fun updateTextualNudge(){
        val textView = frameLayoutTextualNudge!!.findViewById<TextView>(R.id.widget_textual_nudge)
        textView.text = this.currentTextualNudge
    }

    private fun hideTextualNudge () {
        this.isTextualNudgeShown = false
        frameLayoutTextualNudge?.visibility = View.GONE
    }

    fun hideWidget() {
        this.isTextualNudgeShown = false
        frameLayout?.visibility = View.GONE
        frameLayoutTextualNudge?.visibility = View.GONE
    }
}