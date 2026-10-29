package com.sawan4art.noor;

import android.content.Context;
import android.widget.RemoteViews;

/**
 * النسخة الشفافة من ويدجت المواقيت — نفس البيانات والتخصيص الكامل بدون خلفية،
 * تظهر في قائمة الويدجتس كخيار مستقل «نور المسلم شفاف».
 * لون التمييز وحجم الخط وإظهار/إخفاء العناصر تُطبَّق كالنسخة العادية.
 */
public class NoorWidgetTransparentProvider extends NoorWidgetProvider {

    @Override
    protected RemoteViews createViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_noor_transparent);
    }

    @Override
    protected boolean useOpaqueBackground() {
        // الشفافة بلا خلفية دائماً — حتى لو اختار المالك شفافية للنسخة الداكنة
        return false;
    }
}
