package br.com.tec.tecmotors

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.tec.tecmotors.R

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

/**
 * Marca + nome na barra superior.
 *
 * O nome e texto, nao imagem: acompanha o tema sem precisar de um PNG claro e
 * outro escuro, e fica nitido em qualquer densidade.
 */
@Composable
internal fun AppTopBarTitle() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_tecmotors_mark),
            contentDescription = stringResource(R.string.logo_tecmotors_light),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(30.dp)
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = MaterialTheme.colorScheme.onSurface
        )
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
