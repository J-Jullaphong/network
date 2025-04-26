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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.fontawesome.FontAwesome
import com.mikepenz.iconics.typeface.library.simpleicons.SimpleIcons
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.launch
import org.classapp.network.ui.theme.NetworkTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
fun MyCardScreen(profile: UserProfile) {
    NetworkTheme {
        val context = LocalContext.current
        var showShareDialog by remember { mutableStateOf(false) }
        val graphicsLayer = rememberGraphicsLayer()
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
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

            Spacer(modifier = Modifier.height(24.dp))

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
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.intersect),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth()
            )

            if (!profile.profileImage.isNullOrEmpty()) {
                ProfileImageWithGlide(
                    imageUrl = profile.profileImage,
                    modifier = Modifier
                        .size(180.dp)
                        .align(Alignment.BottomCenter)
                        .offset(y = 24.dp)
                )
            }
        }

        if (!profile.profileImage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(48.dp))
        }

        Text(profile.fullName, style = MaterialTheme.typography.headlineSmall)
        if (!profile.career.isNullOrEmpty()) {
            Text(profile.career, style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp)
        }
        if (!profile.organization.isNullOrEmpty()) {
            Text(
                profile.organization,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.padding(horizontal = 32.dp)) {
            ContactRow(FontAwesome.Icon.faw_phone, profile.phone)
            ContactRow(FontAwesome.Icon.faw_envelope, profile.email)
            ContactRow(FontAwesome.Icon.faw_globe, profile.website)
            val location = listOfNotNull(
                profile.city.takeIf { it.isNotBlank() },
                profile.country.takeIf { it.isNotBlank() }
            ).joinToString(", ")

            ContactRow(FontAwesome.Icon.faw_map_pin, location.ifBlank { "" })
            ContactRow(SimpleIcons.Icon.sim_linkedin, profile.linkedin)
            ContactRow(SimpleIcons.Icon.sim_instagram, profile.instagram)
            ContactRow(SimpleIcons.Icon.sim_facebook, profile.facebook)
            ContactRow(SimpleIcons.Icon.sim_twitter, profile.twitter)
            ContactRow(SimpleIcons.Icon.sim_whatsapp, profile.whatsapp)
            ContactRow(SimpleIcons.Icon.sim_telegram, profile.telegram)
            ContactRow(SimpleIcons.Icon.sim_skype, profile.skype)
            ContactRow(SimpleIcons.Icon.sim_wechat, profile.weChat)
            ContactRow(SimpleIcons.Icon.sim_line, profile.line)
        }
    }
}


@Composable
fun ProfileImageWithGlide(imageUrl: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { 240.dp.roundToPx() }

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
        modifier = modifier
    )
}

@Composable
fun ContactRow(icon: IIcon, text: String?) {
    val context = LocalContext.current

    if (!text.isNullOrEmpty()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable {
                    val clipboard =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Contact Info", text)
                    clipboard.setPrimaryClip(clip)

                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                }
        ) {
            val iconDrawable = IconicsDrawable(LocalContext.current, icon)
                .apply {
                    sizeDp = 16
                    colorInt = Color.White.toArgb()
                }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF6299E4), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { context ->
                        ImageView(context).apply {
                            setImageDrawable(iconDrawable)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
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
        dismissButton = {},
        text = {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 50.dp)
                ) {
                    Text(profile.fullName, style = MaterialTheme.typography.titleMedium)
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

                    Column(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        ShareActionRow(FontAwesome.Icon.faw_copy, "Copy Profile Link") {
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText(
                                "Profile Link",
                                "network://profile/${profile.userId}"
                            )
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(
                                context,
                                "Profile link copied to clipboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        ShareActionRow(FontAwesome.Icon.faw_download, "Download QR Code") {
                            saveQrToGallery(context, qrBitmap)
                            Toast.makeText(context, "QR Code saved to gallery", Toast.LENGTH_SHORT)
                                .show()
                        }

                        ShareActionRow(FontAwesome.Icon.faw_share, "Share Profile Card") {
                            coroutineScope.launch {
                                val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                                val uri = saveBitmapToCache(context, bitmap)

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share via"
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    )
}

@Composable
fun ShareActionRow(icon: IIcon, label: String, onClick: () -> Unit) {
    val context = LocalContext.current

    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        AndroidView(
            factory = {
                ImageView(it).apply {
                    val drawable = IconicsDrawable(context, icon).apply {
                        sizeDp = 20
                        colorInt = Color.Black.toArgb()
                    }
                    setImageDrawable(drawable)
                }
            },
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.CenterVertically)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
    }
}

fun generateQrCode(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = createBitmap(size, size)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap[x, y] =
                if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
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
