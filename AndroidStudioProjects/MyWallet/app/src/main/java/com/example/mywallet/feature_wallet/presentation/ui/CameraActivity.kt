package com.example.mywallet.feature_wallet.presentation.ui

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.app.VoiceInteractor.Prompt
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.net.Uri
import com.example.mywallet.R
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.mywallet.core.util.*
import com.example.mywallet.core.util.Constants.INVALID_IMAGE
import com.example.mywallet.core.util.Constants.UNKNOWN_USER
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class CameraActivity : AppCompatActivity() {
    private val TAG = CameraActivity::class.java.simpleName
    lateinit var binding: com.example.mywallet.databinding.ActivityCameraBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var imageCapture: ImageCapture
    var photoFile: File? = null
    var camera: Camera? = null
    private var isPromptToCapturePhoto = false
    private var confirmRequest = false
    lateinit var model: FaceModel
    lateinit var cameraSelector: CameraSelector
    private var completeVerification = false
    lateinit var outputDir: File
    lateinit var userPreferences: UserPreferences
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var progressDialog: AlertDialog
    private var isValidating = false
    lateinit var hdrCameraSelector: CameraSelector
    private var condition: String? = null
    lateinit var extensionsManager: ExtensionsManager
    private lateinit var imgCaptureExecutor: ExecutorService
    var errMsg: String? = null
    var exposureIndex = 0

    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(this).transactionDao(),
            TransactionDatabase.getDatabase(this).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi
        )
    }
    private val viewModel by viewModels<TransactionViewModel> {
        TransactionViewModelFactory(this, repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera)
        checkPermissions()
        lifecycleScope.launch {
            extensionsManager = ExtensionsManager.getInstance(baseContext).await()
        }
        userPreferences = UserPreferences(dataStore);
        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDirectory()

        val bundle = viewModel.transfer.value
        Log.d(TAG, "onCreate: Bundle -> ${bundle?.get("transferMode").toString()}")

        cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)
        cameraProviderFuture.addListener(Runnable {
            try {

                cameraProvider = cameraProviderFuture.get()
                Log.d(
                    TAG, "onViewCreated: voiceInteraction \${activity.isVoiceInteraction()}"
                )
                setLensFaceFront()
                //setLensFaceBack()
                startCameraX()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: CameraInfoUnavailableException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))

        binding.takePhoto.setOnClickListener {
            if (::imageCapture.isInitialized)
                capturePhoto()
        }

        binding.exposureBtn.setOnClickListener {
            val range = camera?.cameraInfo?.exposureState?.exposureCompensationRange
            if (range != null) {
                if (range.contains(exposureIndex + 1)) {
                    camera?.cameraControl?.setExposureCompensationIndex(++exposureIndex)
                    val ev =
                        camera?.cameraInfo?.exposureState?.exposureCompensationStep?.toFloat()
                            ?.times(exposureIndex)
                    Log.i("CameraXLog", "EV: $ev")
                }
            }
        }

    }

    private fun checkPermissions() {
        when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                ActivityCompat.requestPermissions(
                    this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    WRITE_PERMISSION_CODE
                )
            }
        }

    }


    private fun capturePhoto() {
        showToast("Take picture!")
        //Create a storage location whose fileName is timestamped in milliseconds.
        val photoFileCamX = File(
            outputDir,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )
        photoFile = photoFileCamX
        val date = Date()
        val timeStamp = date.time.toString()

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")

        val fileNm = "/$timeStamp.jpg"
        val file =
            File(externalMediaDirs[0].toString() + "/CameraXPhotos/", fileNm)
        //photoFile = file
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFileCamX).build()
//        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
//            contentResolver,
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            contentValues
//        ).build()

        // if (!file.exists()) file.mkdir()
        animateFlash()
        //photoFile = File(photoFilePath)
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setFlashLight(false)
                    showToast("Photo taken successfully")
                    Log.d(TAG, "onImageSaved: -> ${file.toUri()}")
                    //Log.d(TAG, "onImageSaved: bhb-> ${outputFileResults.savedUri}")
                    //val kuri: Uri = Uri.fromFile(photoFile)


                    validateUser(outputFileResults.savedUri)


                }

                override fun onError(exception: ImageCaptureException) {
                    showToast("Error Taking photo " + exception.message)
                }
            }
        )
    }

    private fun showProgressDialog() {
//        if (intent.extras != null){
//            startLocalVoiceInteraction(Bundle())
//        }
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.progess_layout, null)
        progressDialog = AlertDialog
            .Builder(this)
            .setView(dialogLayout)
            .create()

        progressDialog.show()

    }

    private fun dismissProgressDialog() {
        if (::progressDialog.isInitialized) {
            progressDialog.dismiss()
        }
    }

    private fun validateUser(uri: Uri?) {
        lifecycleScope.launch {
            uri?.let {
                Log.d(TAG, "validateUser: photo uri -> $it")
//                if (intent.extras != null){
//                    isValidating = true
//                }
                viewModel.getValidation(it.toString(), photoFile).collect { result ->
                    when (result) {
                        is Resource.Loading -> {
                            Log.d(TAG, "validateUser: Loading data")
                            showProgressDialog()
                        }
                        is Resource.Success -> {
                            dismissProgressDialog()
                            Log.d(TAG, "validateUser: Success..")
                            val message = result.data
                            if (message!!.status) {

                                showToast("Validation is  successfully")
                                userPreferences.updateValidUser(true)
                                completeVerification = true
                                viewModel.setIsUserVerified(true)
                                model =
                                    FaceModel(condition = message.condition, status = true)
                                startLocalVoiceInteraction(Bundle())
                            } else {
                                confirmRequest = true
                                condition = result.data.condition
                                condition?.let {
                                    showToast(it)
                                    startLocalVoiceInteraction(Bundle())
                                }

                            }
                        }

                        is Resource.Error -> {
                            confirmRequest = true
                            condition = "Something went wrong, please try again"
                            showToast("Failed...${result.message}")
                            Log.d(TAG, "validateUser: Error.. ${result.message}")
                            dismissProgressDialog()
                            result.message?.let { it1 -> showToast(it1) }
                            startLocalVoiceInteraction(Bundle())
                        }
                    }
                }
            }
        }
    }

    private fun completeVerificationRequest() {
        showToast("Complete request -> model status ${model.status}")
        val prompt = Prompt("${model.condition} your verification is successful")
//        voiceInteractor.submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
//            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
//                super.onConfirmationResult(confirmed, result)
//                completeVerification = false
//                moveToServiceActivity()
//                stopLocalVoiceInteraction()
//                //val bundle: Bundle = intent?.extras ?: Bundle()
//                Log.d(TAG, "======= logIntent ========= %s")
//                Log.d(TAG, "Logging intent data start")
//                //Log.d(TAG, "logIntent: Action -> ${intent.action}")
//
////                bundle.keySet().forEach { key ->
////                    Log.d(TAG, "[$key=${bundle.get(key)}]")
////                }
//
//                Log.d(TAG, "Logging intent data complete")
//            }
//        })
        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                completeVerification = false
                moveToServiceActivity()
                stopLocalVoiceInteraction()


            }
        })
    }

    private fun moveToServiceActivity() {

        val serviceIntent = Intent(this, ServiceTransferActivity::class.java)
        serviceIntent.putExtra("isValidated", true)
        errMsg?.let {
            serviceIntent.putExtra("error", it)
        }
        //showToast("Moving to Service activity")
        val bundle = viewModel.transfer.value
        Log.d(TAG, "moveToServiceActivity: bundle -> ${bundle?.get("transferMode").toString()}")
        startActivity(serviceIntent)
    }

    private fun startCameraX() {
        // Unbind use cases before rebinding
        try {
            val aspectRatio = aspectRatio(binding.previewView.width, binding.previewView.height)
            val resolutionSize = Size(binding.previewView.width, binding.previewView.height)
            showToast("Camera X started")
            //preview use case
            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()
            preview.setSurfaceProvider(binding.previewView.surfaceProvider)
            //ImageCapture Use Case

            //val previewConfig =  Camera2Config

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(resolutionSize)
                .setTargetRotation(binding.root.display.rotation)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                .build()


            if (extensionsManager.isExtensionAvailable(
                    cameraProvider,
                    cameraSelector,
                    ExtensionMode.BEAUTY
                )
            ) {
                // cameraProvider.unbindAll()
                showToast("Beauty is available")
                hdrCameraSelector = extensionsManager.getExtensionEnabledCameraSelector(
                    cameraProvider,
                    cameraSelector,
                    ExtensionMode.BEAUTY
                )
                camera = cameraProvider!!.bindToLifecycle(
                    this,
                    hdrCameraSelector!!, imageCapture, preview
                )


            } else {
                //showToast("Beauty is not available")
                // Bind use cases to camera
                camera = cameraProvider!!.bindToLifecycle(
                    this,
                    cameraSelector!!, imageCapture, preview
                )
            }


            setFlashLight(false);
            showToast("Prompt to capture")
            isPromptToCapturePhoto = true
            startLocalVoiceInteraction(Bundle())
//            if (intent.extras != null) {
//                showToast("Prompt to capture")
//                isPromptToCapturePhoto = true
//                startLocalVoiceInteraction(Bundle())
//            }
        } catch (e: Exception) {
            Log.d(
                TAG, "startCameraX: Use case binding failed ", e
            )
        }

    }


    private fun aspectRatio(w: Int, h: Int): Int {
        val previewRatio: Double = (Math.max(w, h) / Math.min(w, h)).toDouble()
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }

        return AspectRatio.RATIO_16_9
    }


    private fun selectCameraDirection() {
        cameraProvider!!.unbindAll()
        val hasBackCamera = cameraProvider!!.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFrontCamera = cameraProvider!!.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        if (hasBackCamera && hasFrontCamera) {
            //getPrompt("Open with front camera or back camera");
            if (isVoiceInteraction) {
                promptToSelectLens()
            }
        } else if (hasBackCamera) {
            setLensFaceBack()
        }
    }

    fun getRealPathFromURI(uri: Uri?): String? {
        var path = ""
        if (contentResolver != null) {
            val cursor: Cursor? = contentResolver.query(uri!!, null, null, null, null)
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }

    private fun setLensFaceFront() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
    }

    private fun setLensFaceBack() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

//    private fun clickToCapturePhoto() {
//        binding.takePhoto.setOnClickListener(View.OnClickListener { capturePhoto() })
//    }


    override fun onLocalVoiceInteractionStarted() {
        super.onLocalVoiceInteractionStarted()

        if (isPromptToCapturePhoto) {
            promptToCapturePhoto()
        }

        if (confirmRequest) {
            confirmRequest()
        }

        if (completeVerification) {
            completeVerificationRequest()
        }

        if (isValidating) {
            promptFaceIsValidating()
        }
    }

    private fun promptFaceIsValidating() {
        showToast("Face is validating")
        val prompt = Prompt("Your face is validating, this may take a while")
        voiceInteractor.submitRequest(object : VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                isValidating = false
                stopLocalVoiceInteraction()

            }
        })
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }


    private fun promptToCapturePhoto() {
        Log.d(TAG, "startVoiceTrigger: ")
        val option1 = VoiceInteractor.PickOptionRequest.Option("cheese", 1)
        option1.addSynonym("ok")
        option1.addSynonym("okay")
        option1.addSynonym("ready")
        option1.addSynonym("snap")
        option1.addSynonym("take")
        option1.addSynonym("Ok, ready")
        option1.addSynonym("take a picture")
        option1.addSynonym("take the picture")
        option1.addSynonym("take a photo")
        option1.addSynonym("take the photo")
        option1.addSynonym("I am ready")

        val prompt = getPrompt("Tell me when you are ready")
        voiceInteractor
            .submitRequest(object :
                VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1), null) {
                override fun onPickOptionResult(
                    finished: Boolean,
                    selections: Array<Option>,
                    result: Bundle
                ) {
                    if (finished && selections.size == 1) {
                        val message = Message.obtain()
                        Log.d(TAG, "onPickOptionResult: result")
                        message.obj = result
                        isPromptToCapturePhoto = false
                        capturePhoto()
                        stopLocalVoiceInteraction()
                    } else {
                        showToast("Didn't  take the  picture")
                    }
                }

                override fun onCancel() {
                    showToast("Finish!")
                }
            })
    }

    private fun setFlashLight(flashFlag: Boolean) {
        if (camera!!.cameraInfo.hasFlashUnit()) {
            camera!!.cameraControl.enableTorch(flashFlag)
        }
    }

    private fun getPrompt(s: String): Prompt {
        return Prompt(s)
    }

    private fun promptToSelectLens() {
        Log.d(TAG, "startVoiceTrigger: ")
        val optionBack = VoiceInteractor.PickOptionRequest.Option("back", 1)
        optionBack.addSynonym("back camera")
        optionBack.addSynonym("choose back")
        optionBack.addSynonym("choose back camera")
        optionBack.addSynonym("select back")
        optionBack.addSynonym("select back camera")
        optionBack.addSynonym("use back")
        optionBack.addSynonym("use back camera")
        val optionFront = VoiceInteractor.PickOptionRequest.Option("front", 1)
        optionFront.addSynonym("front camera")
        optionFront.addSynonym("use front")
        optionFront.addSynonym("use front camera")
        optionFront.addSynonym("select front")
        optionFront.addSynonym("select front camera")
        optionFront.addSynonym("choose front")
        optionFront.addSynonym("choose front camera")
        val prompt = getPrompt("Open using front camera or back camera?")
        voiceInteractor
            .submitRequest(object :
                VoiceInteractor.PickOptionRequest(prompt, arrayOf(optionBack, optionFront), null) {
                override fun onPickOptionResult(
                    finished: Boolean,
                    selections: Array<Option>,
                    result: Bundle
                ) {
                    if (finished && selections.size == 1) {
                        val index = selections[0].index
                        when (index) {
                            0 -> {
                                showToast("Back camera")
                                setLensFaceBack()
                            }
                            1 -> {
                                showToast("Front camera")
                                setLensFaceFront()
                            }
                        }
                        val message = Message.obtain()
                        Log.d(
                            TAG, "onPickOptionResult: result"
                        )
                        message.obj = result
                        startCameraX()
                    } else {
                        activity.finish()
                        showToast("Didn't select a side")
                    }
                }

                override fun onCancel() {
                    activity.finish()
                    showToast("Finish!")
                }
            })
    }

    private fun confirmRequest() {

        val msg: String = when (condition) {
            UNKNOWN_USER -> {
                "Validation failed, you need to register as a user"
            }
            INVALID_IMAGE -> {
                "The image is invalid, do you wish to try again"
            }
            else -> "Something went wrong, please try again"
        }

        val prompt = getPrompt(msg)
        voiceInteractor
            .submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
                override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                    super.onConfirmationResult(confirmed, result)
                    if (confirmed) {
                        if (condition == UNKNOWN_USER) {
                            moveToTransactions()
                        }
                        //stopLocalVoiceInteraction()
                        confirmRequest = false
                        startCameraX()
                        promptToCapturePhoto()


                    } else {
                        moveToTransactions()
                    }
                }
            })

    }

    private fun moveToTransactions() {
        val transIntent = Intent(this, TransactionActivity::class.java)
        intent = null
        startActivity(transIntent)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

}