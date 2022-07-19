package com.example.mywallet.core.recognition.animations

import com.example.mywallet.core.recognition.RecognitionBar


class RmsAnimator(recognitionBars: List<RecognitionBar?>) :
    BarParamsAnimator {
    private val barAnimators: MutableList<BarRmsAnimator>

    override fun start() {
        for (barAnimator in barAnimators) {
            barAnimator.start()
        }
    }

    override fun stop() {
        for (barAnimator in barAnimators) {
            barAnimator.stop()
        }
    }

    override fun animate() {
        for (barAnimator in barAnimators) {
            barAnimator.animate()
        }
    }

    fun onRmsChanged(rmsDB: Float) {
        for (barAnimator in barAnimators) {
            barAnimator.onRmsChanged(rmsDB)
        }
    }

    init {
        barAnimators = ArrayList()
        for (bar in recognitionBars) {
            barAnimators.add(bar?.let { BarRmsAnimator(it) }!!)
        }
    }
}