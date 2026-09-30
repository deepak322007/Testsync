package com.example.drugdetector.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.drugdetector.data.TestRecordRepository
import com.example.drugdetector.model.ResultCategory
import com.example.drugdetector.model.TestKitProfile
import com.example.drugdetector.model.TestRecord
import com.example.drugdetector.util.ColorAnalysisEngine
import com.example.drugdetector.util.CryptoUtils
import com.example.drugdetector.util.LocationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DrugDetectorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TestRecordRepository.getInstance(application)
    private val prefs = application.getSharedPreferences(   "drug_detector_user_settings", Context.MODE_PRIVATE)

    // User Settings & Auth State
    private val _userEmail = MutableStateFlow(prefs.getString("user_email", "") ?: "")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean("is_logged_in", false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _googleIdToken = MutableStateFlow(prefs.getString("google_id_token", "") ?: "")
    val googleIdToken: StateFlow<String> = _googleIdToken.asStateFlow()

    private val _operatorId = MutableStateFlow(prefs.getString("operator_id", "") ?: "")
    val operatorId: StateFlow<String> = _operatorId.asStateFlow()

    private val _operatorName = MutableStateFlow(prefs.getString("operator_name", "") ?: "")
    val operatorName: StateFlow<String> = _operatorName.asStateFlow()

    private val _agency = MutableStateFlow(prefs.getString("agency", "") ?: "")
    val agency: StateFlow<String> = _agency.asStateFlow()

    private val _branch = MutableStateFlow(prefs.getString("branch", "") ?: "")
    val branch: StateFlow<String> = _branch.asStateFlow()

    private val _rank = MutableStateFlow(prefs.getString("rank", "") ?: "")
    val rank: StateFlow<String> = _rank.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("is_dark_theme", true))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Test Log Filter States
    val searchQuery = MutableStateFlow("")
    val filterCategory = MutableStateFlow<ResultCategory?>(null)
    val filterKitId = MutableStateFlow<String?>(null)

    // Filtered Records Flow
    val filteredRecords: StateFlow<List<TestRecord>> = combine(
        repository.records,
        searchQuery,
        filterCategory,
        filterKitId
    ) { records, query, category, kitId ->
        records.filter { record ->
            val matchesQuery = query.isBlank() ||
                    record.id.contains(query, ignoreCase = true) ||
                    record.operatorId.contains(query, ignoreCase = true) ||
                    record.operatorName.contains(query, ignoreCase = true) ||
                    record.targetSubstance.contains(query, ignoreCase = true) ||
                    record.locationName.contains(query, ignoreCase = true) ||
                    record.notes.contains(query, ignoreCase = true)

            val matchesCategory = category == null || record.resultCategory == category
            val matchesKit = kitId == null || record.kitId == kitId

            matchesQuery && matchesCategory && matchesKit
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Capture & Analysis State
    private val _selectedKit = MutableStateFlow(TestKitProfile.STANDARD_KITS.first())
    val selectedKit: StateFlow<TestKitProfile> = _selectedKit.asStateFlow()

    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _analysisResult = MutableStateFlow<ColorAnalysisEngine.AnalysisResult?>(null)
    val analysisResult: StateFlow<ColorAnalysisEngine.AnalysisResult?> = _analysisResult.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationHelper.LocationData?>(null)
    val currentLocation: StateFlow<LocationHelper.LocationData?> = _currentLocation.asStateFlow()

    val captureNotes = MutableStateFlow("")

    // Selected Detail Record
    private val _selectedRecord = MutableStateFlow<TestRecord?>(null)
    val selectedRecord: StateFlow<TestRecord?> = _selectedRecord.asStateFlow()

    // Verification Result State for Audit Screen
    private val _auditResult = MutableStateFlow<CryptoUtils.VerificationResult?>(null)
    val auditResult: StateFlow<CryptoUtils.VerificationResult?> = _auditResult.asStateFlow()

    init {
        // Fetch location on startup
        refreshLocation()
    }

    fun saveUserSession(
        email: String,
        name: String,
        agencyName: String,
        badgeId: String,
        branchVal: String = "",
        rankVal: String = ""
    ) {
        _userEmail.value = email
        _operatorName.value = name
        _agency.value = agencyName
        _operatorId.value = badgeId
        _branch.value = branchVal
        _rank.value = rankVal
        _isLoggedIn.value = true

        prefs.edit()
            .putString("user_email", email)
            .putString("operator_name", name)
            .putString("agency", agencyName)
            .putString("operator_id", badgeId)
            .putString("branch", branchVal)
            .putString("rank", rankVal)
            .putBoolean("is_logged_in", true)
            .apply()
    }

    fun logout() {
        _isLoggedIn.value = false
        prefs.edit().putBoolean("is_logged_in", false).apply()
    }

    fun signupWithBackend(
        name: String,
        email: String,
        phone: String,
        password: String,
        branchVal: String,
        rankVal: String,
        badgeIdVal: String,
        onResult: (Boolean, String, String) -> Unit // success, message, targetEmail
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("name", name.trim())
                    put("email", email.trim())
                    put("phone", phone.trim())
                    put("password", password.trim())
                    put("branch", branchVal.trim())
                    put("rank", rankVal.trim())
                    put("badgeId", badgeIdVal.trim())
                }

                val response = performHttpPost("/api/auth/signup", json.toString())
                val resJson = JSONObject(response.second)
                val msg = resJson.optString("message", "Operation processed")

                if (response.first == 201 || response.first == 200) {
                    if (resJson.optBoolean("requiresEmailVerification", false)) {
                        withContext(Dispatchers.Main) {
                            onResult(false, "VERIFY_EMAIL", email.trim())
                        }
                    } else {
                        val userObj = resJson.optJSONObject("user")
                        val savedName = userObj?.optString("name")?.ifBlank { null } ?: name
                        val savedEmail = userObj?.optString("email")?.ifBlank { null } ?: email
                        val savedBranch = userObj?.optString("branch")?.ifBlank { null } ?: branchVal
                        val savedRank = userObj?.optString("rank")?.ifBlank { null } ?: rankVal
                        val savedBadge = userObj?.optString("badgeId")?.ifBlank { null } ?: badgeIdVal.ifBlank { "BADGE-" + Math.abs(savedEmail.hashCode() % 10000) }

                        saveUserSession(
                            email = savedEmail,
                            name = savedName,
                            agencyName = "$savedBranch - $savedRank",
                            badgeId = savedBadge,
                            branchVal = savedBranch,
                            rankVal = savedRank
                        )
                        withContext(Dispatchers.Main) {
                            onResult(true, "Registration Successful!", savedEmail)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, msg, email.trim())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}", email.trim())
                }
            }
        }
    }

    fun loginWithBackend(
        loginInput: String,
        password: String,
        onResult: (Boolean, String, String) -> Unit // success, message, targetEmail
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("loginInput", loginInput.trim())
                    put("email", loginInput.trim())
                    put("phone", loginInput.trim())
                    put("password", password.trim())
                }

                val response = performHttpPost("/api/auth/login", json.toString())
                val resJson = JSONObject(response.second)
                val msg = resJson.optString("message", "Login error")

                if (response.first == 200) {
                    val userObj = resJson.optJSONObject("user")
                    val savedEmail = userObj?.optString("email")?.ifBlank { null } ?: loginInput
                    val savedName = userObj?.optString("name")?.ifBlank { null } ?: savedEmail.substringBefore("@")
                    val savedBranch = userObj?.optString("branch")?.ifBlank { null } ?: "Field Division"
                    val savedRank = userObj?.optString("rank")?.ifBlank { null } ?: "Officer"
                    val savedBadge = userObj?.optString("badgeId")?.ifBlank { null } ?: ("BADGE-" + Math.abs(savedEmail.hashCode() % 10000))

                    saveUserSession(
                        email = savedEmail,
                        name = savedName,
                        agencyName = "$savedBranch - $savedRank",
                        badgeId = savedBadge,
                        branchVal = savedBranch,
                        rankVal = savedRank
                    )
                    withContext(Dispatchers.Main) {
                        onResult(true, "Login Successful!", savedEmail)
                    }
                } else if (response.first == 403 && resJson.optBoolean("requiresEmailVerification", false)) {
                    val targetEmail = resJson.optString("email", loginInput.trim())
                    withContext(Dispatchers.Main) {
                        onResult(false, "VERIFY_EMAIL", targetEmail)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, msg, loginInput.trim())
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}", loginInput.trim())
                }
            }
        }
    }

    fun verifyEmailOtp(
        email: String,
        otp: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                    put("otp", otp.trim())
                }

                val response = performHttpPost("/api/auth/verify-email-otp", json.toString())
                val resJson = JSONObject(response.second)

                if (response.first == 200) {
                    val userObj = resJson.optJSONObject("user")
                    val savedName = userObj?.optString("name")?.ifBlank { null } ?: email.substringBefore("@")
                    val savedEmail = userObj?.optString("email")?.ifBlank { null } ?: email
                    val savedBranch = userObj?.optString("branch")?.ifBlank { null } ?: "Field Division"
                    val savedRank = userObj?.optString("rank")?.ifBlank { null } ?: "Officer"
                    val savedBadge = userObj?.optString("badgeId")?.ifBlank { null } ?: ("BADGE-" + Math.abs(savedEmail.hashCode() % 10000))

                    saveUserSession(
                        email = savedEmail,
                        name = savedName,
                        agencyName = "$savedBranch - $savedRank",
                        badgeId = savedBadge,
                        branchVal = savedBranch,
                        rankVal = savedRank
                    )
                    withContext(Dispatchers.Main) {
                        onResult(true, "Email Verified & Logged In Successfully!")
                    }
                } else {
                    val msg = resJson.optString("message", "Invalid verification code")
                    withContext(Dispatchers.Main) {
                        onResult(false, msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun resendEmailOtp(
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                }

                val response = performHttpPost("/api/auth/resend-email-otp", json.toString())
                val resJson = JSONObject(response.second)
                val msg = resJson.optString("message", "Verification code resent")
                withContext(Dispatchers.Main) {
                    onResult(response.first == 200, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun forgotPassword(
        email: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                }

                val response = performHttpPost("/api/auth/forgot-password", json.toString())
                val resJson = JSONObject(response.second)
                val msg = resJson.optString("message", "Reset code processed")

                withContext(Dispatchers.Main) {
                    onResult(response.first == 200, msg)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun resetPassword(
        email: String,
        otp: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email.trim())
                    put("otp", otp.trim())
                    put("newPassword", newPassword.trim())
                }

                val response = performHttpPost("/api/auth/reset-password", json.toString())
                val resJson = JSONObject(response.second)
                val msg = resJson.optString("message", "Password reset processed")

                if (response.first == 200) {
                    val userObj = resJson.optJSONObject("user")
                    val savedName = userObj?.optString("name")?.ifBlank { null } ?: email.substringBefore("@")
                    val savedEmail = userObj?.optString("email")?.ifBlank { null } ?: email
                    val savedBranch = userObj?.optString("branch")?.ifBlank { null } ?: "Field Division"
                    val savedRank = userObj?.optString("rank")?.ifBlank { null } ?: "Officer"
                    val savedBadge = userObj?.optString("badgeId")?.ifBlank { null } ?: ("BADGE-" + Math.abs(savedEmail.hashCode() % 10000))

                    saveUserSession(
                        email = savedEmail,
                        name = savedName,
                        agencyName = "$savedBranch - $savedRank",
                        badgeId = savedBadge,
                        branchVal = savedBranch,
                        rankVal = savedRank
                    )
                    withContext(Dispatchers.Main) {
                        onResult(true, "Password Reset & Logged In Successfully!")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onResult(false, msg)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun performHttpPost(endpoint: String, jsonBody: String): Pair<Int, String> {
        val urls = listOf(
            "http://127.0.0.1:5000",
            "https://testsyncbackend.onrender.com",
            "http://192.168.29.144:5000"
        )
        var lastException: Exception? = null

        for (baseUrl in urls) {
            try {
                val url = URL(baseUrl + endpoint)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "TestSync-Android-App")
                conn.connectTimeout = 12000
                conn.readTimeout = 12000
                conn.doOutput = true

                conn.outputStream.use { os ->
                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }?.trim() ?: "{}"

                if (!responseText.startsWith("{") && !responseText.startsWith("[")) {
                    throw Exception("Server returned non-JSON response ($code)")
                }

                return Pair(code, responseText)
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw lastException ?: Exception("Server is offline or warming up. Please try again.")
    }

    fun updateOperatorSettings(id: String, name: String, agencyName: String) {
        _operatorId.value = id
        _operatorName.value = name
        _agency.value = agencyName
        prefs.edit()
            .putString("operator_id", id)
            .putString("operator_name", name)
            .putString("agency", agencyName)
            .apply()
    }

    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun selectTestKit(kit: TestKitProfile) {
        _selectedKit.value = kit
        // Re-analyze if bitmap exists
        _capturedBitmap.value?.let { processBitmap(it) }
    }

    fun refreshLocation() {
        viewModelScope.launch {
            val loc = LocationHelper.getCurrentLocation(getApplication())
            _currentLocation.value = loc
        }
    }

    fun processBitmap(bitmap: Bitmap) {
        _capturedBitmap.value = bitmap
        _isAnalyzing.value = true
        viewModelScope.launch {
            val result = ColorAnalysisEngine.analyzeTestImage(
                bitmap = bitmap,
                kitProfile = _selectedKit.value
            )
            _analysisResult.value = result
            _isAnalyzing.value = false
        }
    }

    fun generateSyntheticTest(reactionRgb: IntArray) {
        val bitmap = ColorAnalysisEngine.createSyntheticTestBitmap(reactionRgb)
        processBitmap(bitmap)
    }

    fun saveCurrentTestRecord(onSaved: (TestRecord) -> Unit) {
        val bitmap = _capturedBitmap.value ?: return
        val result = _analysisResult.value ?: return

        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
            val filename = "TEST_${_selectedKit.value.id}_$timeStr.jpg"

            val (savedFile, sha256Hash) = repository.saveCapturedImage(bitmap, filename)

            // Ensure fresh active location is queried before saving record
            var loc = _currentLocation.value
            if (loc == null || loc.latitude == 0.0) {
                loc = LocationHelper.getCurrentLocation(getApplication())
                _currentLocation.value = loc
            }

            val newRecord = TestRecord(
                id = "REC-${System.currentTimeMillis().toString().takeLast(8)}",
                timestampMs = timestamp,
                operatorId = _operatorId.value,
                operatorName = _operatorName.value,
                agency = _agency.value,
                kitId = _selectedKit.value.id,
                kitName = _selectedKit.value.name,
                targetSubstance = _selectedKit.value.targetSubstance,
                resultCategory = result.category,
                confidencePercent = result.confidencePercent,
                notes = captureNotes.value,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationAccuracy = loc.accuracy,
                locationName = loc.locationName,
                imagePath = savedFile.absolutePath,
                imageHashSha256 = sha256Hash,
                rawRgbHex = ColorAnalysisEngine.rgbToHex(result.rawRgb),
                calibratedRgbHex = ColorAnalysisEngine.rgbToHex(result.calibratedRgb),
                calibrationLightingQuality = result.lightingQuality
            )

            val signedRecord = repository.saveRecord(newRecord)
            _selectedRecord.value = signedRecord

            // Cloud Ingestion to MongoDB Atlas
            ingestRecordToCloud(signedRecord)

            // Clear capture fields
            captureNotes.value = ""
            onSaved(signedRecord)
        }
    }

    fun ingestRecordToCloud(record: TestRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("id", record.id)
                    put("recordId", record.id)
                    put("timestampMs", record.timestampMs)
                    put("operatorId", record.operatorId)
                    put("operatorName", record.operatorName)
                    put("operatorEmail", _userEmail.value)
                    put("agency", record.agency)
                    put("kitId", record.kitId)
                    put("kitName", record.kitName)
                    put("targetSubstance", record.targetSubstance)
                    put("resultCategory", record.resultCategory.name)
                    put("confidencePercent", record.confidencePercent)
                    put("notes", record.notes)
                    put("latitude", record.latitude)
                    put("longitude", record.longitude)
                    put("locationAccuracy", record.locationAccuracy)
                    put("locationName", record.locationName)
                    put("imageHashSha256", record.imageHashSha256)
                    put("rawRgbHex", record.rawRgbHex)
                    put("calibratedRgbHex", record.calibratedRgbHex)
                    put("calibrationLightingQuality", record.calibrationLightingQuality)
                    put("digitalSignature", record.digitalSignature)
                    put("keyFingerprint", record.keyFingerprint)
                }

                val response = performHttpPost("/api/records/ingest", json.toString())
                if (response.first == 201 || response.first == 200) {
                    repository.updateRecordSyncStatus(record.id, true)
                    Log.d("CloudIngestion", "Record ${record.id} ingested to MongoDB Atlas")
                }
            } catch (e: Exception) {
                Log.e("CloudIngestion", "Failed to ingest record to cloud: ${e.localizedMessage}")
            }
        }
    }

    fun selectRecordForDetail(recordId: String) {
        val record = repository.getRecordById(recordId)
        _selectedRecord.value = record
        if (record != null) {
            auditRecord(record)
        }
    }

    fun auditRecord(record: TestRecord) {
        val imageFile = repository.getImageFile(record.imagePath)
        val result = CryptoUtils.verifyRecord(getApplication(), record, imageFile)
        _auditResult.value = result
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            repository.deleteRecord(recordId)
            if (_selectedRecord.value?.id == recordId) {
                _selectedRecord.value = null
            }
        }
    }

    fun getImageFile(imagePath: String): File? {
        return repository.getImageFile(imagePath)
    }

    fun getKeyFingerprint(): String {
        return CryptoUtils.getKeyFingerprint(getApplication())
    }
}
