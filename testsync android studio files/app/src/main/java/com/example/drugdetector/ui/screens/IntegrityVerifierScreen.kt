package com.example.drugdetector.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drugdetector.model.TestRecord
import com.example.drugdetector.ui.theme.NegativeRed
import com.example.drugdetector.ui.theme.VerifiedBlue
import com.example.drugdetector.ui.viewmodel.DrugDetectorViewModel
import com.example.drugdetector.util.CryptoUtils
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrityVerifierScreen(
    viewModel: DrugDetectorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val records by viewModel.filteredRecords.collectAsState()
    val deviceFingerprint = remember { viewModel.getKeyFingerprint() }

    var jsonInput by remember { mutableStateOf("") }
    var verificationResult by remember { mutableStateOf<CryptoUtils.VerificationResult?>(null) }
    var parsedRecord by remember { mutableStateOf<TestRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tamper Integrity Verifier", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auditor Integrity Checklist",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Verify whether a digital test record payload or captured image file has been altered or tampered with.",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Device Signing Key Fingerprint: $deviceFingerprint",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Quick Audit Select from Database
            if (records.isNotEmpty()) {
                Text(
                    text = "1. Select Existing Record to Audit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedButton(
                    onClick = {
                        val first = records.first()
                        viewModel.selectRecordForDetail(first.id)
                        val gson = Gson()
                        jsonInput = gson.toJson(first)
                        parsedRecord = first
                        val imgFile = viewModel.getImageFile(first.imagePath)
                        verificationResult = CryptoUtils.verifyRecord(context, first, imgFile)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Audit Latest Record (${records.first().id})")
                }
            }

            // Raw JSON Input / Paste
            Text(
                text = "2. Or Paste Raw JSON Record Certificate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = jsonInput,
                onValueChange = {
                    jsonInput = it
                    if (it.isNotBlank()) {
                        try {
                            val gson = Gson()
                            val rec = gson.fromJson(it, TestRecord::class.java)
                            parsedRecord = rec
                            val imgFile = viewModel.getImageFile(rec.imagePath)
                            verificationResult = CryptoUtils.verifyRecord(context, rec, imgFile)
                        } catch (e: Exception) {
                            verificationResult = CryptoUtils.VerificationResult(
                                isValid = false,
                                isImageHashValid = false,
                                isSignatureValid = false,
                                message = "Invalid JSON format: ${e.message}"
                            )
                        }
                    }
                },
                label = { Text("JSON Digital Certificate Payload") },
                placeholder = { Text("Paste JSON record here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // Audit Result Report
            verificationResult?.let { result ->
                val resultColor = if (result.isValid) VerifiedBlue else NegativeRed

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = resultColor.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (result.isValid) "AUDIT VERIFIED: INTACT" else "AUDIT FAILED: TAMPERING DETECTED",
                                fontWeight = FontWeight.Bold,
                                color = resultColor,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AuditStepRow(
                            label = "Image SHA-256 Digest Match",
                            passed = result.isImageHashValid
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AuditStepRow(
                            label = "HMAC Digital Signature Verification",
                            passed = result.isSignatureValid
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = result.message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AuditStepRow(
    label: String,
    passed: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (passed) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (passed) VerifiedBlue else NegativeRed,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (passed) VerifiedBlue else NegativeRed
        )
    }
}
