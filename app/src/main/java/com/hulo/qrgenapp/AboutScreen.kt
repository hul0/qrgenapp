package com.hulo.qrgenapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun AboutScreen(
    nativeAd: NativeAd?,
    showNativeAd: Boolean
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E3192), Color(0xFF1B1464))
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About App",
                        modifier = Modifier.size(34.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "About QRWiz",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Version 3.0.1",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Our Mission Card
            item {
                InfoCard(title = "Our Mission", icon = Icons.AutoMirrored.Filled.Label) {
                    Text(
                        text = "QRWiz is a versatile and user-friendly application designed to simplify your QR code interactions. Whether you need to quickly generate a QR code for sharing information or efficiently scan one to access content, our app provides a seamless experience.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            item {
                InfoCard(title = "Caution", icon = Icons.Default.WarningAmber) {
                    Text(
                        text = "All your coins and Diamonds are stored locally in your device storage. If you uninstall the app or clear the data of this app , ALL YOUR COINS & DIAMONDS WILL BE LOST!!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.8f)
                    )
                }
            }

            // Legal & Support Card
            item {
                InfoCard(title = "Support & Legal", icon = Icons.AutoMirrored.Filled.Help) {
                    // MODIFIED: Added a URL parameter to the expandable row for the privacy policy.

                        Spacer(modifier = Modifier.height(16.dp))
                        InfoRow(
                            icon = Icons.Default.Link,
                            text = "View Privacy Policy & TOS Online",
                            onClick = { openUrl(context, "https://hul0.github.io/qrpolicy" ) }
                        )

                    ExpandableLegalInfo(
                        icon = Icons.Default.Policy,
                        title = "Privacy Policy",
                        content = getPrivacyPolicyText()

                    )
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    ExpandableLegalInfo(
                        icon = Icons.Default.Gavel,
                        title = "Terms of Service",
                        content = getTermsOfServiceText()
                    )
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    InfoRow(
                        icon = Icons.Default.ContactMail,
                        text = "Support",
                        onClick = { openUrl(context, "mailto:nmrupam@proton.me") }
                    )
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    InfoRow(
                        icon = Icons.Default.StarRate,
                        text = "Rate this App",
                        onClick = { openUrl(context, "market://details?id=${context.packageName}") }
                    )
                }
            }

            // Developer Card
            item {
                InfoCard(title = "Developer", icon = Icons.Default.Person) {
                    Text(
                        text = "Developed with ❤️ by Hul0\nContact: nmrupam@proton.me or,\n workspace@republicwing.com",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Native Ad
            if (showNativeAd) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        // Assuming you have a NativeAdViewComposable defined elsewhere
                         NativeAdViewComposable(nativeAd = nativeAd, showAd = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Go to $text",
            tint = Color.White.copy(alpha = 0.7f)
        )
    }
}

// MODIFIED: This composable now accepts an optional URL to display a link button.
@Composable
private fun ExpandableLegalInfo(
    icon: ImageVector,
    title: String,
    content: String,
    url: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Color.White.copy(alpha = 0.7f)
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )

            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not open link.", android.widget.Toast.LENGTH_SHORT).show()
    }
}

// --- LEGAL TEXT HELPERS ---
// MODIFIED: Removed Markdown formatting (**) from the text.

private fun getPrivacyPolicyText(): String {
    return """
Last Updated: July 30, 2025

Hul0 ("we," "us," or "our") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, and disclose information about you when you use our mobile application, QRWiz (the "Service").

By using the Service, you agree to the collection and use of information in accordance with this policy.

1. Information We Collect

We collect information to provide and improve the Service. The types of information we collect are:

a) Information Stored Locally on Your Device:
We do not require you to create an account. The following information is generated and stored directly on your device using its internal storage (SharedPreferences) and is not transmitted to our servers:
- Virtual Currency Balance: Your current balance of "Coins" and "Diamonds."
- Premium Status: Whether you have upgraded to the Premium version.
- Scan History: A list of the QR codes you have scanned. For free users, this is limited to the last 20 scans.
- Redeemed Codes: A list of any special promotional codes you have redeemed.
- Usage Data: Daily login dates and streak information for the daily bonus feature.

b) Information Collected Automatically for Analytics and Advertising:
When you use our Service, we and our third-party partners may automatically collect certain information:
- Device Information: Device model, operating system version, and unique device identifiers (e.g., Google Advertising ID).
- Usage and Ad-Related Data: We collect data on your interactions with advertisements, such as the number of banner, interstitial, rewarded, and native ads you have viewed (impressions). This is used for our internal analytics to calculate Lifetime Value (LTV) and improve our Service.

2. How We Use Your Information

We use the collected information for the following purposes:
- To Provide and Maintain the Service: To operate the core functions of the app, such as generating QR codes, saving your scan history, and managing your in-app currency.
- To Personalize Your Experience: To provide features like the daily login bonus and track your progress.
- For Advertising: To display relevant advertisements in the free version of the app. Our third-party ad partner, Google AdMob, may use your device's advertising ID to serve personalized ads.
- For Analytics: To understand how our users interact with the Service and to improve it.

3. Third-Party Services & Data Sharing

We do not sell or rent your personal data. We may share information with third parties in the following circumstances:

- Advertising Partners: We partner with Google AdMob to serve ads in the free version of QRWiz. AdMob may collect and use data as described in Google's Privacy & Terms. We recommend you review their policy to understand how they handle data.
- Legal Requirements: We may disclose your information if required to do so by law or in response to valid requests by public authorities.

4. Permissions

QRWiz requests the following permissions on your device:
- CAMERA: This permission is essential for the core functionality of scanning QR codes directly through your device's camera.
- INTERNET: This permission is required to load and display advertisements from our third-party partners.

5. Data Security

We are committed to protecting your information. All data generated by your use of the app (like scan history and currency) is stored locally on your device. Please be aware that no method of transmission over the internet or method of electronic storage is 100% secure.

6. Children's Privacy

Our Service does not address anyone under the age of 13. We do not knowingly collect personally identifiable information from children under 13. If you are a parent or guardian and you are aware that your child has provided us with personal information, please contact us so that we will be able to do the necessary actions.

7. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. We will notify you of any changes by posting the new Privacy Policy within the app and updating the "Last Updated" date at the top of this policy.

8. Contact Us

If you have any questions about this Privacy Policy, please contact us at: workspace@republicwing.com
    """.trimIndent()
}

private fun getTermsOfServiceText(): String {
    return """
Last Updated: July 30, 2025

Welcome to QRWiz! These Terms of Service ("Terms") govern your use of the QRWiz mobile application (the "Service") provided by Hul0 ("we," "us," or "our"). By accessing or using our Service, you agree to be bound by these Terms.

1. Description of Service

QRWiz is a mobile application that allows users to generate and scan QR codes. The Service includes a freemium model with virtual in-app currencies ("Coins" and "Diamonds"), rewarded advertisements, and an optional "Premium" subscription that provides an ad-free experience and other benefits.

2. User Accounts & In-App Currencies

- Accounts: QRWiz does not require a formal user registration. Your data, including currency balances and history, is stored locally on your device. Consequently, if you uninstall the app or clear its data, your progress, currencies, and history may be permanently lost.
- Virtual Currencies: "Coins" and "Diamonds" are virtual points licensed to you for use within the Service. They have no real-world monetary value, cannot be redeemed for cash, and are not transferable. We reserve the right to manage, regulate, control, modify, or eliminate these virtual currencies at any time.
- Premium Service: The Premium subscription can be purchased using "Diamonds." This is a one-time, non-refundable purchase that unlocks premium features for the lifetime of the app on your device.

3. User-Generated Content

You are solely responsible for the content you use to generate QR codes ("User Content"). You agree not to create QR codes that link to or contain content that is illegal, harmful, threatening, defamatory, obscene, infringing, or otherwise objectionable. We do not monitor User Content but reserve the right to take appropriate action if we become aware of any violation of these Terms.

4. Prohibited Conduct

You agree not to:
- Use the Service for any illegal purpose or in violation of any local, state, national, or international law.
- Attempt to reverse engineer, decompile, or otherwise access the source code of the Service.
- Use any automated system to access or use the Service in a manner that sends more request messages to our servers than a human can reasonably produce in the same period.
- Attempt to cheat or manipulate the virtual currency system.

5. Advertisements

By using the free version of our Service, you consent to us and our third-party advertising partners (e.g., Google AdMob) displaying advertisements to you. These may include banner, interstitial, rewarded, and native ads. Opting for the Premium Service will remove these advertisements.

6. Disclaimers and Limitation of Liability

The Service is provided on an "AS IS" and "AS AVAILABLE" basis. We make no warranties, express or implied, regarding the operation or availability of the Service.

To the fullest extent permitted by law, Hul0 shall not be liable for any indirect, incidental, special, consequential, or punitive damages, or any loss of profits or revenues, whether incurred directly or indirectly, or any loss of data, use, goodwill, or other intangible losses, resulting from your use of the Service.

7. Termination

We may terminate or suspend your access to the Service at any time, without prior notice or liability, for any reason whatsoever, including without limitation if you breach the Terms.

8. Governing Law

These Terms shall be governed and construed in accordance with the laws of India, without regard to its conflict of law provisions. Our failure to enforce any right or provision of these Terms will not be considered a waiver of those rights.

9. Changes to Terms

We reserve the right, at our sole discretion, to modify or replace these Terms at any time. We will provide notice of any changes by updating the "Last Updated" date of these Terms.

10. Contact Us

If you have any questions about these Terms, please contact us at: workspace@republicwing.com
    """.trimIndent()
}
