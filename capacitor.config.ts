import type { CapacitorConfig } from '@capacitor/cli';

/**
 * إعدادات كاباسيتور — نور المسلم
 * الـ APK بيغلف نسخة الويب المبنية في dist/ ويشتغل أوفلاين بالكامل.
 */
const config: CapacitorConfig = {
  appId: 'com.sawan4art.noor',
  appName: 'نور المسلم',
  webDir: 'dist',
  android: {
    // السماح بتحميل التلاوات والملفات الصوتية من مواقع القراء
    allowMixedContent: false,
    backgroundColor: '#0f172a',
  },
  server: {
    // مفيش live reload في الإنتاج — النسخة محلية داخل الـ APK
    androidScheme: 'https',
  },
};

export default config;
