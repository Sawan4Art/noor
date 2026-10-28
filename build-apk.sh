#!/usr/bin/env bash
# بناء نسخة أندرويد موقّعة جاهزة للتثبيت — نور المسلم
# الاستخدام: ./build-apk.sh
# التوقيع أوتوماتيك: بيتقری من .env المحلي (متجاهَل في جيت) — بدون إدخال باسورد يدوي.
set -euo pipefail
cd "$(dirname "$0")"

# JDK المحلي لو موجود (بيئة الساندبوكس) — على جهاز المالك يُستخدم JAVA_HOME المثبّت
if [ -z "${JAVA_HOME:-}" ] && [ -d jdk21 ]; then
  export JAVA_HOME="$(pwd)/jdk21"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Android SDK المحلي لو موجود (بيئة الساندبوكس) — على جهاز المالك يُقرأ من local.properties
if [ -z "${ANDROID_HOME:-}" ] && [ -d android-sdk ]; then
  export ANDROID_HOME="$(pwd)/android-sdk"
  export PATH="$ANDROID_HOME/platform-tools:$PATH"
fi

echo "1/4) بناء نسخة الويب (يكتب version.json تلقائياً)…"
npm run build

echo "2/4) منع الـ APK من حزم نفسه — ملفات التنزيل تُستبعد من حزمة الأندرويد…"
rm -rf dist/downloads

echo "3/4) مزامنة كاباسيتور…"
npx cap sync android

echo "4/4) بناء وتوقيع الـ APK (release)…"
( cd android && ./gradlew assembleRelease --console=plain )

APK="android/app/build/outputs/apk/release/app-release.apk"
VERSION=$(node -p "require('./package.json').version")
mkdir -p download
cp "$APK" "download/noor-almuslim-v${VERSION}.apk"

echo "✅ APK موقّع وجاهز: download/noor-almuslim-v${VERSION}.apk"
