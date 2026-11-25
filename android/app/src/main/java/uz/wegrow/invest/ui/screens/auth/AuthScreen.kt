package uz.wegrow.invest.ui.screens.auth

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.wegrow.invest.data.OtpRequestResult
import uz.wegrow.invest.ui.AppStrings
import uz.wegrow.invest.ui.components.StaggeredReveal
import uz.wegrow.invest.ui.components.WeGrowBrandLogo
import uz.wegrow.invest.ui.components.WeGrowButton
import uz.wegrow.invest.ui.components.WeGrowChip
import uz.wegrow.invest.ui.components.bouncyClickable
import uz.wegrow.invest.ui.theme.WeGrowRadius
import uz.wegrow.invest.ui.theme.WeGrowTextStyles
import uz.wegrow.invest.ui.theme.WeGrowTheme

@Composable
fun AuthScreen(
    pendingOtp: OtpRequestResult?,
    isLoading: Boolean,
    errorMessage: String?,
    strings: AppStrings,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (identifier: String, code: String) -> Unit,
    onResetOtp: () -> Unit,
    onContinueAsGuest: () -> Unit,
    onBack: () -> Unit
) {
    val colors = WeGrowTheme.colors
    var identifier by remember { mutableStateOf("+998") }
    var otpCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .bouncyClickable(haptic = true, onClick = onBack)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(1.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "←", style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            }
            Text(text = strings.authTitle, style = WeGrowTextStyles.TitleL, color = colors.textPrimary)
            Spacer(modifier = Modifier.size(44.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        StaggeredReveal(0) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                WeGrowBrandLogo(size = 64.dp)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (pendingOtp == null) strings.authHeading else strings.authOtpHeading,
                    style = WeGrowTextStyles.DisplayM,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (pendingOtp == null) strings.authSubtitle else "${strings.authOtpSentTo} ${pendingOtp.identifier}",
                    style = WeGrowTextStyles.BodyS,
                    color = colors.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StaggeredReveal(1) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(WeGrowRadius.xl))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(WeGrowRadius.xl))
                    .padding(18.dp)
            ) {
                if (pendingOtp == null) {
                    Text(
                        text = strings.authIdentifierLabel,
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        singleLine = true,
                        placeholder = { Text("+998 90 123 45 67 / email@domain.uz") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WeGrowRadius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeGrowButton(
                        text = if (isLoading) "..." else strings.authSendCodeBtn,
                        enabled = !isLoading && identifier.trim().length >= 5,
                        onClick = { onRequestOtp(identifier.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = strings.authCodeLabel,
                        style = WeGrowTextStyles.CaptionStrong,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it.filter(Char::isDigit) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("123456") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(WeGrowRadius.md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.brandPrimary,
                            unfocusedBorderColor = colors.border,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    if (!pendingOtp.devCode.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dev OTP: ${pendingOtp.devCode}",
                                style = WeGrowTextStyles.CaptionStrong,
                                color = colors.brandPrimary
                            )
                            WeGrowChip(
                                label = strings.authFillDevCode,
                                selected = false,
                                onClick = { otpCode = pendingOtp.devCode }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    WeGrowButton(
                        text = if (isLoading) "..." else strings.authVerifyBtn,
                        enabled = !isLoading && otpCode.length == 6,
                        onClick = { onVerifyOtp(pendingOtp.identifier, otpCode) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    WeGrowButton(
                        text = strings.authChangeContact,
                        outlined = true,
                        onClick = {
                            otpCode = ""
                            onResetOtp()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (!errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage,
                        style = WeGrowTextStyles.BodyS,
                        color = colors.statusHaram
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        StaggeredReveal(2) {
            WeGrowButton(
                text = strings.authGuestBtn,
                outlined = true,
                enabled = !isLoading,
                onClick = onContinueAsGuest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
