package br.com.tec.tecmotors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import br.com.tec.tecmotors.R

private const val LOGO_DRAWABLE_NAME = "logo_tecmotors_light"
private const val LOGO_DRAWABLE_DARK_NAME = "logo_tecmotors_dark"

@Composable
internal fun AppVersionBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionText = remember(context) {
        runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "v${packageInfo.versionName ?: "-"} ($versionCode)"
        }.getOrDefault("v-")
    }

    Text(
        text = versionText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                shape = RectangleShape
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    )
}

@Composable
internal fun AppTopBarTitle(isDarkTheme: Boolean) {
    val logoId = drawableIdByName(if (isDarkTheme) LOGO_DRAWABLE_DARK_NAME else LOGO_DRAWABLE_NAME)
    val fallbackLogoId = drawableIdByName(LOGO_DRAWABLE_NAME)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        if (logoId != 0 || fallbackLogoId != 0) {
            Image(
                painter = painterResource(id = if (logoId != 0) logoId else fallbackLogoId),
                contentDescription = stringResource(R.string.logo_tecmotors_light),
                contentScale = ContentScale.Fit,
                modifier = Modifier.height(44.dp)
            )
        } else {
            Text(stringResource(R.string.app_name))
        }
    }
}

@Composable
private fun drawableIdByName(name: String): Int {
    val context = LocalContext.current
    return remember(name, context) {
        context.resources.getIdentifier(name, "drawable", context.packageName)
    }
}

internal fun resolveGoogleClientId(context: Context): String? {
    val manual = context.getString(R.string.google_web_client_id).trim()
    if (manual.isNotEmpty()) return manual

    val generatedResId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    if (generatedResId != 0) {
        val generated = context.getString(generatedResId).trim()
        if (generated.isNotEmpty()) return generated
    }

    return null
}
