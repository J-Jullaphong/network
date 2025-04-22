package org.classapp.network

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import org.classapp.network.ui.theme.NetworkTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MyCardScreen(profile: UserProfile) {
    NetworkTheme {
        val context = LocalContext.current
        var showShareDialog by remember { mutableStateOf(false) }
        val graphicsLayer = rememberGraphicsLayer()

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth()
                    .drawWithContent {
                        graphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(graphicsLayer)
                    }
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ProfileCard(profile = profile)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Button(
                    onClick = { showShareDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Share")
                }

                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(context, ProfileSetupActivity::class.java)
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Edit")
                }
            }
        }

        if (showShareDialog) {
            ShareDialog(
                profile = profile,
                onDismiss = { showShareDialog = false },
                graphicsLayer = graphicsLayer
            )
        }
    }
}

@Composable
fun ProfileCard(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!profile.profileImage.isNullOrEmpty()) {
            ProfileImageWithGlide(profile.profileImage)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(profile.fullName, style = MaterialTheme.typography.headlineSmall)
        Text(profile.career, style = MaterialTheme.typography.bodyMedium)
        Text(profile.organization, style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))

        ContactRow("📞", profile.phone)
        ContactRow("✉️", profile.email)
        ContactRow("🌐", profile.website)
        ContactRow("📍", "${profile.city}, ${profile.country}")
        ContactRow("🔗", profile.linkedin)
        ContactRow("📸", profile.instagram)
        ContactRow("📘", profile.facebook)
        ContactRow("🐦", profile.twitter)
        ContactRow("💬", profile.whatsapp)
        ContactRow("✈️", profile.telegram)
        ContactRow("🎥", profile.skype)
        ContactRow("🟢", profile.weChat)
        ContactRow("📲", profile.line)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ProfileImageWithGlide(imageUrl: String) {
    val sizeDp = 120.dp
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { sizeDp.roundToPx() }

    AndroidView(
        factory = {
            ImageView(context).apply {
                layoutParams = ViewGroup.LayoutParams(sizePx, sizePx)
                scaleType = ImageView.ScaleType.CENTER_CROP

                Glide.with(this)
                    .load(imageUrl)
                    .override(sizePx, sizePx)
                    .circleCrop()
                    .into(this)
            }
        },
        modifier = Modifier.size(sizeDp)
    )
}

@Composable
fun ContactRow(icon: String, text: String?) {
    if (!text.isNullOrEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Text(icon, modifier = Modifier.width(48.dp))
            Text(text)
        }
    }
}

@Composable
fun ShareDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    graphicsLayer: GraphicsLayer
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val qrBitmap = remember(profile) {
        generateQrCode("network://profile/${profile.userId}")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = null,
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(profile.fullName, style = MaterialTheme.typography.titleLarge)
                Text(profile.career, style = MaterialTheme.typography.bodyMedium)
                Text(profile.organization, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            setImageBitmap(qrBitmap)
                        }
                    },
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                ShareActionRow("📋", "Copy Profile Link") {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Profile Link", "network://profile/${profile.userId}")
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Profile link copied to clipboard", Toast.LENGTH_SHORT).show()
                }

                ShareActionRow("📥", "Download QR Code") {
                    saveQrToGallery(context, qrBitmap)
                    Toast.makeText(context, "QR Code saved to gallery", Toast.LENGTH_SHORT).show()
                }

                ShareActionRow("📤", "Share Profile Card") {
                    coroutineScope.launch {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val uri = saveBitmapToCache(context, bitmap)

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        dismissButton = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
    )
}

@Composable
fun ShareActionRow(icon: String, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, modifier = Modifier.width(32.dp))
            Text(label)
        }
    }
}

fun generateQrCode(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri {
    val file = File(context.cacheDir, "shared_card.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
}

fun saveQrToGallery(context: Context, bitmap: Bitmap) {
    val filename = "QR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.png"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/NetworkQRs")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    uri?.let {
        resolver.openOutputStream(it).use { out ->
            if (out != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }
    }
}
