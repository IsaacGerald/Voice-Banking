package com.example.mywallet.core.recognition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.AttributeSet
import android.view.View
import com.example.mywallet.core.recognition.animations.*
import com.example.mywallet.core.recognition.animations.TransformAnimator.OnInterpolationFinishedListener


class RecognitionProgressView  @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : View(
    context,
    attrs,
    defStyleAttr
), RecognitionListener {
    private val recognitionBars: MutableList<RecognitionBar> = ArrayList()
    private var paint: Paint? = null
    private var animator: BarParamsAnimator? = null
    private var radius: Float = 0.0f
    private var spacing = 0
    private var rotationRadius = 0
    private var amplitude = 0
    private var density = 0f
    private var isSpeaking = false
    private var animating = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionListener: RecognitionListener? = null
    private var barColor = -1
    private var barColors: IntArray? = null
    private var barMaxHeights: IntArray? = null

    /**
     * Set SpeechRecognizer which is view animated with
     */
    fun setSpeechRecognizer(recognizer: SpeechRecognizer?) {
        speechRecognizer = recognizer
        speechRecognizer!!.setRecognitionListener(this)
    }

    /**
     * Set RecognitionListener to receive callbacks from [SpeechRecognizer]
     */
    fun setRecognitionListener(listener: RecognitionListener?) {
        recognitionListener = listener
    }

    /**
     * Starts animating view
     */
    fun play() {
        startIdleInterpolation()
        animating = true
    }

    /**
     * Stops animating view
     */
    fun stop() {
        if (animator != null) {
            animator!!.stop()
            animator = null
        }
        animating = false
        resetBars()
    }

    /**
     * Set one color to all bars in view
     */
    fun setSingleColor(color: Int) {
        barColor = color
    }

    /**
     * Set different colors to bars in view
     *
     * @param colors - array with size = [.BARS_COUNT]
     */
    fun setColors(colors: IntArray?) {
        if (colors == null) return
        barColors = IntArray(BARS_COUNT)
        if (colors.size < BARS_COUNT) {
            System.arraycopy(colors, 0, barColors, 0, colors.size)
            for (i in colors.size until BARS_COUNT) {
                barColors!![i] = colors[0]
            }
        } else {
            System.arraycopy(colors, 0, barColors, 0, BARS_COUNT)
        }
    }

    /**
     * Set sizes of bars in view
     *
     * @param heights - array with size = [.BARS_COUNT],
     * if not set uses default bars heights
     */
    fun setBarMaxHeightsInDp(heights: IntArray?) {
        if (heights == null) return
        barMaxHeights = IntArray(BARS_COUNT)
        if (heights.size < BARS_COUNT) {
            System.arraycopy(heights, 0, barMaxHeights, 0, heights.size)
            for (i in heights.size until BARS_COUNT) {
                barMaxHeights!![i] = heights[0]
            }
        } else {
            barMaxHeights?.let { System.arraycopy(heights, 0, it, 0, BARS_COUNT) }
        }
    }

    /**
     * Set radius of circle
     *
     * @param radius - Default value = [.CIRCLE_RADIUS_DP]
     */
    fun setCircleRadiusInDp(radius: Int) {
        this.radius = (radius * density).toInt().toFloat()
    }

    /**
     * Set spacing between circles
     *
     * @param spacing - Default value = [.CIRCLE_SPACING_DP]
     */
    fun setSpacingInDp(spacing: Int) {
        this.spacing = (spacing * density).toInt()
    }

    /**
     * Set idle animation amplitude
     *
     * @param amplitude - Default value = [.IDLE_FLOATING_AMPLITUDE_DP]
     */
    fun setIdleStateAmplitudeInDp(amplitude: Int) {
        this.amplitude = (amplitude * density).toInt()
    }

    /**
     * Set rotation animation radius
     *
     * @param radius - Default value = [.ROTATION_RADIUS_DP]
     */
    fun setRotationRadiusInDp(radius: Int) {
        rotationRadius = (radius * density).toInt()
    }

    init {
        paint = Paint()
        paint?.flags = Paint.ANTI_ALIAS_FLAG
        paint?.color = Color.GRAY
        density = resources.displayMetrics.density
        radius = (CIRCLE_RADIUS_DP * density).toInt().toFloat()
        spacing = (CIRCLE_SPACING_DP * density).toInt()
        rotationRadius = (ROTATION_RADIUS_DP * density).toInt()
        amplitude = (IDLE_FLOATING_AMPLITUDE_DP * density).toInt()
        if (density <= MDPI_DENSITY) {
            amplitude *= 2
        }
    }


    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (recognitionBars.isEmpty()) {
            initBars()
        } else if (changed) {
            recognitionBars.clear()
            initBars()
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (recognitionBars.isEmpty()) {
            return
        }
        if (animating) {
            animator!!.animate()
        }
        for (i in recognitionBars.indices) {
            val bar = recognitionBars[i]
            if (barColors != null) {
                paint?.color = barColors!![i]
            } else if (barColor != -1) {
                paint?.color = barColor
            }
            canvas?.drawRoundRect(bar!!.rect, radius, radius, paint!!)
        }
        if (animating) {
            invalidate()
        }
    }

    private fun initBars() {
        val heights = initBarHeights()
        val firstCirclePosition: Float = measuredWidth / 2 - 2 * spacing - 4 * radius
        for (i in 0 until BARS_COUNT) {
            val x = firstCirclePosition + (2 * radius + spacing) * i
            val bar = RecognitionBar(x, measuredHeight / 2, 2 * radius, heights[i], radius)
            recognitionBars.add(bar)
        }
    }

    private fun initBarHeights(): List<Int> {
        val barHeights: MutableList<Int> = ArrayList()
        if (barMaxHeights == null) {
            for (i in 0 until BARS_COUNT) {
                barHeights.add((DEFAULT_BARS_HEIGHT_DP[i] * density).toInt())
            }
        } else {
            for (i in 0 until BARS_COUNT) {
                barHeights.add((barMaxHeights!![i] * density).toInt())
            }
        }
        return barHeights
    }

    private fun resetBars() {
        for (bar in recognitionBars) {
            bar.x = bar.startX
            bar.y = bar.startY
            bar.height = radius * 2
            bar.update()
        }
    }

    private fun startIdleInterpolation() {
        animator = IdleAnimator(recognitionBars, amplitude)
        animator?.start()
    }

    private fun startRmsInterpolation() {
        resetBars()
        animator = RmsAnimator(recognitionBars)
        animator?.start()
    }

    private fun startTransformInterpolation() {
        resetBars()
        animator =
            TransformAnimator(recognitionBars.toList(), width / 2, height / 2, rotationRadius)
        animator?.start()
        (animator as TransformAnimator).setOnInterpolationFinishedListener(object :
            OnInterpolationFinishedListener {
            override fun onFinished() {
                startRotateInterpolation()
            }
        })
    }

    private fun startRotateInterpolation() {
        animator = RotatingAnimator(recognitionBars, getWidth() / 2, getHeight() / 2)
        animator?.start()
    }

    override fun onReadyForSpeech(params: Bundle) {
        if (recognitionListener != null) {
            recognitionListener!!.onReadyForSpeech(params)
        }
    }

    override fun onBeginningOfSpeech() {
        if (recognitionListener != null) {
            recognitionListener!!.onBeginningOfSpeech()
        }
        isSpeaking = true
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (recognitionListener != null) {
            recognitionListener!!.onRmsChanged(rmsdB)
        }
        if (animator == null || rmsdB < 1f) {
            return
        }
        if (animator !is RmsAnimator && isSpeaking) {
            startRmsInterpolation()
        }
        if (animator is RmsAnimator) {
            (animator as RmsAnimator).onRmsChanged(rmsdB)
        }
    }

    override fun onBufferReceived(buffer: ByteArray) {
        if (recognitionListener != null) {
            recognitionListener!!.onBufferReceived(buffer)
        }
    }

    override fun onEndOfSpeech() {
        if (recognitionListener != null) {
            recognitionListener!!.onEndOfSpeech()
        }
        isSpeaking = false
        startTransformInterpolation()

    }

    override fun onError(error: Int) {
        if (recognitionListener != null) {
            recognitionListener!!.onError(error)
        }
    }

    override fun onResults(results: Bundle) {
        if (recognitionListener != null) {
            recognitionListener!!.onResults(results)
        }
    }

    override fun onPartialResults(partialResults: Bundle) {
        if (recognitionListener != null) {
            recognitionListener!!.onPartialResults(partialResults)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle) {
        if (recognitionListener != null) {
            recognitionListener!!.onEvent(eventType, params)
        }
    }

    companion object {
        const val BARS_COUNT = 5
        private const val CIRCLE_RADIUS_DP = 5
        private const val CIRCLE_SPACING_DP = 11
        private const val ROTATION_RADIUS_DP = 25
        private const val IDLE_FLOATING_AMPLITUDE_DP = 3
        private val DEFAULT_BARS_HEIGHT_DP = intArrayOf(60, 46, 70, 54, 64)
        private const val MDPI_DENSITY = 1.5f
    }
}