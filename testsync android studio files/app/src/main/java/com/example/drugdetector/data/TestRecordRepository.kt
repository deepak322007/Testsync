package com.example.drugdetector.data

import android.content.Context
import android.graphics.Bitmap
import com.example.drugdetector.model.TestRecord
import com.example.drugdetector.util.CryptoUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TestRecordRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val recordsFile = File(appContext.filesDir, "drug_test_records.json")
    private val imagesDir = File(appContext.filesDir, "record_images").apply { if (!exists()) mkdirs() }

    private val _records = MutableStateFlow<List<TestRecord>>(emptyList())
    val records: StateFlow<List<TestRecord>> = _records.asStateFlow()

    init {
        loadRecords()
    }

    private fun loadRecords() {
        if (!recordsFile.exists()) {
            _records.value = emptyList()
            return
        }
        try {
            val json = recordsFile.readText()
            val type = object : TypeToken<List<TestRecord>>() {}.type
            val list: List<TestRecord> = gson.fromJson(json, type) ?: emptyList()

            // Filter out any legacy predefined sample records
            val cleanList = list.filterNot {
                it.id.startsWith("REC-2025-08") ||
                it.operatorName == "Officer J. Miller" ||
                it.operatorName == "Det. S. Vance"
            }

            _records.value = cleanList.sortedByDescending { it.timestampMs }
            if (cleanList.size != list.size) {
                saveRecordsToDisk()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _records.value = emptyList()
        }
    }

    private fun saveRecordsToDisk() {
        try {
            val json = gson.toJson(_records.value)
            recordsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun saveRecord(record: TestRecord): TestRecord = withContext(Dispatchers.IO) {
        val signature = CryptoUtils.signRecord(appContext, record)
        val fingerprint = CryptoUtils.getKeyFingerprint(appContext)
        val signedRecord = record.copy(
            digitalSignature = signature,
            keyFingerprint = fingerprint
        )

        val updatedList = _records.value.toMutableList().apply {
            add(0, signedRecord)
        }
        _records.value = updatedList
        saveRecordsToDisk()
        signedRecord
    }

    suspend fun saveCapturedImage(bitmap: Bitmap, filename: String): Pair<File, String> = withContext(Dispatchers.IO) {
        val imageFile = File(imagesDir, filename)
        val fos = FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
        fos.flush()
        fos.close()

        val hash = CryptoUtils.calculateFileSha256(imageFile)
        Pair(imageFile, hash)
    }

    fun getRecordById(id: String): TestRecord? {
        return _records.value.find { it.id == id }
    }

    fun getImageFile(imagePath: String): File? {
        val file = File(imagePath)
        if (file.exists()) return file
        val fileInDir = File(imagesDir, File(imagePath).name)
        if (fileInDir.exists()) return fileInDir
        return null
    }

    suspend fun deleteRecord(id: String) = withContext(Dispatchers.IO) {
        val recordToDelete = getRecordById(id)
        if (recordToDelete != null) {
            val imageFile = getImageFile(recordToDelete.imagePath)
            imageFile?.delete()
        }
        val updatedList = _records.value.filterNot { it.id == id }
        _records.value = updatedList
        saveRecordsToDisk()
    }

    suspend fun updateRecordSyncStatus(recordId: String, isSynced: Boolean) = withContext(Dispatchers.IO) {
        val updatedList = _records.value.map {
            if (it.id == recordId) it.copy(isSyncedToCloud = isSynced) else it
        }
        _records.value = updatedList
        saveRecordsToDisk()
    }

    companion object {
        @Volatile
        private var INSTANCE: TestRecordRepository? = null

        fun getInstance(context: Context): TestRecordRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TestRecordRepository(context).also { INSTANCE = it }
            }
        }
    }
}
