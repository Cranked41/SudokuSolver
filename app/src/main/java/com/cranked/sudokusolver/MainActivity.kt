package com.cranked.sudokusolver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.cranked.sudokusolver.databinding.ActivityMainBinding
import com.cranked.sudokusolver.extensions.getAssetAsFile
import com.cranked.sudokusolver.extensions.isNullOrEmptyOrBlank
import com.cranked.sudokusolver.extensions.preprocessImage
import com.cranked.sudokusolver.extensions.showToast
import com.cranked.sudokusolver.model.SudokuResultModel
import com.cranked.sudokusolver.ocr.MlKitOcrHelper
import com.cranked.sudokusolver.ocr.TessOcr
import com.cranked.sudokusolver.tensorflow.CameraSettings
import com.cranked.sudokusolver.utils.BitmapUtil
import com.cranked.sudokusolver.utils.file_utils.FileUtil
import com.cranked.sudokusolver.utils.maze.ImageUtil.imageToBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis
import androidx.core.view.isVisible

class MainActivity : AppCompatActivity() {
    private var _camera: Camera? = null

    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var cameraExecutor: ExecutorService
    private val sudokuSolver = SudokuSolver()
    private var tessOcr = TessOcr(this@MainActivity)
    private var trainedDataFileName = "digits.traineddata"

    private val sudokuResultHasMap = hashMapOf<Int, String>()
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val ocrResultModelList = arrayListOf<SudokuResultModel>()

    private var drawProcessOnSudoku = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val deniedPermissions = permissions.filterValues { !it }

            if (deniedPermissions.isEmpty()) {
                // Tüm izinler verildi
                initiateTessOcr()
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

//        val inputBitmap = drawableToBitmap(getDrawable(R.drawable.sudoku_test)!!)
        initClickListener()
        setContentView(binding.root)
        OpenCVLoader.initDebug()
        initClickListener()
        observeData()
        onBackPressedDispatcher.addCallback(this) {
            if (binding.resultLinLayout.isVisible) {
                // Result ekranındayken geri: yeniden dene gibi davran
                binding.solvedSudokuImageView.setImageDrawable(null)
                resumeCamera()
            } else {
                // Normal geri davranışı
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        this@MainActivity.supportActionBar?.hide()
        checkAndRequestCameraPermission()
        val model = SudokuTestModel().sudokuModels[0]
        CoroutineScope(Dispatchers.Default).launch {
            val initTime = System.currentTimeMillis()
            println("Sudoku ValidState ${sudokuSolver.isValidSudoku(model.intArray)}")
            val result = sudokuSolver.solveSudokuAsync(model.intArray)
            if (result) {
                println("Çözdü: ${System.currentTimeMillis() - initTime} ms")
                printSudokuAsString(model.intArray)
            }
        }
    }

    fun printSudokuAsString(sudoku: Array<IntArray>) {
        sudoku.forEach { row ->
            println(row.joinToString(" "))
        }
    }


    private fun setCamera() {
        val processCameraProvider = CameraSettings.processCameraProvider(this)
        cameraProvider = processCameraProvider
        val cameraSelector = CameraSettings.cameraSelector()
        val preview = CameraSettings.preview()
        val analysis = CameraSettings.analysis()

        binding.previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        binding.previewView.implementationMode = PreviewView.ImplementationMode.PERFORMANCE

        preview.surfaceProvider = binding.previewView.surfaceProvider

        analysis.setAnalyzer(Executors.newSingleThreadExecutor()) { proxy ->
            if (binding.resultLinLayout.isVisible) {
                proxy.close()
                return@setAnalyzer
            }
            detectSquares(proxy)
        }

        _camera = processCameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
    }

    private fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Throwable) {
        }
    }

    private fun resumeCamera() {
        drawProcessOnSudoku = false
        binding.resultLinLayout.visibility = View.GONE
        binding.previewView.visibility = View.VISIBLE
        startCamera()
    }

    private fun startCamera() {
        // yeniden bind
        setCamera()
    }


    private fun showSolvedOnUi(finalBitmap: Bitmap) {
        binding.solvedSudokuImageView.setImageBitmap(finalBitmap)
        binding.resultLinLayout.visibility = View.VISIBLE
        binding.previewView.visibility = View.GONE
        binding.solveButton.visibility = View.GONE
        stopCamera()
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
        binding.tryAgainButton.setOnClickListener {
            binding.solvedSudokuImageView.setImageDrawable(null)
            resumeCamera()
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
        val bitmap = image.imageToBitmap() ?: return
        mainActivityViewModel.checkSudoku(bitmap, rotationDegrees = 0f)
    }


    private fun checkAndRequestCameraPermission() {
        when {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                initiateTessOcr()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Kullanıcı daha önce reddetmiş, izin neden gerektiğini açıklayın
                showToast(this.getString(R.string.please_grant_camera_permission))
                requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
            }

            else -> {
                requestPermissionLauncher.launch(requiredPermissions.toTypedArray())
            }
        }
    }

    private fun initiateTessOcr() {
        // İzin verildi
        cameraExecutor = Executors.newSingleThreadExecutor()
        setCamera()
        val trainnedDataFromAssetFile = this.getAssetAsFile(trainedDataFileName)
        val tessDataFile =
            this.applicationContext.getExternalFilesDir("model/tessdata")

        val trainedDataFileName = File(tessDataFile, trainedDataFileName)
        if (!trainedDataFileName.exists()) {
            trainedDataFileName.createNewFile()
        }

        FileUtil.copyFile(trainnedDataFromAssetFile, trainedDataFileName.absolutePath)

        tessOcr.initializeOcr()

    }

    private fun observeData() {
        mainActivityViewModel.resultBitmap.observe(this) { resultBitmap ->
            resultBitmap?.let {
                ocrResultModelList.clear()
                CoroutineScope(Dispatchers.Default).launch {
                    it.forEachIndexed { index, sudokuOcrModel ->
                        if (sudokuOcrModel.notOcr) {
                            sudokuResultHasMap[index] = "0"
                        } else {
                            val processedbitmap =
                                sudokuOcrModel.cellBitmap.preprocessImage()
                            val ocrResultModel = CoroutineScope(Dispatchers.Default).async {

                                val resultModel =
                                    tessOcr.ocrCamera.convertImageToText(
                                        processedbitmap
                                    )
                                if ((resultModel?.accuracy
                                        ?: 0) < 20 || resultModel?.ocr.isNullOrEmptyOrBlank()
                                ) {
                                    resultModel?.ocr = "0"
                                }

                                return@async resultModel
                            }.await()
                            sudokuResultHasMap[index] =
                                if (ocrResultModel?.ocr?.length == 2) ocrResultModel?.ocr?.first()
                                    .toString() else if (ocrResultModel?.ocr?.length!! > 2 == true) "0" else ocrResultModel?.ocr
                                    ?: ""
                            ocrResultModelList.add(
                                SudokuResultModel(
                                    text = ocrResultModel?.ocr ?: "",
                                    accuracy = ocrResultModel?.accuracy ?: 0,
                                    cellBitmap = processedbitmap
                                )
                            )
                        }
                    }
                    val intArray =
                        mainActivityViewModel.convertHashMapToSudokuArray(sudokuResultHasMap)
                    val original = intArray.map { it.clone() }.toTypedArray()
                    val sudokuSolver = SudokuSolver()

                    printSudokuAsString(intArray)
                    println("---------------------------\n")
                    if (sudokuSolver.isValidSudoku(intArray)) {
                        printSudokuAsString(intArray)
                        val isSolved = sudokuSolver.solveSudokuAsync(intArray)
                        if (isSolved) {
                            CoroutineScope(Dispatchers.Main).launch {
                                printSudokuAsString(intArray)
                                if (drawProcessOnSudoku) return@launch
                                val gridBitmap = mainActivityViewModel.gridBitmapLiveData.value
                                val solvedBitmap = if (gridBitmap != null) {
                                    BitmapUtil.overlaySolutionOnGrid(gridBitmap, original, intArray)
                                } else {
                                    sudokuSolver.drawSudokuGrid(intArray)
                                }
                                showSolvedOnUi(solvedBitmap)
                                drawProcessOnSudoku = true
                                //showToast(getString(R.string.sudokuSolved))
                            }
                        }
                    }


                    mainActivityViewModel.initOcrVariable()
                }
            }
        }
        mainActivityViewModel.rotateBitmapLiveData.observe(this) {
            CoroutineScope(Dispatchers.Main).launch {
                //binding.rotateBitmap.setImageBitmap(it)
            }
        }
        mainActivityViewModel.gridBitmapLiveData.observe(this) {
            CoroutineScope(Dispatchers.Main).launch {
                binding.imageSudokuGridBitmap.setImageBitmap(it)
                /* if (it == null) {
                     binding.sudokuResultImageView.setImageBitmap(it)
                 }*/
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}