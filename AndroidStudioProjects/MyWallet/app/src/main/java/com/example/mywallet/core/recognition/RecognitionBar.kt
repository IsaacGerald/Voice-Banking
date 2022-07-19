package com.example.mywallet.core.recognition

import android.graphics.RectF

class RecognitionBar(var x: Float, var y: Int, var height: Float, val maxHeight: Int, val radius: Float) {
    val startX: Float = x
    val startY: Int = y
    val rect: RectF = RectF(
        (x - radius),
        (y - height / 2),
        (x + radius),
        (y + height / 2)
    )

    fun update() {
        rect[(x - radius), (
                y - height / 2).toFloat(), (
                x + radius)] = (
                y + height / 2).toFloat()
    }

}



