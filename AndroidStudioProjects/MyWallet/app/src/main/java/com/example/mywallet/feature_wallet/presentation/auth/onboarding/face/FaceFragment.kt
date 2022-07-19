package com.example.mywallet.feature_wallet.presentation.auth.onboarding.face

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
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
import androidx.navigation.fragment.findNavController
import com.example.mywallet.R
import com.example.mywallet.core.util.*
import com.example.mywallet.core.util.Constants.INVALID_IMAGE
import com.example.mywallet.databinding.FragmentFaceBinding
import com.example.mywallet.feature_wallet.data.TransactionDatabase
import com.example.mywallet.feature_wallet.data.remote.FaceValidationService
import com.example.mywallet.feature_wallet.data.remote.VoiceValidationService
import com.example.mywallet.feature_wallet.data.repository.TransactionRepositoryImpl
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.example.mywallet.feature_wallet.domain.repository.TransactionRepository
import com.example.mywallet.feature_wallet.domain.use_case.GetFacialValidation
import com.example.mywallet.feature_wallet.domain.use_case.SaveUserOnBoardingStatus
import com.example.mywallet.feature_wallet.presentation.auth.login.LoginActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.LoginVoiceInteraction
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.OnBoardingActivity
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.dataStore
import com.example.mywallet.feature_wallet.presentation.auth.onboarding.face.FaceValidationViewModel.*
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FaceFragment : Fragment(R.layout.fragment_face), LoginVoiceInteraction {
    private val TAG = FaceFragment::class.java.simpleName
    lateinit var binding: FragmentFaceBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var imageCapture: ImageCapture
    var photoFile: File? = null
    var camera: Camera? = null
    private var isPromptToCapturePhoto = false
    private var confirmRequest = false
    lateinit var model: FaceModel
    private lateinit var cameraSelector: CameraSelector
    private var completeVerification = false
    lateinit var outputDir: File
    var imageIsInvalid = false
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
            TransactionDatabase.getDatabase(requireContext()).transactionDao(),
            TransactionDatabase.getDatabase(requireContext()).userDao(),
            FaceValidationService.validationApi,
            VoiceValidationService.voiceValidationApi
        )
    }
    private val userPreferences: UserPreferences by lazy {
        UserPreferences((requireActivity() as OnBoardingActivity).dataStore)
    }
    private val viewModel by viewModels<FaceValidationViewModel> {
        FaceValidationViewModelFactory(
            this,
            GetFacialValidation(repository),
            SaveUserOnBoardingStatus(userPreferences)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity() as OnBoardingActivity).initLocalVoiceInteraction(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentFaceBinding.bind(view)

        lifecycleScope.launch {
            extensionsManager = ExtensionsManager.getInstance(requireContext()).await()
        }

        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDirectory()



        lifecycleScope.launchWhenStarted {
            subscribeFaceUIEvent()
            initCameraX()
        }


        viewModel.setVoiceFaceValidationState(
            VoiceFaceValidationState(isPromptToCapturePhoto = true)
        )

        voiceValidationState()

        binding.takePhoto.setOnClickListener {
            if (::imageCapture.isInitialized)
                capturePhoto()
        }

    }

    private fun voiceValidationState() {
        viewModel.voiceFaceValidationState.observe(viewLifecycleOwner) { state ->
            isPromptToCapturePhoto = state.isPromptToCapturePhoto
            confirmRequest = state.confirmRequest
            completeVerification = state.completeVerification
            isValidating = state.isValidating
            imageIsInvalid = state.imageIsInvalid

            activity?.startLocalVoiceInteraction(Bundle())
        }

    }

    private fun subscribeFaceUIEvent() {
        viewModel.faceUIEvent.onEach { event ->
            when (event) {
                is UIEvent.IsLoading -> {
                    showProgressDialog()
                }
                is UIEvent.Success -> {
                    dismissProgressDialog()
                    if (event.model?.condition == "registration successful") {
                        Log.d(TAG, "subscribeFaceUIEvent: success -> ${event.model.condition}")
                        model = FaceModel(
                            condition = event.model.condition,
                            status = event.model.status
                        )
                        viewModel.setUserOnBoardingStatus()
                        viewModel.setVoiceFaceValidationState(
                            VoiceFaceValidationState(completeVerification = true)
                        )
                    } else {
                        Log.d(
                            TAG,
                            "subscribeFaceUIEvent: not successfull ->${event.model?.condition}"
                        )
                        model = FaceModel(
                            condition = event.model?.condition ?: "Something went wrong",
                            status = event.model?.status ?: false,
                        )
                        viewModel.setVoiceFaceValidationState(
                            VoiceFaceValidationState(
                                imageIsInvalid = true
                            )
                        )
                    }
                }
                is UIEvent.Error -> {
                    dismissProgressDialog()
                    showToast("Image Error -> ${event.message}")
                    viewModel.setVoiceFaceValidationState(
                        VoiceFaceValidationState(confirmRequest = true)
                    )
                }
            }

        }.launchIn(lifecycleScope)
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
        val user = FaceFragmentArgs.fromBundle(requireArguments()).user
        val userName =  user?.userName ?: "Isaac"

        val photoFileCamX = File(
            outputDir, "${userName}.jpg"
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
                        viewModel.getFaceValidation(it)
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
            viewModel.setVoiceFaceValidationState(VoiceFaceValidationState(isPromptToCapturePhoto = true))

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

        if (isPromptToCapturePhoto) {
            promptToCapturePhoto()
        } else if (confirmRequest) {
            confirmRequest()
        } else if (completeVerification) {
            completeVerificationRequest()
        } else if (isValidating) {
            promptFaceIsValidating()
        } else if (imageIsInvalid) {
            promptImageIsInvalid()
        }

    }

    private fun promptImageIsInvalid() {

        if (::model.isInitialized) {
            promptToTryAgain()
        }

    }

    private fun promptToTryAgain() {
        val prompt =
            VoiceInteractor.Prompt("${model.condition}, do you want to try again")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    promptToCapturePhoto()
                } else {
                    moveToAudioFragment()
                    activity?.stopLocalVoiceInteraction()
                }
            }
        })
    }


    private fun completeVerificationRequest() {
        //val args = FaceFragmentArgs.fromBundle(requireArguments()).isFaceOrAudioRegistered
        val args = Constants.AUDIO_NOT_REGISTERED

        if (args == Constants.AUDIO_NOT_REGISTERED) {
            promptRegisterAudio()
        } else {
            completeFacialRegistration()
        }
        //completeFacialRegistration()


    }

    private fun promptRegisterAudio() {
        val prompt =
            VoiceInteractor.Prompt("your facial registration is successful, Would want to enable  Voice authentication?")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.ConfirmationRequest(prompt, null) {
            override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                super.onConfirmationResult(confirmed, result)
                if (confirmed) {
                    navigateToAudioFragment()
                } else {

                    //navigateToPinFragment()
                    moveToLogin()
                }
                activity?.stopLocalVoiceInteraction()
            }
        })

    }

    private fun moveToLogin() {
        Toast.makeText(context, "Move to login ", Toast.LENGTH_SHORT).show()
        val intent = Intent((requireActivity() as OnBoardingActivity), LoginActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToPinFragment() {
        val user = FaceFragmentArgs.fromBundle(requireArguments()).user
        val action = FaceFragmentDirections.actionFaceFragmentToPinFragment(user!!)
        findNavController().navigate(action)
    }

    private fun navigateToAudioFragment() {
        val args = FaceFragmentArgs.fromBundle(requireArguments()).user
        val action = FaceFragmentDirections.actionFaceFragmentToRecordAudioFragment(
            Constants.FACE_REGISTERED,
            args
        )
        findNavController().navigate(action)
    }

    private fun completeFacialRegistration() {
        val prompt =
            VoiceInteractor.Prompt("your facial registration is successfully, please login to continue")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                activity?.stopLocalVoiceInteraction()
                completeVerification = false
                moveToLogin()


            }
        })
    }

    private fun confirmRequest() {

        val msg: String = when (condition) {
            INVALID_IMAGE -> {
                "The image is invalid, do you want to try again"
            }
            else -> "Something went wrong, do yo want to try again"
        }

        val prompt = getPrompt(msg)
        activity?.voiceInteractor!!
            .submitRequest(object : VoiceInteractor.ConfirmationRequest(prompt, null) {
                override fun onConfirmationResult(confirmed: Boolean, result: Bundle?) {
                    super.onConfirmationResult(confirmed, result)
                    if (confirmed) {
                        promptToCapturePhoto()

                    } else {
                        moveToAudioFragment()
                        activity?.stopLocalVoiceInteraction()
                    }

                }
            })
    }

    private fun moveToAudioFragment() {
        val args = FaceFragmentArgs.fromBundle(requireArguments())
        if (Constants.AUDIO_NOT_REGISTERED == args.isFaceOrAudioRegistered) {
            val action = FaceFragmentDirections.actionFaceFragmentToRecordAudioFragment(
                Constants.FACE_REGISTERED,
                args.user
            )
            findNavController().navigate(action)
        } else {
            val user = FaceFragmentArgs.fromBundle(requireArguments()).user
            val action = FaceFragmentDirections.actionFaceFragmentToPinFragment(user!!)
            findNavController().navigate(action)
        }
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
            getPrompt("Please move closer to the camera and make sure your you have a proper lighting in the background, tell me when you are ready")
        activity?.voiceInteractor!!
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
                        capturePhoto()
                        activity?.stopLocalVoiceInteraction()
                    } else {
                        showToast("Didn't  take the  picture")
                    }
                }

                override fun onCancel() {
                    showToast("Finish!")
                }
            })
    }

    private fun promptFaceIsValidating() {
        showToast("Face is validating")
        val prompt = VoiceInteractor.Prompt("Your face is validating, this may take a while")
        activity?.voiceInteractor!!.submitRequest(object :
            VoiceInteractor.CompleteVoiceRequest(prompt, null) {
            override fun onCompleteResult(result: Bundle?) {
                super.onCompleteResult(result)
                isValidating = false
                activity?.stopLocalVoiceInteraction()

            }
        })
    }


}


