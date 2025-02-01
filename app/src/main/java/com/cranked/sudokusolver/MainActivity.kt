package com.cranked.sudokusolver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import com.cranked.sudokusolver.extensions.preProcessBitmap
import com.cranked.sudokusolver.extensions.processForOCR
import com.cranked.sudokusolver.extensions.reduceNoise
import com.cranked.sudokusolver.extensions.showToast
import com.cranked.sudokusolver.extensions.toGrayscale
import com.cranked.sudokusolver.ocr.CameraAnalysisConfig
import com.cranked.sudokusolver.ocr.MlKitOcrHelper
import com.cranked.sudokusolver.ocr.TessOcr
import com.cranked.sudokusolver.utils.file_utils.FileUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
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
    private var tfLiteFileName = "sudoku_model.tflite"
    private val sudokuResultHasMap = hashMapOf<Int, String>()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    val mlKitOcrHelper = MlKitOcrHelper()

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
        val inputBitmap = drawableToBitmap(getDrawable(R.drawable.sudoku_test)!!)

        setContentView(binding.root)
        OpenCVLoader.initDebug()

        this@MainActivity.supportActionBar?.hide()
        checkAndRequestCameraPermission()
    }

    fun drawableToBitmap(drawable: Drawable): Bitmap {
        // Eğer drawable zaten BitmapDrawable ise, direkt Bitmap döndür
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }

        // Drawable'ın boyutlarını kontrol et
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

        // Bitmap oluştur
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Drawable'ı Canvas kullanarak Bitmap'e çiz
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
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
        mainActivityViewModel.checkSudoku(context = this@MainActivity, bitmap)
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
        val trainnedDataFromAssetFile = this.getAssetAsFile(trainedDataFileName)
        val tfLiteFromAssetFile = this.getAssetAsFile(trainedDataFileName)

        val tessDataFile =
            this.applicationContext.getExternalFilesDir("model/tessdata")
                .toString()
        val targetFile =
            this.applicationContext.getExternalFilesDir("model")
                .toString()
        val trainedDataFileName = File(tessDataFile, trainedDataFileName)
        if (!trainedDataFileName.exists()) {
            trainedDataFileName.createNewFile()
        }
        val tfLiteModelFile = File(targetFile, tfLiteFileName)
        if (!tfLiteModelFile.exists()) {
            tfLiteModelFile.createNewFile()
        }
        FileUtil.copyFile(trainnedDataFromAssetFile, trainedDataFileName.absolutePath)
        FileUtil.copyFile(tfLiteFromAssetFile, tfLiteModelFile.absolutePath)
        tessOcr.initializeOcr()
        initClickListener()
        observeData()
    }

    private fun observeData() {
        mainActivityViewModel.resultBitmap.observe(this) { resultBitmap ->
            resultBitmap?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    it.forEachIndexed { index, bitmap ->
                        val text = CoroutineScope(Dispatchers.IO).async {
                            val result =
                                tessOcr.ocrCamera.convertImageToText(
                                    bitmap.preProcessBitmap().reduceNoise().toGrayscale()
                                )
                            return@async result
                        }.await()
                        sudokuResultHasMap[index] =
                            if (text?.length == 2) text?.first().toString() else text ?: ""
                    }
                    val intArray =
                        mainActivityViewModel.convertHashMapToSudokuArray(sudokuResultHasMap)
                    val sudokuSolver = SudokuSolver()
                    println("SudokuResultHasMap: $sudokuResultHasMap")

                    if (sudokuSolver.isValidSudoku(intArray)) {
                        println("ValidArray: $intArray")
                        val isSolved = sudokuSolver.solveSudokuAsync(intArray)
                        if (isSolved) {
                            CoroutineScope(Dispatchers.Main).launch {
                                binding.sudokuResultImageView.setImageBitmap(
                                    sudokuSolver.drawSudokuGrid(
                                        intArray
                                    )
                                )
                                showToast("Çözüldü")
                            }
                        }
                    }


                    mainActivityViewModel.initOcrVariable()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}