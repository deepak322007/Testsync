package com.example.drugdetector.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.drugdetector.R
import com.example.drugdetector.ui.theme.VerifiedBlue
import com.example.drugdetector.ui.viewmodel.DrugDetectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: DrugDetectorViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current

    val userEmail by viewModel.userEmail.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val operatorName by viewModel.operatorName.collectAsState()
    val agency by viewModel.agency.collectAsState()
    val operatorId by viewModel.operatorId.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) } // 0 = Login, 1 = Signup
    var isLoading by remember { mutableStateOf(false) }

    // Email OTP Modal State
    var showEmailOtpSheet by remember { mutableStateOf(false) }
    var pendingEmail by remember { mutableStateOf("") }
    var emailOtpInput by remember { mutableStateOf("") }
    var isVerifyingOtp by remember { mutableStateOf(false) }

    // Forgot Password Modal State
    var showForgotPasswordSheet by remember { mutableStateOf(false) }
    var forgotEmailInput by remember { mutableStateOf("") }
    var forgotOtpInput by remember { mutableStateOf("") }
    var forgotNewPasswordInput by remember { mutableStateOf("") }
    var isSendingForgotOtp by remember { mutableStateOf(false) }
    var isResettingPassword by remember { mutableStateOf(false) }
    var forgotStep by remember { mutableIntStateOf(1) } // 1 = Request, 2 = Reset

    // Login Fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Signup Fields
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    var signupBadge by remember { mutableStateOf("") }
    var signupBranch by remember { mutableStateOf("") }
    var signupRank by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Authentication", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Emblem
            Surface(
                color = Color(0xFF1E293B),
                shape = CircleShape,
                shadowElevation = 8.dp,
                modifier = Modifier.size(88.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_splash_logo),
                        contentDescription = "TestSync Logo",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TestSync",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Secure Officer Identity Portal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            if (isLoggedIn) {
                // Active Authenticated Profile Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = VerifiedBlue.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = operatorName.ifBlank { "U" }.take(1).uppercase(),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = VerifiedBlue
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = VerifiedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUTHENTICATED OPERATOR",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VerifiedBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "$operatorName • $agency ($operatorId)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Signed out", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Auth Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = { selectedTabIndex = 0 },
                                text = { Text("Sign In", fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = { selectedTabIndex = 1 },
                                text = { Text("Sign Up", fontWeight = FontWeight.Bold) }
                            )
                        }

                        if (isLoading) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Connecting...", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        } else if (selectedTabIndex == 0) {
                            // Sign In Form
                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                label = { Text("Email Address or Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            TextButton(
                                onClick = {
                                    forgotEmailInput = loginEmail
                                    forgotStep = 1
                                    showForgotPasswordSheet = true
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Forgot Password?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (loginEmail.isBlank() || loginPassword.isBlank()) {
                                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.loginWithBackend(loginEmail, loginPassword) { success, message, targetEmail ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else if (message == "VERIFY_EMAIL") {
                                            pendingEmail = targetEmail
                                            showEmailOtpSheet = true
                                            Toast.makeText(context, "Please verify the OTP sent to your email", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Sign In", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        } else {
                            // Sign Up Form
                            OutlinedTextField(
                                value = signupName,
                                onValueChange = { signupName = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupEmail,
                                onValueChange = { signupEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupPhone,
                                onValueChange = { input ->
                                    if (input.length <= 10 && input.all { char -> char.isDigit() }) {
                                        signupPhone = input
                                    }
                                },
                                label = { Text("Phone Number (10 digits)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupBadge,
                                onValueChange = { signupBadge = it },
                                label = { Text("Badge / Operator ID") },
                                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupBranch,
                                onValueChange = { signupBranch = it },
                                label = { Text("Branch / Department / Agency") },
                                leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupRank,
                                onValueChange = { signupRank = it },
                                label = { Text("Rank") },
                                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = signupPassword,
                                onValueChange = { signupPassword = it },
                                label = { Text("Password (min 6 chars)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (signupName.isBlank() || signupEmail.isBlank() || signupBranch.isBlank() || signupRank.isBlank() || signupPassword.length < 6) {
                                        Toast.makeText(context, "Please fill in all required fields (password min 6 chars)", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signupPhone.isNotBlank() && signupPhone.length != 10) {
                                        Toast.makeText(context, "Phone number must be exactly 10 digits", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.signupWithBackend(
                                        name = signupName,
                                        email = signupEmail,
                                        phone = signupPhone,
                                        password = signupPassword,
                                        branchVal = signupBranch,
                                        rankVal = signupRank,
                                        badgeIdVal = signupBadge
                                    ) { success, message, targetEmail ->
                                        isLoading = false
                                        if (success) {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                            onLoginSuccess()
                                        } else if (message == "VERIFY_EMAIL") {
                                            pendingEmail = targetEmail
                                            showEmailOtpSheet = true
                                            Toast.makeText(context, "Verification OTP code sent to your email", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Email Verification Modal Bottom Sheet
    if (showEmailOtpSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmailOtpSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Verify Email Address",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "A 6-digit verification OTP code has been sent to:\n$pendingEmail",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = emailOtpInput,
                    onValueChange = { emailOtpInput = it },
                    label = { Text("6-Digit Verification Code") },
                    placeholder = { Text("e.g. 482910") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (emailOtpInput.isBlank()) {
                            Toast.makeText(context, "Please enter the 6-digit OTP code", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isVerifyingOtp = true
                        viewModel.verifyEmailOtp(pendingEmail, emailOtpInput) { success, message ->
                            isVerifyingOtp = false
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showEmailOtpSheet = false
                                onLoginSuccess()
                            }
                        }
                    },
                    enabled = !isVerifyingOtp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isVerifyingOtp) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                    } else {
                        Text("Verify & Complete Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                TextButton(
                    onClick = {
                        viewModel.resendEmailOtp(pendingEmail) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Resend Verification Code", fontSize = 13.sp)
                }
            }
        }
    }

    // Forgot Password Modal Bottom Sheet
    if (showForgotPasswordSheet) {
        ModalBottomSheet(
            onDismissRequest = { showForgotPasswordSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reset Your Password",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (forgotStep == 1) {
                    Text(
                        text = "Enter your registered email address to receive a 6-digit password reset code.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = forgotEmailInput,
                        onValueChange = { forgotEmailInput = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (forgotEmailInput.isBlank()) {
                                Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSendingForgotOtp = true
                            viewModel.forgotPassword(forgotEmailInput) { success, message ->
                                isSendingForgotOtp = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    forgotStep = 2
                                }
                            }
                        },
                        enabled = !isSendingForgotOtp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isSendingForgotOtp) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                        } else {
                            Text("Send Reset Code", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    Text(
                        text = "Enter the 6-digit reset code sent to:\n$forgotEmailInput\nand create a new password.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = forgotOtpInput,
                        onValueChange = { forgotOtpInput = it },
                        label = { Text("6-Digit Reset Code") },
                        placeholder = { Text("e.g. 482910") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = forgotNewPasswordInput,
                        onValueChange = { forgotNewPasswordInput = it },
                        label = { Text("New Password (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (forgotOtpInput.isBlank() || forgotNewPasswordInput.length < 6) {
                                Toast.makeText(context, "Please enter OTP and new password (min 6 chars)", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isResettingPassword = true
                            viewModel.resetPassword(forgotEmailInput, forgotOtpInput, forgotNewPasswordInput) { success, message ->
                                isResettingPassword = false
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    showForgotPasswordSheet = false
                                    onLoginSuccess()
                                }
                            }
                        },
                        enabled = !isResettingPassword,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isResettingPassword) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                        } else {
                            Text("Reset Password & Sign In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    TextButton(
                        onClick = { forgotStep = 1 }
                    ) {
                        Text("Resend Code to another email", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
