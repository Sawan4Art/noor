package com.sawan4art.noor;

import android.content.Context;
import android.widget.RemoteViews;

/**
 * النسخة الشفافة من ويدجت المواقيت — نفس البيانات بدون خلفية،
 * تظهر في قائمة الويدجتس كخيار مستقل «نور المسلم شفاف».
 */
public class NoorWidgetTransparentProvider extends NoorWidgetProvider {

    @Override
    protected RemoteViews createViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_noor_transparent);
    }
}
