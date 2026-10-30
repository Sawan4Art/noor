package com.sawan4art.noor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sawan4art.noor.ui.NoorApp
import com.sawan4art.noor.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {

    /**
     * الأذونات تُطلب في حوارات منفصلة بالتتابع — درس v0.4.2:
     * الطلب المجمّع يجعل بعض الأجهزة تعرض حوار الموقع فقط وتُسقط الباقي صامتاً.
     * والموقع أولاً ثم الإشعارات، والممنوح يُتخطى تلقائياً.
     */
    private val permissionQueue = ArrayDeque<List<String>>()
    private var permissionFlowStarted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        // بغض النظر عن النتيجة: الحوار التالي في الطابور — والمرفوض نهائياً
        // يمرّ صامتاً فوراً فتنهي السلسلة بالحالة الفعلية.
        launchNextPermissionGroup()
    }

    private fun enqueueMissingPermissions() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val notifications = Manifest.permission.POST_NOTIFICATIONS

        if (ContextCompat.checkSelfPermission(this, fine) != PackageManager.PERMISSION_GRANTED) {
            permissionQueue.add(listOf(fine, coarse))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, notifications) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionQueue.add(listOf(notifications))
        }
    }

    private fun launchNextPermissionGroup() {
        permissionQueue.removeFirstOrNull()?.let { group ->
            permissionLauncher.launch(group.toTypedArray())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        enqueueMissingPermissions()
        setContent {
            val viewModel: HomeViewModel = viewModel()
            NoorApp(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // الانطلاق في سلسلة الحوارات بعد أول ظهور فقط (يلزم حالة STARTED)
        if (!permissionFlowStarted && permissionQueue.isNotEmpty()) {
            permissionFlowStarted = true
            launchNextPermissionGroup()
        }
    }
}
