package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.repository.AuthRepository
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.LanguageToggleChip

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.minimumInteractiveComponentSize

@Composable
fun SignInScreen(
    authRepository: AuthRepository,
    credentialManager: CredentialManager? = null,
    selectedRole: String = "tenant",
    onRoleSelected: (String) -> Unit = {},
    onLanguageChange: (AppLanguage) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activeCredentialManager = remember(context, credentialManager) {
        credentialManager ?: CredentialManager.create(context)
    }
    val strings = LocalStrings.current
    val language = LocalLanguage.current

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Apartment,
                            contentDescription = strings.appName,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "SMART RENT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LanguageToggleChip(
                    currentLanguage = language,
                    onToggle = onLanguageChange
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    Image(
                        painter = painterResource(id = R.drawable.img_auth_hero),
                        contentDescription = "Smart Rent Manager Building Illustration",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp),
                        contentScale = ContentScale.Crop
                    )
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = strings.appName,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = strings.tagline,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (language == AppLanguage.BN) {
                                "প্রথমে আপনার ভূমিকা নির্বাচন করুন:"
                            } else {
                                "First, choose how you want to use the app:"
                            },
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            RoleSelectorCard(
                                title = strings.roleAdminBadge,
                                subtitle = if (language == AppLanguage.BN) "নিজের ভবন ও ভাড়াটিয়া ম্যানেজ করুন" else "Create & manage your own buildings",
                                icon = Icons.Filled.Apartment,
                                selected = selectedRole == "admin",
                                testTag = "select_role_owner_btn",
                                onClick = { onRoleSelected("admin") },
                                modifier = Modifier.weight(1f)
                            )
                            RoleSelectorCard(
                                title = strings.roleTenantBadge,
                                subtitle = if (language == AppLanguage.BN) "বিল দেখুন ও রিসিট/প্রমাণ দিন" else "View bills & submit payment proof",
                                icon = Icons.Filled.Home,
                                selected = selectedRole == "tenant",
                                testTag = "select_role_tenant_btn",
                                onClick = { onRoleSelected("tenant") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (errorMessage != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        val buttonLabel = if (selectedRole == "admin") {
                            if (language == AppLanguage.BN) "মালিক হিসেবে Google দিয়ে লগইন করুন"
                            else "Sign in as Owner with Google"
                        } else {
                            if (language == AppLanguage.BN) "ভাড়াটিয়া হিসেবে Google দিয়ে লগইন করুন"
                            else "Sign in as Tenant with Google"
                        }

                        Button(
                            onClick = {
                                errorMessage = null
                                isSigningIn = true
                                authRepository.onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = activeCredentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { err ->
                                        isSigningIn = false
                                        errorMessage = err
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(strings.signingIn)
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = buttonLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (language == AppLanguage.BN) {
                                    "সুরক্ষিত Google লগইন ও প্রতিবেশী/মালিকদের জন্য আলাদা ওয়ার্কস্পেস"
                                } else {
                                    "Secured Google Sign-In & Isolated Workspaces per Owner"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoleSelectorCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = if (selected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
fun WorkspaceSetupScreen(
    userEmail: String,
    onSelectOwner: () -> Unit,
    onSelectTenant: () -> Unit,
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = strings.setupOwnerTitle,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "$userEmail\n${strings.setupOwnerSubtitle}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSelectOwner,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("setup_as_owner_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Apartment, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.setupAsOwnerBtn)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSelectTenant,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("setup_as_tenant_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Filled.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.setupAsTenantBtn)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSignOut) {
            Text(strings.signOut)
        }
    }
}

@Composable
fun InactiveAccountScreen(
    email: String,
    onSignOut: () -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Block,
            contentDescription = "Account Inactive",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (language == AppLanguage.BN) "অ্যাকাউন্ট নিষ্ক্রিয় করা হয়েছে" else "Account Deactivated",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (language == AppLanguage.BN) {
                "$email অ্যাকাউন্টটি মালিক কর্তৃক সাময়িকভাবে নিষ্ক্রিয় আছে। অনুগ্রহ করে বাড়ির মালিকের সাথে যোগাযোগ করুন।"
            } else {
                "$email has been deactivated by the property owner. Please contact management."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSignOut) {
            Text(strings.signOut)
        }
    }
}
