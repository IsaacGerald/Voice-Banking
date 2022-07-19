package com.example.mywallet.feature_wallet.presentation.auth.login

import android.app.AlertDialog
import android.app.VoiceInteractor
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.camera2.*
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
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
import androidx.lifecycle.lifecycleScope
import com.example.mywallet.R
import com.example.mywallet.core.util.*
import com.example.mywallet.databinding.FragmentPreviewBinding
import com.example.mywallet.feature_wallet.data.repository.UserPreferences
import com.example.mywallet.feature_wallet.domain.model.FaceModel
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PreviewFragment : Fragment(R.layout.fragment_preview) {
    private val TAG = PreviewFragment::class.java.simpleName
    lateinit var binding: FragmentPreviewBinding
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

    private val cameraManager by lazy {
        activity?.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentPreviewBinding.bind(view)
        checkPermissions()
        lifecycleScope.launch {
            extensionsManager = ExtensionsManager.getInstance(requireContext()).await()
        }

        imgCaptureExecutor = Executors.newSingleThreadExecutor()
        outputDir = getOutputDirectory()!!


        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
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
        }, ContextCompat.getMainExecutor(requireContext()))

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

    private fun getOutputDirectory(): File? {
        val mediaDir = activity?.externalMediaDirs?.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else activity?.filesDir
    }


    private fun setLensFaceFront() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
    }

    private fun checkPermissions() {
        when (PackageManager.PERMISSION_DENIED) {
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.RECORD_AUDIO) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.READ_CONTACTS) -> {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    RECORD_AUDIO_PERMISSION_CODE
                )
            }
            ContextCompat.checkSelfPermission(
                requireActivity(),
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
            File(activity?.externalMediaDirs?.get(0)!!.toString() + "/CameraXPhotos/", fileNm)
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
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    setFlashLight(false)
                    showToast("Photo taken successfully")
                    Log.d(TAG, "onImageSaved: -> ${file.toUri()}")
                    //Log.d(TAG, "onImageSaved: bhb-> ${outputFileResults.savedUri}")
                    //val kuri: Uri = Uri.fromFile(photoFile)


                    //validateUser(outputFileResults.savedUri)


                }

                override fun onError(exception: ImageCaptureException) {
                    showToast("Error Taking photo " + exception.message)
                }
            }
        )
    }

    private fun showProgressDialog() {
        val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.progess_layout, null)
        progressDialog = AlertDialog
            .Builder(context)
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

    private fun setFlashLight(flashFlag: Boolean) {
        if (camera!!.cameraInfo.hasFlashUnit()) {
            camera!!.cameraControl.enableTorch(flashFlag)
        }
    }

    private fun getPrompt(s: String): VoiceInteractor.Prompt {
        return VoiceInteractor.Prompt(s)
    }


    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
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