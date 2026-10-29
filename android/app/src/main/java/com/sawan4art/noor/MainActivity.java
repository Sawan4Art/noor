package com.sawan4art.noor;

import android.os.Bundle;
import androidx.core.splashscreen.SplashScreen;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // سبلاش موحد من أول إطار: الهلال على الخلفية الخضراء مباشرة
        // ثم يبدّل لثيم التطبيق — إصلاح «شاشة بيضاء بشعار أزرق قبل السبلاش»
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        // جسر نور المسلم النيتِفي: الأذونات + منبهات الأذان + الويدجت
        registerPlugin(NoorNativePlugin.class);
    }
}
