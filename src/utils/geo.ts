/**
 * عكس الإحداثيات إلى اسم مدينة عربي — إصلاح مشكلة «تحديد الموقع يعمل لكن
 * اسم المدينة لا يظهر» (بطلب المالك 2026-10-29).
 *
 * الاستراتيجية (مصدر أساسي + احتياطيان):
 *  1) BigDataCloud reverse-geocode-client — مجاني بلا مفتاح ومصمم للاستخدام
 *     من العميل مباشرة، يدعم العربية (localityLanguage=ar).
 *  2) Nominatim (OpenStreetMap) — احتياط أول.
 *  3) أسماء عربية مدمجة لأشهر المدن بالتقريب الجغرافي — احتياط أخير أوفلاين.
 *
 * أي فشل يرجع «موقعي الحالي» — لا يكسر تحديد الموقع أبداً.
 */

export interface ReverseGeoResult {
  city: string;
  country?: string;
}

/** مدن عربية مدمجة للتقريب الأوفلاين — احتياط أخير */
const KNOWN_CITIES: Array<{ name: string; lat: number; lng: number }> = [
  { name: 'القاهرة', lat: 30.0444, lng: 31.2357 },
  { name: 'الجيزة', lat: 30.0131, lng: 31.2089 },
  { name: 'الإسكندرية', lat: 31.2001, lng: 29.9187 },
  { name: 'مكة المكرمة', lat: 21.3891, lng: 39.8579 },
  { name: 'المدينة المنورة', lat: 24.5247, lng: 39.5692 },
  { name: 'الرياض', lat: 24.7136, lng: 46.6753 },
  { name: 'جدة', lat: 21.4858, lng: 39.1925 },
  { name: 'الدمام', lat: 26.4207, lng: 50.0888 },
  { name: 'دبي', lat: 25.2048, lng: 55.2708 },
  { name: 'أبو ظبي', lat: 24.4539, lng: 54.3773 },
  { name: 'الدوحة', lat: 25.2854, lng: 51.531 },
  { name: 'الكويت', lat: 29.3759, lng: 47.9774 },
  { name: 'المنامة', lat: 26.2285, lng: 50.586 },
  { name: 'مسقط', lat: 23.588, lng: 58.3829 },
  { name: 'عمّان', lat: 31.9539, lng: 35.9106 },
  { name: 'القدس', lat: 31.7683, lng: 35.2137 },
  { name: 'غزة', lat: 31.5017, lng: 34.4668 },
  { name: 'بيروت', lat: 33.8938, lng: 35.5018 },
  { name: 'دمشق', lat: 33.5138, lng: 36.2765 },
  { name: 'بغداد', lat: 33.3152, lng: 44.3661 },
  { name: 'الخرطوم', lat: 15.5007, lng: 32.5599 },
  { name: 'طرابلس', lat: 32.8872, lng: 13.1913 },
  { name: 'تونس', lat: 36.8065, lng: 10.1815 },
  { name: 'الجزائر', lat: 36.7538, lng: 3.0588 },
  { name: 'الدار البيضاء', lat: 33.5731, lng: -7.5898 },
  { name: 'الرباط', lat: 34.0209, lng: -6.8416 },
  { name: 'النيانو / نواكشوط', lat: 18.0735, lng: -15.9582 },
  { name: 'إسطنبول', lat: 41.0082, lng: 28.9784 },
  { name: 'أنقرة', lat: 39.9334, lng: 32.8597 },
  { name: 'لندن', lat: 51.5074, lng: -0.1278 },
  { name: 'باريس', lat: 48.8566, lng: 2.3522 },
  { name: 'برلين', lat: 52.52, lng: 13.405 },
  { name: 'نيويورك', lat: 40.7128, lng: -74.006 },
  { name: 'تورونتو', lat: 43.6532, lng: -79.3832 },
  { name: 'كوالالمبور', lat: 3.139, lng: 101.6869 },
  { name: 'جاكرتا', lat: -6.2088, lng: 106.8456 },
  { name: 'إسلام أباد', lat: 33.6844, lng: 73.0479 },
  { name: 'كراتشي', lat: 24.8607, lng: 67.0011 },
];

function nearestKnownCity(lat: number, lng: number): ReverseGeoResult {
  let best = KNOWN_CITIES[0];
  let bestDist = Number.MAX_VALUE;
  for (const c of KNOWN_CITIES) {
    const dLat = c.lat - lat;
    const dLng = c.lng - lng;
    const dist = dLat * dLat + dLng * dLng;
    if (dist < bestDist) {
      bestDist = dist;
      best = c;
    }
  }
  // لو بعيد جداً عن أقرب مدينة معروفة (أكثر من ~2 درجة ≈ 220 كم) — اسم عام
  if (bestDist > 4) return { city: 'موقعي الحالي' };
  return { city: best.name };
}

function fetchJson(url: string, timeoutMs = 9000): Promise<unknown> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  return fetch(url, {
    signal: controller.signal,
    headers: { Accept: 'application/json' },
  })
    .then((res) => (res.ok ? res.json() : Promise.reject(new Error(String(res.status)))))
    .finally(() => clearTimeout(timer));
}

/** المصدر الأساسي: BigDataCloud (مجاني بلا مفتاح، يدعم العربية) */
async function viaBigDataCloud(lat: number, lng: number): Promise<ReverseGeoResult | null> {
  try {
    const data = (await fetchJson(
      `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lng}&localityLanguage=ar`,
    )) as {
      city?: string;
      locality?: string;
      principalSubdivision?: string;
      countryName?: string;
    };
    const city = data.city || data.locality || data.principalSubdivision || '';
    if (city && city.trim()) {
      return {
        city: city.trim(),
        country: data.countryName?.trim() || undefined,
      };
    }
  } catch {
    // احتياط
  }
  return null;
}

/** الاحتياط الأول: Nominatim من OpenStreetMap */
async function viaNominatim(lat: number, lng: number): Promise<ReverseGeoResult | null> {
  try {
    const data = (await fetchJson(
      `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=jsonv2&accept-language=ar&zoom=10`,
    )) as { address?: Record<string, string> };
    const a = data.address ?? {};
    const city = a.city || a.town || a.village || a.county || a.state || '';
    if (city && city.trim()) {
      return { city: city.trim(), country: a.country?.trim() || undefined };
    }
  } catch {
    // احتياط
  }
  return null;
}

/**
 * عكس الإحداثيات إلى اسم مدينة — يجرّب المصادر بالترتيب ثم الاحتياط الأوفلاين.
 * لا يرمي خطأ أبداً: أي فشل = «موقعي الحالي».
 */
export async function reverseGeocodeCity(lat: number, lng: number): Promise<ReverseGeoResult> {
  const primary = await viaBigDataCloud(lat, lng);
  if (primary) return primary;

  const fallback = await viaNominatim(lat, lng);
  if (fallback) return fallback;

  return nearestKnownCity(lat, lng);
}
