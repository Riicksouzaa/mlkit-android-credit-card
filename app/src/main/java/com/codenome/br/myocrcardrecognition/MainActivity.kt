package com.codenome.br.myocrcardrecognition

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codenome.br.myocrcardrecognition.databinding.ActivityMainBinding
import com.codenome.br.myocrcardrecognition.model.BarcodeRecognition
import com.codenome.br.myocrcardrecognition.model.GmsTextRecognition
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = requireNotNull(_binding)

    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private val gmsTextRecognition = GmsTextRecognition()
    private val barcodeRecognition = BarcodeRecognition()
    private lateinit var camera: Camera
    private var imageCapture: ImageCapture? = null

    @ExperimentalGetImage
    private val framesAnalyzer: ImageAnalysis.Analyzer by lazy {
        ImageAnalysis.Analyzer {
            onFrameReceived(it)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 1
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private val RUNTIME_PERMISSIONS_REQUIRED = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    @ExperimentalGetImage
    private fun onFrameReceived(imageProxy: ImageProxy) {
        val frame = imageProxy.image
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        if (frame == null) {
            imageProxy.close()
            return
        }

//        barcodeRecognition.processFrame(frame, rotationDegrees)
//            .addOnCompleteListener { imageProxy.close() }
//            .addOnSuccessListener {
//                Log.d(
//                    "TEST",
//                    it.joinToString { barcode -> barcode.rawValue.toString() })
//            }
//            .addOnFailureListener { Log.e("ops", "deu merda ${it.toString()}") }

        gmsTextRecognition
            .processFrame(frame, rotationDegrees)
            .addOnCompleteListener { imageProxy.close() }
            .addOnSuccessListener { recognizedLineList ->

                val txt = recognizedLineList.firstOrNull { recognizedLine ->
                    val hehe = recognizedLine.text.replace("[ ]".toRegex(), "")
                    hehe.length == 16 && hehe.matches("-?\\d+(\\.\\d+)?".toRegex())
                }
                txt?.let { recognizedLine ->
                    Toast.makeText(this, recognizedLine.text, Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { Log.e("HEHE", it.toString()) }
    }

    private fun allRuntimePermissionsGranted(): Boolean {
        return RUNTIME_PERMISSIONS_REQUIRED.any {
            isPermissionGranted(this, it)
        }
    }

    private fun getRuntimePermissions() {
        val permissionsToRequest = ArrayList<String>()

        RUNTIME_PERMISSIONS_REQUIRED.forEach {
            if (!isPermissionGranted(this, it)) {
                permissionsToRequest.add(it)
            }
            if (shouldShowRequestPermissionRationale(it)) {
                Log.d(this::class.java.simpleName, "$it - TEST")
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if (!allRuntimePermissionsGranted()) {
            getRuntimePermissions()
        }

        startCamera()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(this).apply {
            addListener(
                { bindCameraUseCases(get()) },
                ContextCompat.getMainExecutor(this@MainActivity)
            )
        }
    }

    private fun bindCameraUseCases(processCameraProvider: ProcessCameraProvider) {
        val screenAspectRatio =
            aspectRatio(binding.cameraPreview.width, binding.cameraPreview.height)

        val preview = Preview.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setTargetAspectRatio(screenAspectRatio)
            .build()
            .also { it.setSurfaceProvider(binding.cameraPreview.surfaceProvider) }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setTargetAspectRatio(screenAspectRatio)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(cameraExecutor, framesAnalyzer) }

        imageCapture = ImageCapture.Builder()
            .setTargetRotation(Surface.ROTATION_0)
            .setTargetAspectRatio(screenAspectRatio)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        val useCaseGroup = UseCaseGroup.Builder().run {
            addUseCase(preview)
            addUseCase(imageAnalyzer)
            addUseCase(imageCapture!!)
            binding.cameraPreview.viewPort?.let { setViewPort(it) }
            build()
        }

        try {
            processCameraProvider.unbindAll()
            camera = processCameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                useCaseGroup
            )
        } catch (t: Throwable) {
            Log.e("Opa", "Camera use cases binding failed")
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }
}