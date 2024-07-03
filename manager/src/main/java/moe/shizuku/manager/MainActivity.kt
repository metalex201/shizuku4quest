package moe.shizuku.manager;

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.provider.Settings.Global
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.topjohnwu.superuser.internal.UiThreadHandler.handler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.AdbMdns
import moe.shizuku.manager.adb.AdbPairingClient
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import moe.shizuku.manager.starter.Starter
import rikka.shizuku.Shizuku


@RequiresApi(Build.VERSION_CODES.O)
fun String.runCommand(): String? {
    try {
        val context = application.applicationContext
        val parts = this.split("\\s".toRegex())
        val procbuild = ProcessBuilder(*parts.toTypedArray())
            .directory(context.filesDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
        procbuild.environment().apply {
            put("HOME", context.filesDir.path)
            put("TMPDIR", context.cacheDir.path)
        }
        return procbuild.start().inputStream.bufferedReader().readText()
    } catch(e: Exception) {
        e.printStackTrace()
        return null
    }
}
fun SecureSettingsAllowed(context: Context): Boolean {
    val packageInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
    val list: List<String>  = packageInfo.requestedPermissions!!.filterIndexed { index, permission ->
        (packageInfo.requestedPermissionsFlags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
    }
    if (list.contains("android.permission.WRITE_SECURE_SETTINGS")) { return true } else {return false}
}

fun launchActivityPage(activity:Activity, page:Int, withsettings:Boolean) {
    val context = application
    GlobalScope.launch {
        if (withsettings)  {
            delay(100)
            val intent = Intent("com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS")

            intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
            application.startActivity(intent)
            delay(200)
        }

        val intentt = Intent(context, MainActivity::class.java)
        intentt.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intentt.putExtra("page", page)
        context.startActivity(intentt)
    }
    activity.finishAndRemoveTask()
}

class StartService() : Service() {
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            Thread {
                val context = getApplicationContext();
                val bootAdb = adbHandler(context)
                Global.putLong(context.getContentResolver(), "adb_allowed_connection_time", 0L)
                Global.putInt(context.getContentResolver(), "adb_wifi_enabled", 1);
                bootAdb.startupSetup()
            }.start()

            return START_STICKY  // Consider using START_STICKY to restart the service if killed
        }
        override fun onBind(intent: Intent?): IBinder? {
            return null  // We don't intend to bind to this service from other components
        }
}

class LaunchService() : Service() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        return START_STICKY  // Consider using START_STICKY to restart the service if killed
    }
    override fun onBind(intent: Intent?): IBinder? {
        return null  // We don't intend to bind to this service from other components
    }
}

class BootUpReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("AAAA", "startupsetup")
        //val serviceIntent = Intent(context, StartService::class.java)
        //context.startService(serviceIntent)
        val intent = Intent(context, LaunchActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

var CustomColorScheme =
    darkColorScheme(
        background = Color(27, 43, 59, 255),
        surface = Color(50,63,76,255),
        error = Color(187,0,19,255),
        onSurface = Color(240,244,245,255),
    )

class MainActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        Starter.writeSdcardFiles(applicationContext)
        super.onCreate(savedInstanceState)

        
        setContent {
            val page by remember { mutableStateOf(intent.getIntExtra("page", 0)) }

            MaterialTheme(colorScheme = CustomColorScheme) {
                Surface (modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background)
                {   
                    if (page == 0) {
                        mainPage()
                    }
                    if (page == 1) {
                        PairingPage()
                    }
                    if (page == 2) {
                        AUthPage()
                    }
                    if (page == 3) {
                        SettingsPage()
                    }
                }
            }
        }
    }
}

@Composable
fun AUthPage() {}

@Composable
fun SettingsPage() {
    val activity = (LocalContext.current as Activity)
    val context = LocalContext.current

    val cardcolors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface)

    val cardmodifier = Modifier
        .wrapContentSize()
        .fillMaxWidth()
        .padding(20.dp, 10.dp)
        .clip(RoundedCornerShape(100.dp))

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {

            Card(modifier = cardmodifier, colors = cardcolors,
                onClick = { launchActivityPage(activity, 3, true) },
            ) {
                Row(Modifier.padding(25.dp)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Settings"
                        )
                    }
                    var checked by remember { mutableStateOf(true) }
                    Switch(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                        }
                    )
                }
            }
        }
    }
}




@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun mainPage() {
    val activity = (LocalContext.current as Activity)
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(Shizuku.pingBinder()) }
    GlobalScope.launch {
        while (true) {
            delay(5)
            val newValue = Shizuku.pingBinder()
            withContext(Dispatchers.Main) {
                isRunning = newValue  // Update the state of shizuku server on the main thread
            }
        }
    }

    // PRESETS

    val cardcolors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    val cardmodifier = Modifier
        .wrapContentSize()
        .fillMaxWidth()
        .padding(20.dp, 10.dp)
        .clip(RoundedCornerShape(100.dp))

    // ------

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.align(Alignment.Center)) {
            Card(modifier = cardmodifier, colors = cardcolors)
            {
                Row(Modifier.padding(25.dp)) {
                    Icon(
                        imageVector = if (isRunning) Icons.Filled.Check else Icons.Filled.Warning,
                        contentDescription = ""
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = if (isRunning) "Shizuku is running :D" else "Shizuku is not running D:"
                        )
                    }
                }
            }
            Card(modifier = cardmodifier, colors = cardcolors,
                onClick = { launchActivityPage(activity, 3, true) },
            ) {
                Row(Modifier.padding(25.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Build,
                        contentDescription = "",
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Settings"
                        )
                    }
                }
            }

            if (isRunning) {
                Card(modifier = cardmodifier, colors = cardcolors,
                    onClick = {launchActivityPage(activity, 2, true)}
                ) {
                    Row(Modifier.padding(25.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Build,
                            contentDescription = "",
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                modifier = Modifier.align(Alignment.Center),
                                text = "Authorized apps"
                            )
                        }
                    }
                }
            }

            Card(
                modifier = cardmodifier,  colors = cardcolors,
                onClick = {launchActivityPage(activity, 1, true)})
            {
                Row(Modifier.padding(25.dp)) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "",
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Start Pairing process"
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun PairingPage() {
    val activity = (LocalContext.current as Activity)
    val context = LocalContext.current
    val pairingAdb = adbHandler(context)

    var code by remember { mutableStateOf("") }
    var showPairing by remember { mutableStateOf(false) }

    fun setPairing(state: Boolean) { showPairing = state } // state updater function
    pairingAdb.startPairing { state -> setPairing(state)} // update port number

    Box(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .wrapContentSize()
                .fillMaxWidth()
                .padding(20.dp, 10.dp)
                .clip(RoundedCornerShape(100.dp))
                .align(Alignment.Center),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Column() {
                Text(modifier = Modifier.padding(25.dp), text = "Scroll down, Enable and tap on \"Wireless debugging\" then tap on \" Pair device with pairing code\"")
                if (showPairing) {
                    TextField(
                        value = code,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                code = newValue
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(textAlign = TextAlign.Center)
                    )

                    Button(modifier = Modifier.align(Alignment.CenterHorizontally) , onClick = {
                        GlobalScope.launch {
                            finishPairing(code, pairingAdb.getPort())
                            pairingAdb.StopPairing()
                            delay(500)
                            pairingAdb.startupSetup()
                            delay(500)
                            launchActivityPage(activity, 0, false)
                        }
                    }) {
                        Text("run")
                    }
                }
            }

        }

    }
}


@SuppressLint("CustomSplashScreen")
class LaunchActivity : ComponentActivity() {
    @SuppressLint("CoroutineCreationDuringComposition")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        Starter.writeSdcardFiles(applicationContext)
        super.onCreate(savedInstanceState)

        val context = getApplicationContext()
        if(SecureSettingsAllowed(context)) {
            val serviceIntent = Intent(context, StartService::class.java)
            context.startService(serviceIntent)
        }

        setContent {
            val activity = (LocalContext.current as? Activity)
            var finished by remember { mutableStateOf(false) }
            var label by remember { mutableStateOf("Launching Shizuku4quest") }

            LaunchedEffect(Unit) {
                if(!SecureSettingsAllowed(context)) {
                    finished = true
                    label = "Error, missing permission D:"
                    delay(10)
                    activity?.finish()
                }
                while (true) {
                    delay(5)
                    if(Shizuku.pingBinder()) {
                        finished = true
                        label = "Launched :D"
                        delay(10)
                        activity?.finish()
                    }
                }
            }

            Box(modifier = Modifier
                .fillMaxSize()
                .background(color = CustomColorScheme.background)
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(30.dp))
                    , colors = CardDefaults.cardColors(
                        containerColor = CustomColorScheme.surface,
                        contentColor = CustomColorScheme.onSurface
                    )
                ) {
                    Row {
                        if(finished) {
                            Text(modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(10.dp), text = label)
                        } else {
                            CircularProgressIndicator(modifier = Modifier.padding(10.dp))
                            Text(modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .padding(10.dp), text = label)
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
class adbHandler(context: Context) {
    val context = context
    val PairPort = MutableLiveData<Int>()
    val adbMdns = AdbMdns(context, AdbMdns.TLS_PAIRING, PairPort)
    var PairingPort = 0
    var observer = Observer<Int> { port -> }
    var connectPort = 0

    fun getPort(): Int {
        return PairingPort
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun startPairing(updateState: (Boolean) -> Unit) {

        observer = Observer{ port ->
            if (port in 0..65535) {
                PairingPort = port
                updateState(true)
                Log.i("shizuku", "PAIRING PORT FOUND: " + port)
            } else {
                updateState(false)
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            PairPort.observeForever(observer)
        } else {
            handler.post { PairPort.observeForever(observer) }
        }

        adbMdns.start()
    }
    @RequiresApi(Build.VERSION_CODES.R)
    fun StopPairing() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            PairPort.removeObserver(observer)
        } else {
            handler.post { PairPort.removeObserver(observer) }
        }
        adbMdns.stop()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startConnecting() {
        val adbPort = MutableLiveData<Int>()
        val adbConnect = AdbMdns(context, AdbMdns.TLS_CONNECT, adbPort)

        val ConnectObserver = Observer<Int> { port ->
            if (port in 0..65535) {
                Log.i(context.packageName, "port: " + port)
                connectPort = port
            }
            //adbConnect.stop()
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            adbPort.observeForever(ConnectObserver)
        } else {
            handler.post { adbPort.observeForever(ConnectObserver) }
        }

        adbConnect.start()

    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun startupSetup() {
        val adbPort = MutableLiveData<Int>()
        val adbConnect = AdbMdns(context, AdbMdns.TLS_CONNECT, adbPort)
        Log.i("AAAA", "launch")
        val ConnectObserver = Observer<Int> { port ->
            if (port in 0..65535) {
                Log.i(context.packageName, "port: " + port)
                Log.i("AAAA", "port: " + port)
                adbConnect.stop()
                GlobalScope.launch() {
                    while(true) {
                        launchShizuku(port)
                        delay(10000)
                        if (Shizuku.pingBinder() != false) {break}
                    }
                }
                FirstLaunch(port)
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            adbPort.observeForever(ConnectObserver)
        } else {
            handler.post { adbPort.observeForever(ConnectObserver) }
        }

        adbConnect.start()
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun finishPairing(code: String, port: Int) {
    GlobalScope.launch(Dispatchers.IO) {
        val host = "127.0.0.1"

        val key = try {
            AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
        } catch (e: Throwable) {
            e.printStackTrace()
            return@launch
        }

        AdbPairingClient(host, port, code, key).runCatching {
            start()
        }.onFailure {
        }.onSuccess {
        }
    }

}

@RequiresApi(Build.VERSION_CODES.R)
private fun launchShizuku(port: Int) {
    Log.i("AAAA", "LAUNCHING SHIZUKU")
    GlobalScope.launch(Dispatchers.IO) {
        val host = "127.0.0.1"

        val key = try {
            AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
        } catch (e: Throwable) {
            e.printStackTrace()
            return@launch
        }

        AdbClient(host, port, key).runCatching {
            connect()
            shellCommand(Starter.sdcardCommand) {
                Log.i("AAAA", String(it))
            }
            close()
        }.onFailure {
            it.printStackTrace()
        }
    }

}

@RequiresApi(Build.VERSION_CODES.R)
private fun FirstLaunch(port: Int) {
    Log.i("AAAA", "LAUNCHING SHIZUKU")
    GlobalScope.launch(Dispatchers.IO) {
        val host = "127.0.0.1"

        val key = try {
            AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
        } catch (e: Throwable) {
            e.printStackTrace()
            return@launch
        }

        AdbClient(host, port, key).runCatching {
            connect()
            shellCommand("pm grant moe.shizuku.privileged.api android.permission.WRITE_SECURE_SETTINGS") {}
            shellCommand("pm grant moe.shizuku.privileged.api android.permission.RECEIVE_BOOT_COMPLETED") {}
            shellCommand(Starter.sdcardCommand) {}
            close()
        }.onFailure {
            it.printStackTrace()
        }
    }

}

