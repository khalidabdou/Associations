package org.associations.project.ui.activation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.associations.project.repository.LicenseResult
import org.associations.project.viewmodel.ActivationViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ActivationScreen(
        onNavigateToDashboard: () -> Unit,
        viewModel: ActivationViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var licenseKey by remember { mutableStateOf("") }

    // Check on launch
    LaunchedEffect(Unit) {
        if (viewModel.checkActivation()) {
            onNavigateToDashboard()
        }
    }

    // React to success state
    LaunchedEffect(uiState) {
        if (uiState is LicenseResult.Success) {
            onNavigateToDashboard()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            Text(text = "Device Activation", style = MaterialTheme.typography.headlineMedium)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                    value = licenseKey,
                    onValueChange = { licenseKey = it },
                    label = { Text("License Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                    onClick = { viewModel.activate(licenseKey) {} },
                    enabled = uiState !is LicenseResult.Loading && licenseKey.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is LicenseResult.Loading) {
                    CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                    )
                } else {
                    Text("Activate")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is LicenseResult.Error) {
                Text(
                        text = (uiState as LicenseResult.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
