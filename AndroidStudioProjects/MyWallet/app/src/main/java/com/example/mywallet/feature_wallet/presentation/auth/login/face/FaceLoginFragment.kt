package com.example.mywallet.feature_wallet.presentation.auth.login.face

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.SimpleDateFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.*
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.mywallet.R
import com.example.mywallet.core.util.*
import com.example.mywallet.databinding.FragmentFaceValidationBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialLogin
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserLoginStatus
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginActivity
import com.example.mywallet.feature_wallet.presentation.auth.login.VoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.login.dataStore
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.ui.TransactionActivity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FaceLoginFragment : Fragment(R.layout.fragment_face_validation), VoiceInteraction {
    private val TAG = FaceLoginFragment::class.java.simpleName
    lateinit var binding: FragmentFaceValidationBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var imageCapture: ImageCapture
    var photoFile: File? = null
    var camera: Camera? = null
    lateinit var model: FaceModel
    private lateinit var cameraSelector: CameraSelector
    lateinit var outputDir: File
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var progressDialog: AlertDialog
    lateinit var hdrCameraSelector: CameraSelector
    private var errorMsg: String? = null
    lateinit var voiceState: FaceVoiceState
    lateinit var extensionsManager: ExtensionsManager
    private lateinit var imgCaptureExecutor: ExecutorService

    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as LoginActivity).dataStore)
    }


    private val repository: TransactionRepository by lazy {
        TransactionRepositoryImpl(
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi
        )
    }

    private val viewModel by viewModels<FaceLoginViewModel> {
        FaceLoginViewModelFactory(
            GetFacialLogin(repository),
            SaveUserLoginStatus(userPreferences),
            this
        )
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as LoginActivity).initVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFaceValidationBinding.bind(view)
        lifecycleScope.launch {
            extensionsManager = ExtensionsManager.getInstance(requireContext()).await()
        }

        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDirectory()

        initCameraX()

        viewModel.onEvent(FaceLoginViewModel.FaceLoginUiEvent.InitVoicePrompt)

        lifecycleScope.launchWhenStarted {
            subscribeFaceUiState()
        }

    }


    private suspend fun subscribeFaceUiState() {
        viewModel.faceUiState.collectLatest { state ->

            if (state.isLoading) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
            }

            state.voiceState?.let {
                voiceState = it
                activity?.startLocalVoiceInteraction(null)
            }

            state.error?.let {
                errorMsg = it
            }

            state.faceModel?.let { faceModel ->
                model = FaceModel(
                    condition = faceModel.condition,
                    status = faceModel.status
                )
            }

        }
    }

    private fun initCameraX() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            try {

                cameraProvider = cameraProviderFuture.get()
                Log.d(
                    TAG, "onViewCreated: voiceInteraction \${activity.isVoiceInteraction()}"
                )
                setLensFaceFront()

                startCameraX()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: CameraInfoUnavailableException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun checkPermissions() {
        when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.RECORD_AUDIO
            ) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
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
            File(activity?.externalMediaDirs!![0].toString() + "/CameraXPhotos/", fileNm)
        //photoFile = file
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFileCamX).build()

        animateFlash()

        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setFlashLight(false)
                    showToast("Photo taken successfully")
                    Log.d(TAG, "onImageSaved: -> ${file.toUri()}")

                    photoFile?.let {
                        viewModel.onEvent(FaceLoginViewModel.FaceLoginUiEvent.GetValidation(it))
                    }


                }

                override fun onError(exception: ImageCaptureException) {
                    showToast("Error Taking photo " + exception.message)
                }
            }
        )
    }

    private fun showProgressDialog() {
        val dialogLayout =
            LayoutInflater.from(requireContext()).inflate(R.layout.progess_layout, null)
        progressDialog = AlertDialog
            .Builder(requireContext())
            .setView(dialogLayout)
            .create()

        progressDialog.show()

    }

    private fun dismissProgressDialog() {
        if (::progressDialog.isInitialized) {
            progressDialog.dismiss()
        }
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
                .setFlashMode(ImageCapture.FLASH_MODE_ON)
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
                camera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector, imageCapture, preview
                )
            }


            setFlashLight(false);
            viewModel.onEvent(FaceLoginViewModel.FaceLoginUiEvent.CapturePhoto)

        } catch (e: Exception) {
            Log.d(
                TAG, "startCameraX: Use case binding failed ", e
            )
        }

    }

    private fun showToast(s: String) {
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show()
    }


    private fun aspectRatio(w: Int, h: Int): Int {
        val previewRatio: Double = (Math.max(w, h) / Math.min(w, h)).toDouble()
        if (Math.abs(previewRatio - 4.0 / 3.0) <= Math.abs(previewRatio - 16.0 / 9.0)) {
            return AspectRatio.RATIO_4_3
        }

        return AspectRatio.RATIO_16_9
    }


    fun getRealPathFromURI(uri: Uri?): String? {
        var path = ""
        if (activity?.contentResolver != null) {
            val cursor: Cursor? = activity?.contentResolver!!.query(uri!!, null, null, null, null)
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

    private fun getOutputDirectory(): File {
        val mediaDir = activity?.externalMediaDirs!!.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity?.filesDir!!
    }

    private fun setFlashLight(flashFlag: Boolean) {
        if (camera!!.cameraInfo.hasFlashUnit()) {
            camera!!.cameraControl.enableTorch(flashFlag)
        }
    }

    private fun getPrompt(s: String): VoiceInteractor.Prompt {
        return VoiceInteractor.Prompt(s)
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

    override fun isVoiceInteractionStarted() {
        if (::voiceState.isInitialized) {
            when {
                voiceState.isFacialRecognition -> {
                    initFacialRecognition()
                }
                voiceState.isPromptToCapturePhoto -> {
                    promptToCapturePhoto()
                }
                voiceState.confirmRequest -> {
                    confirmRequest()
                }
                voiceState.completeVerification -> {
                    completeFacialRegistration()
                }
                voiceState.isInvalidImage -> {
                    promptImageIsInvalid()
                }
            }
        }
    }

    private fun promptImageIsInvalid() {
        if (::model.isInitialized) {
            when (model.condition) {
                Constants.INVALID_IMAGE -> {
                    promptToTryAgain()
                }
                Constants.UNKNOWN_USER -> {
                    promptUserIsNotRegistered()
                }
            }
        }
    }

    private fun promptUserIsNotRegistered() {
        val prompt = VoiceInteractor.Prompt("${model.condition}, do want to try again?")
        activity?.voiceInteractor!!
            .submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
                override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                    super.onConfirmationResult(confirmed, result)
                    if (confirmed) {
                        promptToCapturePhoto()
                    } else {
                        moveToPinFragment()
                    }
                }
            })


    }

    private fun promptToTryAgain() {
        val prompt =
            VoiceInteractor.Prompt(model.condition)
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptToCapturePhoto()

            }
        })
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
        option1.addSynonym("I'm ready")

        val prompt =
            getPrompt("Please move closer to the camera and make sure you have proper lighting in the background, Tell me when you are ready")
        activity?.voiceInteractor!!
            .submitRequest(object :
                VoiceInteractor.PickOptionRequest(prompt, arrayOf(option1), Bundle()) {
                override fun onPickOptionResult(
                    finished: Boolean,
                    selections: Array<Option>,
                    result: Bundle
                ) {
                    if (finished && selections.size == 1) {

                        Log.d(TAG, "onPickOptionResult: result")
                        activity?.stopLocalVoiceInteraction()
                        capturePhoto()

                    }

                }


            })
    }

    private fun initFacialRecognition() {
        val prompt = VoiceInteractor.Prompt("We are validating your face")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                promptToCapturePhoto()
            }
        })
    }

    private fun completeFacialRegistration() {
        val prompt = VoiceInteractor.Prompt("${model.condition}, Your Login is successful")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                showToast("Registration successful")
                viewModel.setUserIsLoggedIn(model.condition)
                moveToTransactionFragment()
                activity?.stopLocalVoiceInteraction()


            }
        })
    }

    private fun moveToTransactionFragment() {
        requireActivity().run {
            val transactionIntent = Intent(this, TransactionActivity::class.java)
            transactionIntent.putExtra("username", model.condition)
            startActivity(transactionIntent)
            finish()
        }
    }

    private fun confirmRequest() {

        if (errorMsg == null)
            return

        val prompt = getPrompt("$errorMsg, do want to try again?")
        activity?.voiceInteractor!!
            .submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
                override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                    super.onConfirmationResult(confirmed, result)
                    if (confirmed) {
                        promptToCapturePhoto()
                    } else {
                        moveToPinFragment()
                        activity?.stopLocalVoiceInteraction()
                    }
                }
            })

    }

    private fun moveToPinFragment() {
        showToast("Move to pin fragment")
    }


}