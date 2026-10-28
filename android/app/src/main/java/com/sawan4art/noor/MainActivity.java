package com.sawan4art.noor;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // جسر نور المسلم النيتِفي: الأذونات + منبهات الأذان + الويدجت
        registerPlugin(NoorNativePlugin.class);
    }
}
