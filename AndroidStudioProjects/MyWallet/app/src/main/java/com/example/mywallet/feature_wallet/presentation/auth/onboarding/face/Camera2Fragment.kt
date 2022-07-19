package com.example.mywallet.feature_wallet.presentation.auth.onboarding.face

import android.Manifest
import android.app.*
import android.app.VoiceInteractor.Prompt
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.net.Uri
import android.os.*
import android.util.Log
import android.util.Size
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.mywallet.R
import com.example.mywallet.core.camera.AutoFitTextureView
import com.example.mywallet.databinding.FragmentCamera2Binding
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit


class Camera2Fragment : Fragment(R.layout.fragment_camera2) {
    private val TAG = Camera2Fragment::class.java.simpleName
    lateinit var binding: FragmentCamera2Binding
    private val EXTRA_TIMER_DURATION_SECONDS = "android.intent.extra.TIMER_DURATION_SECONDS"

    private val mTimerCountdownLabel: TextView? = null
    private var mTimerCountdownToast: Toast? = null


    private val EXTRA_USE_FRONT_FACING_CAMERA = "android.intent.extra.USE_FRONT_CAMERA"

    /**
     * Camera state: Showing camera preview.
     */
    private val STATE_PREVIEW = 0

    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private val STATE_WAITING_LOCK = 1

    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private val STATE_WAITING_PRECAPTURE = 2

    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private val STATE_WAITING_NON_PRECAPTURE = 3

    /**
     * Camera state: Picture was taken.
     */
    private val STATE_PICTURE_TAKEN = 4

    /**
     * [TextureView.SurfaceTextureListener] handles several lifecycle events on a
     * [TextureView].
     */
    private val mSurfaceTextureListener: SurfaceTextureListener = object : SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureAvailable: ")
            openCamera(width, height)
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: ")
            configureTransform(width, height)
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            Log.d(TAG, "onSurfaceTextureDestroyed: ")
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            Log.d(TAG, "onSurfaceTextureUpdated: ")
        }
    }

    /**
     * ID of the current [CameraDevice].
     */
    private var mCameraId: String? = null

    /**
     * An [AutoFitTextureView] for camera preview.
     */
    private lateinit var mTextureView: AutoFitTextureView

    /**
     * A [CameraCaptureSession] for camera preview.
     */
    private var mCaptureSession: CameraCaptureSession? = null

    /**
     * A reference to the opened [CameraDevice].
     */
    private var mCameraDevice: CameraDevice? = null

    /**
     * The [android.util.Size] of camera preview.
     */
    private var mPreviewSize: Size? = null

    /**
     * Characteristics of the current [CameraDevice]
     */
    private var mCharacteristics: CameraCharacteristics? = null

    private var mOrientationListener: OrientationEventListener? = null

    /**
     * Current device orientation in degrees.
     */
    private var mOrientation = 0

    /**
     * [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.
     */
    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            Log.d(TAG, "onOpened: ")
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraPreviewSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            Log.d(TAG, "onDisconnected: ")
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            Log.d(TAG, "onError: ")
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
            val activity: Activity? = activity
            activity?.finish()
        }
    }

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private var mBackgroundThread: HandlerThread? = null

    /**
     * A [Handler] for running tasks in the background.
     */
    private var mBackgroundHandler: Handler? = null

    /**
     * An [ImageReader] that handles still image capture.
     */
    private var mImageReader: ImageReader? = null

    private var mEnabledAutoFocus = true

    /**
     * This is the output file for our picture.
     */
    private var mFile: File? = null

    /**
     * This a callback object for the [ImageReader]. "onImageAvailable" will be called when a
     * still image is ready to be saved.
     */
    private val mOnImageAvailableListener =
        ImageReader.OnImageAvailableListener { reader ->
            Log.d(TAG, "onImageAvailable: ")
            mBackgroundHandler!!.post(ImageSaver(activity, reader.acquireNextImage(), mFile))
        }

    /**
     * [CaptureRequest.Builder] for the camera preview
     */
    private var mPreviewRequestBuilder: CaptureRequest.Builder? = null

    /**
     * [CaptureRequest] generated by [.mPreviewRequestBuilder]
     */
    private var mPreviewRequest: CaptureRequest? = null

    /**
     * The current state of camera state for taking pictures.
     *
     * @see .mCaptureCallback
     */
    private var mState = STATE_PREVIEW

    /**
     * A [Semaphore] to prevent the app from exiting before closing the camera.
     */
    private val mCameraOpenCloseLock = Semaphore(1)

    /**
     * A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.
     */
    private val mCaptureCallback: CameraCaptureSession.CaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            private fun process(result: CaptureResult) {
                Log.d(TAG, "process: ")
                when (mState) {
                    STATE_PREVIEW -> {}
                    STATE_WAITING_LOCK -> {
                        val afState = result.get(CaptureResult.CONTROL_AF_STATE)!!
                        Log.e(TAG, "afstate: $afState")
                        if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState
                        ) {
                            mEnabledAutoFocus = true
                            // CONTROL_AE_STATE can be null on some devices
                            val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                            Log.e(TAG, "aestate: $aeState")
                            if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED
                            ) {
                                mState = STATE_WAITING_NON_PRECAPTURE
                                captureStillPicture()
                            } else {
                                Log.e(TAG, "precapture")
                                runPrecaptureSequence()
                            }
                        } else if (CaptureResult.CONTROL_AF_MODE_OFF == afState) {
                            Log.e(TAG, "Autofocus disabled")
                            mEnabledAutoFocus = false
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        }
                    }
                    STATE_WAITING_PRECAPTURE -> {

                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                            mState = STATE_WAITING_NON_PRECAPTURE
                        }
                    }
                    STATE_WAITING_NON_PRECAPTURE -> {

                        // CONTROL_AE_STATE can be null on some devices
                        val aeState = result.get(CaptureResult.CONTROL_AE_STATE)
                        if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                            mState = STATE_PICTURE_TAKEN
                            captureStillPicture()
                        }
                    }
                }
            }

            override fun onCaptureProgressed(
                session: CameraCaptureSession, request: CaptureRequest,
                partialResult: CaptureResult
            ) {
                Log.d(TAG, "onCaptureProgressed: ")
                process(partialResult)
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession, request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                Log.d(TAG, "onCaptureCompleted: ")
                process(result)
            }
        }

    /**
     * A [Handler] for showing [Toast]s.
     */
    private val mMessageHandler: Handler by lazy {
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                Log.d(TAG, "handleMessage: ")
                val activity: Activity? = activity
                if (activity != null) {
                    Toast.makeText(activity, msg.obj as String, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Shows a [Toast] on the UI thread.
     *
     * @param text The message to show
     */
    private fun showToast(text: String) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        val activity: Activity? = activity
        if (activity!!.isVoiceInteraction) {
            val message = Message.obtain()
            message.obj = text
            //mSharingHandler.sendMessage(message);
            val contextUri = Uri.fromFile(mFile)
            Log.e(TAG, "PHOTO URI: $contextUri")
            Log.e(TAG, "PHOTO LOCATION: " + mFile!!.absolutePath)
            //Log.e(TAG, "showToast:" + Log.getStackTraceString(new Exception()));
            val extras = Bundle()
            extras.putParcelable("context_uri", contextUri)
            val prompt = Prompt("Here it is")
            activity.voiceInteractor.submitRequest(object :
                VoiceInteractor.CompleteVoiceRequest(prompt, extras) {
                override fun onCompleteResult(result: Bundle) {
                    super.onCompleteResult(result)
                    ///Log.d(TAG, "OnCompleteResult:" + Log.getStackTraceString(new Exception()));
                    val intent = Intent()
                    intent.action = Intent.ACTION_VIEW
                    intent.setDataAndType(Uri.parse("file://" + mFile!!.absolutePath), "image/*")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    getActivity().finish()
                    startActivity(intent)
                }
            })
        } else {
            val message = Message.obtain()
            message.obj = text
            mMessageHandler.sendMessage(message)
        }
    }

    /**
     * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal `Size`, or an arbitrary one if none were big enough
     */
    private fun chooseOptimalSize(
        choices: Array<Size>,
        width: Int,
        height: Int,
        aspectRatio: Size
    ): Size? {
        Log.d(TAG, "chooseOptimalSize: ")
        // Collect the supported resolutions that are at least as big as the preview Surface
        val bigEnough: MutableList<Size> = ArrayList()
        val w = aspectRatio.width
        val h = aspectRatio.height
        for (option in choices) {
            if (option.height == option.width * h / w && option.width >= width && option.height >= height) {
                bigEnough.add(option)
            }
        }

        // Pick the smallest of those, assuming we found any
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough, CompareSizesByArea())
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size")
            choices[0]
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d(TAG, "onViewCreated: ")
        binding = FragmentCamera2Binding.bind(view)
        mTextureView = binding.texture

        binding.picture.setOnClickListener {
            takePicture()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "onActivityCreated: ")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ), "VoiceCamera"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.d(TAG, "failed to create directory")
                activity?.finish()
            }
        }
        mOrientationListener = object : OrientationEventListener(activity?.applicationContext) {
            override fun onOrientationChanged(orientation: Int) {
                mOrientation = orientation
            }
        }
        try {
            mFile = File.createTempFile(imageFileName, ".jpg", storageDir)
            Log.e(TAG, "Photo file: " + mFile!!.absolutePath)
        } catch (e: IOException) {
            Log.e(TAG, "Error creating file: ", e)
            activity?.finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: ")
        startBackgroundThread()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (mTextureView.isAvailable) {
            openCamera(mTextureView.width, mTextureView.height)
        } else {
            mTextureView.surfaceTextureListener = mSurfaceTextureListener
        }
        if (mOrientationListener!!.canDetectOrientation()) {
            mOrientationListener!!.enable()
        }
        if (requireActivity().isVoiceInteraction) {
            startVoiceTrigger()
            //            if (isTimerSpecified()) {
//                startVoiceTimer();
//            } else {
//                startVoiceTrigger();
//            }
        }
    }

    private fun startVoiceTrigger() {
        Log.d(TAG, "startVoiceTrigger: ")
        val option = VoiceInteractor.PickOptionRequest.Option("cheese", 1)
        option.addSynonym("ready")
        option.addSynonym("go")
        option.addSynonym("take it")
        option.addSynonym("ok")
        val prompt = Prompt("Say Cheese")
        requireActivity().voiceInteractor
            .submitRequest(object :
                VoiceInteractor.PickOptionRequest(prompt, arrayOf(option), null) {
                override fun onPickOptionResult(
                    finished: Boolean,
                    selections: Array<Option>,
                    result: Bundle
                ) {
                    if (finished && selections.size == 1) {
                        val message = Message.obtain()
                        message.obj = result
                        takePicture()
                    } else {
                        activity.finish()
                    }
                }

                override fun onCancel() {
                    activity.finish()
                }
            })
    }

    override fun onPause() {
        Log.d(TAG, "onPause: ")
        closeCamera()
        mOrientationListener!!.disable()
        stopBackgroundThread()
        super.onPause()
    }

    private fun isCameraSpecified(): Boolean {
        Log.d(TAG, "isCameraSpecified: ")
        return arguments != null && requireArguments().containsKey(EXTRA_USE_FRONT_FACING_CAMERA)
    }

    private fun shouldUseCamera(lensFacing: Int): Boolean {
        Log.d(TAG, "shouldUseCamera: ")
        return if (requireArguments().getBoolean(EXTRA_USE_FRONT_FACING_CAMERA)) {
            lensFacing == CameraCharacteristics.LENS_FACING_FRONT
        } else {
            lensFacing == CameraCharacteristics.LENS_FACING_BACK
        }
    }

    /**
     * Sets up member variables related to camera.
     *
     * @param width  The width of available size for camera preview
     * @param height The height of available size for camera preview
     */
    private fun setUpCameraOutputs(width: Int, height: Int) {
        Log.d(TAG, "setUpCameraOutputs: ")
        val activity: Activity? = activity
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                if (!isCameraSpecified()) {
                    // We don't use a front facing camera in this sample.
                    if (characteristics.get(CameraCharacteristics.LENS_FACING)
                        == CameraCharacteristics.LENS_FACING_FRONT
                    ) {
                        continue
                    }
                } else if (!shouldUseCamera(characteristics.get(CameraCharacteristics.LENS_FACING)!!)) {
                    // TODO: Handle case where there is no camera match
                    continue
                }
                val map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )

                // For still image captures, we use the largest available size.
                val largest = Collections.max(
                    Arrays.asList(*map!!.getOutputSizes(ImageFormat.JPEG)),
                    CompareSizesByArea()
                )
                mImageReader = ImageReader.newInstance(
                    largest.width, largest.height,
                    ImageFormat.JPEG,  /*maxImages*/2
                )
                mImageReader!!.setOnImageAvailableListener(
                    mOnImageAvailableListener, mBackgroundHandler
                )

                // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
                // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(
                    map.getOutputSizes(SurfaceTexture::class.java),
                    width, height, largest
                )

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                val orientation = resources.configuration.orientation
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    mTextureView.setAspectRatio(
                        mPreviewSize!!.width, mPreviewSize!!.height
                    )
                } else {
                    mTextureView.setAspectRatio(
                        mPreviewSize!!.height, mPreviewSize!!.width
                    )
                }
                mCameraId = cameraId
                mCharacteristics = characteristics
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            Log.e(TAG, "NPE: ", e)
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog().onCreateDialog(Bundle()).show()
            //ErrorDialog().show(fragmentManager, "dialog")
        }
    }

    /**
     * Opens the camera specified by [CameraFragment.mCameraId].
     */
    private fun openCamera(width: Int, height: Int) {
        Log.d(TAG, "openCamera: ")
        setUpCameraOutputs(width, height)
        configureTransform(width, height)
        val activity: Activity? = activity
        val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            manager.openCamera(mCameraId!!, mStateCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    /**
     * Closes the current [CameraDevice].
     */
    private fun closeCamera() {
        Log.d(TAG, "closeCamera: ")
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != mImageReader) {
                mImageReader!!.close()
                mImageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    /**
     * Starts a background thread and its [Handler].
     */
    private fun startBackgroundThread() {
        Log.d(TAG, "startBackgroundThread: ")
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    /**
     * Stops the background thread and its [Handler].
     */
    private fun stopBackgroundThread() {
        Log.d(TAG, "stopBackgroundThread: ")
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * Creates a new [CameraCaptureSession] for camera preview.
     */
    private fun createCameraPreviewSession() {
        Log.d(TAG, "createCameraPreviewSession: ")
        try {
            val texture: SurfaceTexture = mTextureView.surfaceTexture!!

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize!!.width, mPreviewSize!!.height)

            // This is the output Surface we need to start preview.
            val surface = Surface(texture)

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewRequestBuilder!!.addTarget(surface)

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice!!.createCaptureSession(
                Arrays.asList(surface, mImageReader!!.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        // The camera is already closed
                        if (null == mCameraDevice) {
                            return
                        }

                        // When the session is ready, we start displaying the preview.
                        mCaptureSession = cameraCaptureSession
                        try {
                            // Auto focus should be continuous for camera preview.
                            mPreviewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            // Flash is automatically enabled when necessary.
                            mPreviewRequestBuilder!!.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )

                            // Finally, we start displaying the camera preview.
                            mPreviewRequest = mPreviewRequestBuilder!!.build()
                            mCaptureSession!!.setRepeatingRequest(
                                mPreviewRequest!!,
                                mCaptureCallback, mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        showToast("Failed")
                    }
                }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Configures the necessary [android.graphics.Matrix] transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        Log.d(TAG, "configureTransform: ")
        val activity: Activity? = activity
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(
            0F, 0F, mPreviewSize!!.height.toFloat(),
            mPreviewSize!!.width.toFloat()
        )
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale =
                (viewHeight.toFloat() / mPreviewSize!!.height).coerceAtLeast(viewWidth.toFloat() / mPreviewSize!!.width)
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate((90 * (rotation - 2)).toFloat(), centerX, centerY)
        }
        mTextureView.setTransform(matrix)
    }

    /**
     * Initiate a still image capture.
     */
    private fun takePicture() {
        Log.d(TAG, "takePicture: ")
        lockFocus()
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private fun lockFocus() {
        Log.d(TAG, "lockFocus: ")
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK
            mCaptureSession!!.setRepeatingRequest(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            Log.e(TAG, "CameraAccessException: $e")
            e.printStackTrace()
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when we
     * get a response in [.mCaptureCallback] from [.lockFocus].
     */
    private fun runPrecaptureSequence() {
        Log.d(TAG, "runPrecaptureSequence: ")
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder!!.set(
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START
            )
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE
            mCaptureSession!!.capture(
                mPreviewRequestBuilder!!.build(), mCaptureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * [.mCaptureCallback] from both [.lockFocus].
     */
    private fun captureStillPicture() {
        Log.d(TAG, "captureStillPicture: ")
        try {
            val activity: Activity? = activity
            if (null == activity || null == mCameraDevice) {
                return
            }
            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader!!.surface)

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(
                CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
            )
            captureBuilder.set(
                CaptureRequest.CONTROL_AE_MODE,
                CaptureRequest.FLASH_MODE_OFF
            )
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(mCharacteristics, mOrientation)
            )
            val CaptureCallback: CameraCaptureSession.CaptureCallback =
                object : CameraCaptureSession.CaptureCallback() {
                    override fun onCaptureCompleted(
                        session: CameraCaptureSession, request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        showToast("Saved Picture")
                        //unlockFocus();
                    }
                }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.capture(captureBuilder.build(), CaptureCallback, null)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * Translates from the device orientation given by the Android sensor APIs to the
     * orientation for a JPEG image.
     */
    private fun getJpegOrientation(
        c: CameraCharacteristics?,
        deviceOrientation: Int
    ): Int {
        var deviceOrientation = deviceOrientation
        Log.d(TAG, "getJpegOrientation: ")
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation =
            c!!.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront = c.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }


    /**
     * Saves a JPEG [Image] into the specified [File].
     */
    private class ImageSaver(val context: Context?, val image: Image, val file: File?) :
        Runnable {
        private val TAG = Camera2Fragment::class.java.simpleName

        /**
         * The JPEG image
         */
        override fun run() {
            Log.d(TAG, "run: ")
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer[bytes]
            var output: FileOutputStream? = null
            try {
                output = FileOutputStream(file)
                output.write(bytes)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                image.close()
                if (null != output) {
                    try {
                        output.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                val contentUri = Uri.fromFile(file)
                mediaScanIntent.data = contentUri
                context!!.sendBroadcast(mediaScanIntent)
            }
        }

    }

    /**
     * Compares two `Size`s based on their areas.
     */
    internal class CompareSizesByArea : Comparator<Size?> {

        override fun compare(lhs: Size?, rhs: Size?): Int {

            return java.lang.Long.signum(
                lhs!!.width.toLong() * lhs.height -
                        rhs!!.width.toLong() * rhs.height
            )


        }
    }

    class ErrorDialog : DialogFragment() {
        private val TAG = Camera2Fragment::class.java.simpleName
        override fun onCreateDialog(savedInstanceState: Bundle): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                .setMessage("This device doesn't support Camera2 API.")
                .setPositiveButton(
                    android.R.string.ok
                ) { _, i ->
                    Log.d(TAG, "onClick: ")
                    activity.finish()
                }
                .create()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: ")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: ")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView: ")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetach: ")
    }

}