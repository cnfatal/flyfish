package cn.fatalc.flyfish

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import cn.fatalc.flyfish.ui.theme.FlyFishTheme
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlin.reflect.KClass

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            Theme {
                MainContent()
            }
        }
    }
}

@Composable
fun Theme(content: @Composable () -> Unit) {
    val systemUiController = rememberSystemUiController()
    val useDarkIcons = MaterialTheme.colors.isLight
    SideEffect {
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = useDarkIcons
        )
    }
    ProvideWindowInsets {
        FlyFishTheme {
            Surface(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxSize()
            ) {
                content()
            }
        }
    }
}


@Composable
fun MainContent() {
    val context = LocalContext.current
    var enabled by (remember { mutableStateOf(false) })

    OnLifecycleEvent { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME, Lifecycle.Event.ON_START,
            -> {
                enabled = isAccessServiceEnabled(context, AccessibilityService::class)
            }
            else -> Unit
        }
    }

    var openDialog by remember { mutableStateOf(false) }
    if (openDialog) {
        AlertDialog(
            onDismissRequest = {
                openDialog = false
            },
            title = {
                Text(text = "开启辅助功能")
            },
            text = {
                Text(
                    text = "点击 \"已下载的应用\"/\"已安装服务\" -> \"%s\" ,选择开启"
                        .format(
                            context.applicationInfo.loadLabel(context.packageManager).toString()
                        )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(context, intent, null)
                    }
                ) {
                    Text("知道了")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("不要")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!enabled) {
            Button(
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp),
                onClick = {
                    openDialog = true
                }
            ) {
                Text(
                    text = stringResource(R.string.enable_accessibility_service),
                )
            }
        } else {
            Button(
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp),
                colors = ButtonDefaults.buttonColors(),
                onClick = {}
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Rounded.Done,
                    contentDescription = stringResource(R.string.enabled_accessibility_service)
                )
            }
        }
    }
}

@Composable
fun OnLifecycleEvent(onEvent: (owner: LifecycleOwner, event: Lifecycle.Event) -> Unit) {
    val eventHandler = rememberUpdatedState(onEvent)
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwner.value) {
        val lifecycle = lifecycleOwner.value.lifecycle
        val observer = LifecycleEventObserver { owner, event ->
            eventHandler.value(owner, event)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}

fun isAccessServiceEnabled(context: Context, accessibilityServiceClass: KClass<*>): Boolean {
    return Settings.Secure.getString(
        context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ).orEmpty().contains(
        context.packageName + "/" + accessibilityServiceClass.qualifiedName
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultServicePreview() {
    Theme {
        MainContent()
    }
}