#!/usr/bin/env node
/**
 * تحميل مصحف التجويد الملون كاملاً من alquran.cloud (نسخة quran-tajweed
 * المعتمدة على مصحف مجمع الملك فهد) وضغطه في ملف واحد صغير للتطبيق.
 *
 * المصدر: https://api.alquran.cloud/v1/quran/quran-tajweed
 * الصيغة الناتجة: { "1": ["بِسْمِ [h:1[ٱ]لَّ...", ...], "2": [...] }
 * أي: رقم السورة -> مصفوفة نصوص الآيات بالترميز التجويدي.
 */
const fs = require('fs');
const path = require('path');

const API = 'https://api.alquran.cloud/v1/quran/quran-tajweed';
const OUT = path.join(__dirname, '..', 'public', 'data', 'quran-tajweed.json');

async function main() {
  console.log('⏬ تحميل مصحف التجويد الكامل من alquran.cloud (عبر curl) ...');
  const { execFileSync } = require('child_process');
  const raw = execFileSync(
    'curl',
    ['-sL', '--retry', '3', '--max-time', '300', API],
    { maxBuffer: 1024 * 1024 * 1024 },
  ).toString('utf8');
  const json = JSON.parse(raw);
  if (json.code !== 200 || !json.data?.surahs) throw new Error('bad payload');

  const compact = {};
  let ayahCount = 0;
  for (const surah of json.data.surahs) {
    compact[String(surah.number)] = surah.ayahs.map((a) => a.text);
    ayahCount += surah.ayahs.length;
  }

  fs.mkdirSync(path.dirname(OUT), { recursive: true });
  fs.writeFileSync(OUT, JSON.stringify(compact));
  const sizeMB = (fs.statSync(OUT).size / 1024 / 1024).toFixed(2);
  console.log(`✅ تم: ${Object.keys(compact).length} سورة • ${ayahCount} آية • ${sizeMB} MB`);

  // فحص سريع: البقرة 255 موجودة وغير فارغة وفيها ترميز تجويد
  const sample = compact['2'][254];
  if (!sample || !sample.includes('[')) throw new Error('sample ayah missing tajweed markup!');
  console.log('عيّنة البقرة 255:', sample.slice(0, 80));
}

main().catch((e) => {
  console.error('❌ فشل:', e.message);
  process.exit(1);
});
