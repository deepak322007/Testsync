package com.example.drugdetector.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drugdetector.model.ResultCategory
import com.example.drugdetector.ui.theme.InconclusiveAmber
import com.example.drugdetector.ui.theme.NegativeRed
import com.example.drugdetector.ui.theme.PositiveGreen
import com.example.drugdetector.ui.theme.VerifiedBlue
import com.example.drugdetector.ui.viewmodel.DrugDetectorViewModel
import com.example.drugdetector.util.PdfExporter
import com.example.drugdetector.util.QrCodeGenerator
import com.google.gson.GsonBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    viewModel: DrugDetectorViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val record by viewModel.selectedRecord.collectAsState()
    val auditResult by viewModel.auditResult.collectAsState()

    val currentRecord = record

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = currentRecord?.id ?: "Record Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentRecord != null) {
                        IconButton(onClick = {
                            val gson = GsonBuilder().setPrettyPrinting().create()
                            val jsonProof = gson.toJson(currentRecord)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putchar("EXTRA_TEXT", jsonProof)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Digital Proof Record"))
                        }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share Proof")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (currentRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No record selected.")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val categoryColor = when (currentRecord!!.resultCategory) {
                    ResultCategory.POSITIVE -> PositiveGreen
                    ResultCategory.NEGATIVE -> NegativeRed
                    ResultCategory.INCONCLUSIVE -> InconclusiveAmber
                }

                // Integrity Audit Status Banner
                auditResult?.let { audit ->
                    val auditColor = if (audit.isValid) VerifiedBlue else NegativeRed
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = auditColor.copy(alpha = 0.12f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (audit.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = auditColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (audit.isValid) "CRYPTOGRAPHICALLY VERIFIED INTACT" else "TAMPERING DETECTED",
                                    fontWeight = FontWeight.Bold,
                                    color = auditColor,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = audit.message,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Outcome Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = categoryColor.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentRecord!!.resultCategory.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = categoryColor
                        )
                        Text(
                            text = "${currentRecord!!.kitName} • ${currentRecord!!.targetSubstance}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Confidence Level: ${currentRecord!!.confidencePercent}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Captured Image View
                val imageFile = remember(currentRecord!!.imagePath) {
                    viewModel.getImageFile(currentRecord!!.imagePath)
                }
                val capturedBitmap = remember(imageFile) {
                    if (imageFile != null && imageFile.exists()) {
                        BitmapFactory.decodeFile(imageFile.absolutePath)
                    } else null
                }

                capturedBitmap?.let { bmp ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Evidentiary Image Capture",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Test Image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                // Export PDF Certificate Action Button
                Button(
                    onClick = {
                        PdfExporter.generateAndSharePdf(context, currentRecord!!, capturedBitmap)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VerifiedBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Evidence Certificate (PDF)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // QR Code Verification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.QrCode2, contentDescription = null, tint = VerifiedBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Court & Audit QR Verification", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }

                        val qrUrl = "https://testsyncbackend.onrender.com/api/records/verify/${currentRecord!!.id}"
                        val qrBitmap = remember(currentRecord!!.id) {
                            QrCodeGenerator.generateQrCode(qrUrl, 260)
                        }

                        qrBitmap?.let { qr ->
                            Image(
                                bitmap = qr.asImageBitmap(),
                                contentDescription = "Verification QR Code",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        Text(
                            text = "Scan QR code to verify record on live cloud ledger",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Metadata Grid
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Chain of Custody Metadata",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )

                        HorizontalDivider()

                        DetailRow(icon = Icons.Default.Security, label = "Record ID", value = currentRecord!!.id)
                        DetailRow(icon = Icons.Default.Schedule, label = "Timestamp", value = currentRecord!!.formattedTimestamp)
                        DetailRow(icon = Icons.Default.Schedule, label = "ISO-8601 UTC", value = currentRecord!!.isoTimestamp)
                        DetailRow(icon = Icons.Default.Person, label = "Operator", value = "${currentRecord!!.operatorName} (${currentRecord!!.operatorId})")
                        DetailRow(icon = Icons.Default.Person, label = "Agency", value = currentRecord!!.agency)
                        DetailRow(
                            icon = Icons.Default.LocationOn,
                            label = "GPS Location",
                            value = "${currentRecord!!.locationName}\n(${currentRecord!!.latitude}, ${currentRecord!!.longitude} ± ${currentRecord!!.locationAccuracy}m)"
                        )
                        if (currentRecord!!.notes.isNotBlank()) {
                            DetailRow(icon = Icons.Default.Security, label = "Notes", value = currentRecord!!.notes)
                        }
                    }
                }

                // Cryptographic Proof Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = VerifiedBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Cryptographic Proof & Digital Signature",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        HorizontalDivider()

                        Text(text = "Image SHA-256 Hash:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentRecord!!.imageHashSha256,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        )

                        Text(text = "HMAC-SHA256 Digital Signature:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = currentRecord!!.digitalSignature.ifEmpty { "Unsigned" },
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = VerifiedBlue,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        )

                        DetailRow(
                            icon = Icons.Default.Fingerprint,
                            label = "Device Key Fingerprint",
                            value = currentRecord!!.keyFingerprint.ifEmpty { "Standard Key" }
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.auditRecord(currentRecord!!) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Re-Verify Proof")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.deleteRecord(currentRecord!!.id)
                            onNavigateBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NegativeRed)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Record")
                    }
                }
            }
        }
    }
}

// Helper extension for putExtra in Intent
private fun Intent.putchar(key: String, value: String) {
    this.putExtra(key, value)
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
