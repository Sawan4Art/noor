#!/usr/bin/env bash
# نشر أصول تنزيل الموقع بعد كل إصدار — خطوتان:
# ١) أجزاءُ الـ APK كوميتَ جذرٍ واحدٍ على فرع site-assets يُستبدل بالكامل
#    مع كل إصدارٍ (بلا تاريخٍ يتضخّم) فتُخدَم من raw.githubusercontent.com —
#    CDN مجهولٌ لا بوابةَ تحقُّقٍ فيه تحجز github.com للحسابات المسجَّلة دخولاً
#    (درس لقطة الشاشة «الزر بيبقى واقف عند صفحة التحقق بخطوتين»)
# ٢) docs/version.json على main (بصمة SHA-256 وحجمٌ وروابط الأجزاء) —
#    الصفحة تقرؤها وتجمّع الأجزاء وتتحقق قبل إعطاء الملف للمتصفح
# الاستعمال: deploy_site_assets.sh <version> <versionCode> <apk-path> [parts-only]
set -euo pipefail

VER="${1:?الإصدار مطلوب}"
CODE="${2:?versionCode مطلوب}"
APK="${3:?مسار الـ APK مطلوب}"
MODE="${4:-full}"
BRANCH="site-assets"

OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
python3 "$(dirname "$0")/build_site_assets.py" "$VER" "$CODE" "$APK" "$OUT"

# ١) أجزاء التحميل — كوميتٌ جذرٌ واحدٌ على الفرع المستقل
GIT_DIR_ABS="$(git rev-parse --absolute-git-dir)"
cd "$OUT"
export GIT_DIR="$GIT_DIR_ABS" GIT_WORK_TREE="$OUT" GIT_INDEX_FILE="$OUT.index"
git read-tree --empty
git update-index --add parts/*
TREE="$(git write-tree)"
COMMIT="$(git commit-tree "$TREE" -m "أصول تنزيل الموقع — أجزاء APK v${VER} للتحميل المباشر بلا بوابة جت هب")"
unset GIT_DIR GIT_WORK_TREE GIT_INDEX_FILE
git push origin "${COMMIT}:refs/heads/${BRANCH}" --force
echo "نُشرت الأجزاء على ${BRANCH} (${COMMIT})"

[ "$MODE" = "parts-only" ] && exit 0

# ٢) نسخة الصفحة على main — مع إعادة محاولةٍ لو تقدّم main أثناء البناء
for i in 1 2 3; do
  git fetch origin main
  git checkout -B main origin/main
  cp "$OUT/version.json" docs/version.json
  git add docs/version.json
  if git diff --cached --quiet; then
    echo "نسخة الموقع محدَّثةٌ أصلاً — لا تغيير"
    exit 0
  fi
  git -c user.name="noor-bot" -c user.email="actions@users.noreply.github.com" \
    commit -m "نسخة الموقع v${VER} — الأجزاء وversion.json تتبع آخر إصدار [skip ci]"
  if git push origin main; then
    echo "نُشرت نسخة الموقع $VER ($CODE)"
    exit 0
  fi
  echo "الدفعة تعارضت — إعادة المحاولة $i بعد ١٠ ثوانٍ"
  sleep 10
done
echo "تعذّر نشر نسخة الصفحة على main — الأجزاء نُشرت والتحميل يعمل منها"
