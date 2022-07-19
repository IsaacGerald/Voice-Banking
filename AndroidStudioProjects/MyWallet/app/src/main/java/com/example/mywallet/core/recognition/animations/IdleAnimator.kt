package com.example.mywallet.core.recognition.animations

import com.example.mywallet.core.recognition.RecognitionBar


class IdleAnimator(
    private val bars: List<RecognitionBar>,
    private var floatingAmplitude: Int
) :
    BarParamsAnimator {

    private val IDLE_DURATION: Long = 1500
    private var startTimestamp: Long = 0
    private var isPlaying = false


    override fun start() {
        isPlaying = true
        startTimestamp = System.currentTimeMillis();
    }

    override fun stop() {
        isPlaying = false
    }

    override fun animate() {
        if (isPlaying) {
            update(bars);
        }
    }

    fun update(bars: List<RecognitionBar?>) {
        val currTimestamp = System.currentTimeMillis()
        if (currTimestamp - startTimestamp > IDLE_DURATION) {
            startTimestamp += IDLE_DURATION
        }
        val delta = currTimestamp - startTimestamp
        var i = 0
        for (bar in bars) {
            bar?.let { updateCirclePosition(it, delta, i) }
            i++
        }
    }

    private fun updateCirclePosition(bar: RecognitionBar, delta: Long, num: Int) {
        val angle = delta.toFloat() / IDLE_DURATION * 360f + 120f * num
        val y: Int =
            (Math.sin(Math.toRadians(angle.toDouble())) * floatingAmplitude).toInt() + bar.startY
        bar.y = y
        bar.update()
    }



}