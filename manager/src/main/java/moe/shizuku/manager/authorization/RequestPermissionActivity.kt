package moe.shizuku.manager.authorization

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppActivity
import moe.shizuku.manager.databinding.ConfirmationDialogBinding
import moe.shizuku.manager.ktx.toHtml
import moe.shizuku.manager.utils.Logger.LOGGER
import rikka.core.res.resolveColor
import rikka.html.text.HtmlCompat
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED
import rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_IS_ONETIME
import rikka.shizuku.server.ktx.workerHandler
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RequestPermissionActivity : AppActivity() {


    private fun setResult(requestUid: Int, requestPid: Int, requestCode: Int, allowed: Boolean, onetime: Boolean) {
        val data = Bundle()
        data.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed)
        data.putBoolean(REQUEST_PERMISSION_REPLY_IS_ONETIME, onetime)
        try {
            Shizuku.dispatchPermissionConfirmationResult(requestUid, requestPid, requestCode, data)
        } catch (e: Throwable) {
            LOGGER.e("dispatchPermissionConfirmationResult")
        }
    }

    private fun checkSelfPermission(): Boolean {
        val permission = Shizuku.checkRemotePermission("android.permission.GRANT_RUNTIME_PERMISSIONS") == PackageManager.PERMISSION_GRANTED
        if (permission) return true

        val icon = getDrawable(R.drawable.ic_system_icon)
        icon?.setTint(theme.resolveColor(android.R.attr.colorAccent))

        val dialog = MaterialAlertDialogBuilder(this)
                .setIcon(icon)
                .setTitle("Shizuku: ${getString(R.string.app_management_dialog_adb_is_limited_title)}")
                .setMessage(getString(R.string.app_management_dialog_adb_is_limited_message, Helps.ADB.get()).toHtml(HtmlCompat.FROM_HTML_OPTION_TRIM_WHITESPACE))
                .setPositiveButton(android.R.string.ok, null)
                .setOnDismissListener { finish() }
                .create()
        dialog.setOnShowListener {
        }
        try {
            dialog.show()
        } catch (ignored: Throwable) {
        }
        return false
    }

    private fun waitForBinder(): Boolean {
        val countDownLatch = CountDownLatch(1)

        val listener = object : Shizuku.OnBinderReceivedListener {
            override fun onBinderReceived() {
                countDownLatch.countDown()
                Shizuku.removeBinderReceivedListener(this)
            }
        }

        Shizuku.addBinderReceivedListenerSticky(listener, workerHandler)

        return try {
            countDownLatch.await(5, TimeUnit.SECONDS)
            true
        } catch (e: TimeoutException) {
            LOGGER.e(e, "Binder not received in 5s")
            false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!waitForBinder()) {
            finish()
            return
        }

        val uid = intent.getIntExtra("uid", -1)
        val pid = intent.getIntExtra("pid", -1)
        val requestCode = intent.getIntExtra("requestCode", -1)
        val ai = intent.getParcelableExtra<ApplicationInfo>("applicationInfo")
        if (uid == -1 || pid == -1 || ai == null) {
            finish()
            return
        }
        if (!checkSelfPermission()) {
            setResult(uid, pid, requestCode, allowed = false, onetime = true)
            return
        }

        val label = try {
            ai.loadLabel(packageManager)
        } catch (e: Exception) {
            ai.packageName
        }

        setContent {


            val context = LocalContext.current
            val packageManager = context.packageManager
            val applicationInfo = context.applicationInfo

            val icon: Drawable = packageManager.getApplicationIcon(ai)
            val bitmap = icon.toBitmap() // Convert Drawable to Bitmap
            val imageBitmap = remember { bitmap.asImageBitmap() } // Remember the Bitmap

            Box(modifier = Modifier
                .fillMaxSize()
            ) {
                Card(
                    modifier = Modifier
                        .wrapContentSize()
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(30.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black, //Card background color
                        contentColor = Color.Black  //Card content color,e.g.text
                    )
                ) {
                    Column {
                        Row {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Package Icon",
                                modifier = Modifier
                                    .size(60.dp)
                                    .padding(10.dp)
                            )
                            Text(text = "allow $label to use shizuku4quest?",
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                                    .padding(8.dp))

                        }
                        Button(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .padding(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            onClick = { setResult(uid, pid, requestCode, allowed = true, onetime = false)
                                finishAndRemoveTask()}
                        ) {
                            Text(text = "Allow all the time", color = Color.Black)
                        }

                        Button(modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .padding(horizontal = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            onClick = { setResult(uid, pid, requestCode, allowed = false, onetime = true)
                                finishAndRemoveTask()}
                        ) {
                            Text(text = "Deny", color = Color.Black)
                        }
                    }
                }
            }
        }


        /*val binding = ConfirmationDialogBinding.inflate(layoutInflater).apply {
            button1.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = true, onetime = false)
                dialog.dismiss()
            }
            button3.setOnClickListener {
                setResult(uid, pid, requestCode, allowed = false, onetime = true)
                dialog.dismiss()
            }
            title.text = HtmlCompat.fromHtml(getString(R.string.permission_warning_template,
                    label, getString(R.string.permission_group_description)))
        }

        dialog = MaterialAlertDialogBuilder(this)
                .setView(binding.root)
                .setCancelable(false)
                .setOnDismissListener { finish() }
                .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()*/
    }
}
