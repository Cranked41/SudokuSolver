package com.cranked.sudokusolver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.cranked.sudokusolver.databinding.ActivityMainBinding
import com.cranked.sudokusolver.extensions.getAssetAsFile
import com.cranked.sudokusolver.extensions.showToast
import com.cranked.sudokusolver.ocr.CameraAnalysisConfig
import com.cranked.sudokusolver.ocr.TessOcr
import com.cranked.sudokusolver.utils.CoroutineCustomExceptionHandler
import com.cranked.sudokusolver.utils.file_utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private val sudokuSolver = SudokuSolver()
    private var tessOcr = TessOcr(this@MainActivity)
    private var trainedDataFileName = "eng.traineddata"

    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filterValues { !it }

            if (deniedPermissions.isEmpty()) {
                // Tüm izinler verildi
                initiateTfLite()
            } else {
                // Reddedilen izinler varsa
                showToast("Bazı izinler reddedildi: ${deniedPermissions.keys}")
            }
        }
    private val requiredPermissions = mutableListOf<String>().apply {
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this@MainActivity.supportActionBar?.hide()
        checkAndRequestCameraPermission()

    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@MainActivity)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(this@MainActivity)
        )
    }

    @SuppressLint("SetTextI18n")
    private fun initClickListener() {
        binding.solveButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                val timeTaken = measureTimeMillis {
                    val selectedArray = SudokuTestModel().sudokuModels.random()
                    if (sudokuSolver.isValidSudoku(selectedArray.intArray)) {
                        if (sudokuSolver.solveSudokuAsync(selectedArray.intArray)) {
                            binding.sudokuOutput.text = selectedArray.arrayName
                            binding.sudokuResultImageView.setImageBitmap(
                                sudokuSolver.drawSudokuGrid(
                                    selectedArray.intArray
                                )
                            )
                        } else {
                            binding.sudokuOutput.text = "Sudoku çözülemedi."
                        }
                    } else {
                        binding.sudokuOutput.text =
                            "Sudoku ${selectedArray.arrayName} matrisi geçersiz!"
                    }
                }
                binding.timeTakenTextView.text = "Çözüm süresi ${timeTaken} ms"
            }
        }
    }

    // Asenkron Sudoku çözüm fonksiyonu
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: run {
            println("Hata oluştu CameraProvider")
            return
        }
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        preview = CameraAnalysisConfig.previewBuilder(binding.viewFinder.display!!.rotation)
        //binding.viewFinder.implementationMode =  PreviewView.ImplementationMode.PERFORMANCE

        imageAnalyzer =
            CameraAnalysisConfig.imageBuilder(binding.viewFinder.display!!.rotation).also {
                it.setAnalyzer(cameraExecutor) { image ->
                    detectSquares(image)
                    image.close()
                }
            }

        cameraProvider.unbindAll()

        try {
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            println("CameraProvider:  $exc")
        }
    }

    private fun detectSquares(image: ImageProxy) {/*
        if (!::bitmapBuffer.isInitialized) {
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
         */
        val bitmap = image.toBitmap() ?: return
        mainActivityViewModel.sendResultBitmap(bitmap)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = binding.viewFinder.display!!.rotation
    }

    private fun checkAndRequestCameraPermission() {
        when {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                initiateTfLite()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Kullanıcı daha önce reddetmiş, izin neden gerektiğini açıklayın
                showToast("Lütfen Kamera iznini veriniz")
                requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
            }

            else -> {
                requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        }
    }

    private fun initiateTfLite() {
        // İzin verildi
        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.viewFinder.post { setUpCamera() }
        val assetFile = this.getAssetAsFile(trainedDataFileName)

        val targetFile =
            this.applicationContext.getExternalFilesDir("model/tessdata")
                .toString()
        val trainedDataFileName = File(targetFile, trainedDataFileName)
        if (!trainedDataFileName.exists()) {
            trainedDataFileName.createNewFile()
        }
        FileUtil.copyFile(assetFile, trainedDataFileName.absolutePath)
        tessOcr.initializeOcr()
        initClickListener()
        observeData()
    }

    private fun observeData() {
        mainActivityViewModel.resultBitmap.observe(this) {
            CoroutineScope(Dispatchers.IO + CoroutineCustomExceptionHandler.handler).launch {
                val result = tessOcr.ocrCamera.convertImageToText(it)
                println("Ocrsonuçları  $result")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}