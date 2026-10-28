/**
 * Countries and Cities Directory for Prayer Times
 * Supports Country selection -> Dynamic Cities selection, as well as GPS Geolocation
 */

export interface CityLocation {
  name: string;
  latitude: number;
  longitude: number;
}

export interface CountryInfo {
  code: string;
  name: string;
  cities: CityLocation[];
}

export const COUNTRIES_DATA: CountryInfo[] = [
  {
    code: 'EG',
    name: 'مصر (Egypt)',
    cities: [
      { name: 'القاهرة', latitude: 30.0444, longitude: 31.2357 },
      { name: 'الإسكندرية', latitude: 31.2001, longitude: 29.9187 },
      { name: 'الجيزة', latitude: 30.0131, longitude: 31.2089 },
      { name: 'المنصورة', latitude: 31.0409, longitude: 31.3785 },
      { name: 'طنطا', latitude: 30.7865, longitude: 31.0004 },
      { name: 'أسيوط', latitude: 27.1809, longitude: 31.1837 },
      { name: 'أسوان', latitude: 24.0889, longitude: 32.8998 },
      { name: 'بورسعيد', latitude: 31.2653, longitude: 32.3019 },
      { name: 'الإسماعيلية', latitude: 30.5965, longitude: 32.2715 },
      { name: 'السويس', latitude: 29.9668, longitude: 32.5498 },
      { name: 'شرم الشيخ', latitude: 27.9158, longitude: 34.3299 },
      { name: 'الغردقة', latitude: 27.2579, longitude: 33.8116 },
    ],
  },
  {
    code: 'SA',
    name: 'المملكة العربية السعودية (Saudi Arabia)',
    cities: [
      { name: 'مكة المكرمة', latitude: 21.4225, longitude: 39.8262 },
      { name: 'المدينة المنورة', latitude: 24.5247, longitude: 39.5692 },
      { name: 'الرياض', latitude: 24.7136, longitude: 46.6753 },
      { name: 'جدة', latitude: 21.5433, longitude: 39.1728 },
      { name: 'الدمام', latitude: 26.4207, longitude: 50.0888 },
      { name: 'الخبر', latitude: 26.2172, longitude: 50.1971 },
      { name: 'تبوك', latitude: 28.3835, longitude: 36.5662 },
      { name: 'أبها', latitude: 18.2164, longitude: 42.5053 },
      { name: 'بريدة', latitude: 26.326, longitude: 43.975 },
      { name: 'الطائف', latitude: 21.2854, longitude: 40.4222 },
    ],
  },
  {
    code: 'PS',
    name: 'فلسطين (Palestine)',
    cities: [
      { name: 'القدس الشريف', latitude: 31.7683, longitude: 35.2137 },
      { name: 'غزة', latitude: 31.5017, longitude: 34.4668 },
      { name: 'رام الله', latitude: 31.9038, longitude: 35.2034 },
      { name: 'الخليل', latitude: 31.5326, longitude: 35.0998 },
      { name: 'نابلس', latitude: 32.2211, longitude: 35.2544 },
      { name: 'جنين', latitude: 32.4608, longitude: 35.2974 },
      { name: 'خان يونس', latitude: 31.3462, longitude: 34.3063 },
    ],
  },
  {
    code: 'AE',
    name: 'الإمارات العربية المتحدة (UAE)',
    cities: [
      { name: 'دبي', latitude: 25.2048, longitude: 55.2708 },
      { name: 'أبوظبي', latitude: 24.4539, longitude: 54.3773 },
      { name: 'الشارقة', latitude: 25.3463, longitude: 55.4209 },
      { name: 'عجمان', latitude: 25.4052, longitude: 55.5136 },
      { name: 'العين', latitude: 24.2075, longitude: 55.7447 },
      { name: 'رأس الخيمة', latitude: 25.7895, longitude: 55.9432 },
    ],
  },
  {
    code: 'KW',
    name: 'الكويت (Kuwait)',
    cities: [
      { name: 'مدينة الكويت', latitude: 29.3759, longitude: 47.9774 },
      { name: 'حولي', latitude: 29.3328, longitude: 48.0289 },
      { name: 'السالمية', latitude: 29.3344, longitude: 48.0772 },
      { name: 'الأحمدي', latitude: 29.0769, longitude: 48.0839 },
      { name: 'الجهراء', latitude: 29.3375, longitude: 47.6581 },
    ],
  },
  {
    code: 'MA',
    name: 'المغرب (Morocco)',
    cities: [
      { name: 'الرباط', latitude: 34.0209, longitude: -6.8416 },
      { name: 'الدار البيضاء', latitude: 33.5731, longitude: -7.5898 },
      { name: 'مراكش', latitude: 31.6295, longitude: -7.9811 },
      { name: 'فاس', latitude: 34.0181, longitude: -5.0078 },
      { name: 'طنجة', latitude: 35.7595, longitude: -5.834 },
      { name: 'أكادير', latitude: 30.4278, longitude: -9.5981 },
    ],
  },
  {
    code: 'DZ',
    name: 'الجزائر (Algeria)',
    cities: [
      { name: 'الجزائر العاصمة', latitude: 36.7538, longitude: 3.0588 },
      { name: 'وهران', latitude: 35.6987, longitude: -0.6349 },
      { name: 'قسنطينة', latitude: 36.365, longitude: 6.6147 },
      { name: 'عنابة', latitude: 36.9, longitude: 7.7667 },
    ],
  },
  {
    code: 'JO',
    name: 'الأردن (Jordan)',
    cities: [
      { name: 'عمان', latitude: 31.9454, longitude: 35.9284 },
      { name: 'الزرقاء', latitude: 32.0728, longitude: 36.088 },
      { name: 'إربد', latitude: 32.5568, longitude: 35.8469 },
      { name: 'العقبة', latitude: 29.5321, longitude: 35.0063 },
    ],
  },
  {
    code: 'TR',
    name: 'تركيا (Turkey)',
    cities: [
      { name: 'إسطنبول', latitude: 41.0082, longitude: 28.9784 },
      { name: 'أنقرة', latitude: 39.9334, longitude: 32.8597 },
      { name: 'إزمير', latitude: 38.4237, longitude: 27.1428 },
      { name: 'بورصة', latitude: 40.1885, longitude: 29.061 },
    ],
  },
  {
    code: 'QA',
    name: 'قطر (Qatar)',
    cities: [
      { name: 'الدوحة', latitude: 25.2854, longitude: 51.531 },
      { name: 'الوكرة', latitude: 25.1768, longitude: 51.6048 },
      { name: 'الريان', latitude: 25.2919, longitude: 51.4244 },
    ],
  },
  {
    code: 'GB',
    name: 'المملكة المتحدة (United Kingdom)',
    cities: [
      { name: 'لندن (London)', latitude: 51.5074, longitude: -0.1278 },
      { name: 'برمنغهام (Birmingham)', latitude: 52.4862, longitude: -1.8904 },
      { name: 'مانشستر (Manchester)', latitude: 53.4808, longitude: -2.2426 },
    ],
  },
  {
    code: 'US',
    name: 'الولايات المتحدة (USA)',
    cities: [
      { name: 'نيويورك (New York)', latitude: 40.7128, longitude: -74.006 },
      { name: 'شيكاغو (Chicago)', latitude: 41.8781, longitude: -87.6298 },
      { name: 'لوس أنجلوس (Los Angeles)', latitude: 34.0522, longitude: -118.2437 },
      { name: 'دالاس (Dallas)', latitude: 32.7767, longitude: -96.797 },
    ],
  },
];

/**
 * Request user's device geolocation via HTML5 Geolocation API
 */
export function getDeviceCoordinates(): Promise<{ latitude: number; longitude: number; name: string }> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('Geolocation not supported'));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        resolve({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          name: 'موقعي الحالي (GPS)',
        });
      },
      (error) => {
        reject(error);
      },
      { timeout: 10000, enableHighAccuracy: true }
    );
  });
}
