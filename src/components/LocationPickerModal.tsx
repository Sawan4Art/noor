import React, { useState } from 'react';
import {
  MapPin,
  Crosshair,
  Globe2,
  Check,
  X,
  Compass,
  AlertCircle,
} from 'lucide-react';
import { COUNTRIES_DATA, CountryInfo, CityLocation, getDeviceCoordinates } from '../utils/countriesData';
import { Coordinates } from '../utils/prayerMath';
import { reverseGeocodeCity } from '../utils/geo';

interface LocationPickerModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentCoords: Coordinates;
  onSelectCoords: (coords: Coordinates) => void;
}

export const LocationPickerModal: React.FC<LocationPickerModalProps> = ({
  isOpen,
  onClose,
  currentCoords,
  onSelectCoords,
}) => {
  const [selectedCountryCode, setSelectedCountryCode] = useState<string>('EG');
  const [isLocatingGPS, setIsLocatingGPS] = useState<boolean>(false);
  const [gpsError, setGpsError] = useState<string | null>(null);

  if (!isOpen) return null;

  const currentCountry = COUNTRIES_DATA.find((c) => c.code === selectedCountryCode) || COUNTRIES_DATA[0];

  /**
   * تحديد الموقع عبر GPS + جلب اسم المدينة الحقيقي (بطلب المالك:
   * «تحديد الموقع يعمل لكن اسم المدينة لا يظهر») — أي فشل في الاسم
   * لا يكسر التحديد: يبقى «موقعي الحالي» مع الإحداثيات الدقيقة.
   */
  const handleUseGPS = async () => {
    setIsLocatingGPS(true);
    setGpsError(null);
    try {
      const pos = await getDeviceCoordinates();
      let city = 'موقعي الحالي';
      try {
        const geo = await reverseGeocodeCity(pos.latitude, pos.longitude);
        city =
          geo.country && !geo.city.includes(geo.country)
            ? `${geo.city}، ${geo.country}`
            : geo.city;
      } catch {
        // الاسم احتياطي فقط — الإحداثيات الدقيقة كافية للمواقيت
      }
      onSelectCoords({
        city,
        latitude: pos.latitude,
        longitude: pos.longitude,
      });
      setIsLocatingGPS(false);
      onClose();
    } catch {
      setIsLocatingGPS(false);
      setGpsError('تعذر الحصول على الموقع الجغرافي تلقائياً. يرجى تفعيل إذن الموقع أو اختيار البلد والمدينة يدوياً.');
    }
  };

  const handleSelectCity = (city: CityLocation) => {
    onSelectCoords({
      city: `${city.name}، ${currentCountry.name.split(' ')[0]}`,
      latitude: city.latitude,
      longitude: city.longitude,
    });
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-3xl bg-slate-900 border border-emerald-600/40 p-6 shadow-2xl space-y-5 text-right">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-800">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center">
              <Globe2 className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">تحديد موقع مواقيت الصلاة</h3>
              <p className="text-xs text-slate-400">عبر الـ GPS أو باختيار البلد ثم المدينة</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-slate-800 text-slate-400 hover:text-white"
            aria-label="إغلاق نافذة الموقع"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* GPS Quick Button */}
        <div>
          <button
            onClick={handleUseGPS}
            disabled={isLocatingGPS}
            className="w-full p-3.5 rounded-2xl bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 text-white font-bold text-xs sm:text-sm flex items-center justify-center gap-2 shadow-lg shadow-emerald-950/60 active:scale-95 transition-all"
          >
            {isLocatingGPS ? (
              <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Crosshair className="w-4 h-4 text-emerald-200" />
            )}
            <span>تحديد موقعي الحالي تلقائياً عبر الـ GPS</span>
          </button>

          {gpsError && (
            <div className="mt-2 p-2.5 rounded-xl bg-rose-950/60 border border-rose-800/80 text-[11px] text-rose-300 flex items-start gap-1.5">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <span>{gpsError}</span>
            </div>
          )}
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-500">
          <div className="flex-1 h-px bg-slate-800" />
          <span>أو اختر الدولة والمدينة يدوياً</span>
          <div className="flex-1 h-px bg-slate-800" />
        </div>

        {/* 1. Country Selector */}
        <div>
          <label className="block text-xs font-bold text-slate-300 mb-1.5">
            ١. اختر الدولة:
          </label>
          <select
            value={selectedCountryCode}
            onChange={(e) => setSelectedCountryCode(e.target.value)}
            className="w-full bg-slate-950 border border-slate-800 text-xs text-slate-200 rounded-xl p-3 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          >
            {COUNTRIES_DATA.map((country) => (
              <option key={country.code} value={country.code} className="bg-slate-900 text-slate-100">
                {country.name}
              </option>
            ))}
          </select>
        </div>

        {/* 2. City Selector */}
        <div>
          <label className="block text-xs font-bold text-slate-300 mb-1.5">
            ٢. اختر المدينة في {currentCountry.name.split(' ')[0]}:
          </label>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-48 overflow-y-auto p-1 pr-2">
            {currentCountry.cities.map((city) => {
              const isSelected = currentCoords.city.includes(city.name);
              return (
                <button
                  key={city.name}
                  onClick={() => handleSelectCity(city)}
                  className={`p-2.5 rounded-xl border text-xs text-right transition-all flex items-center justify-between ${
                    isSelected
                      ? 'bg-emerald-950 border-emerald-500 text-white font-bold'
                      : 'bg-slate-950/60 border-slate-800 text-slate-300 hover:border-slate-700 hover:bg-slate-950'
                  }`}
                >
                  <span className="truncate">{city.name}</span>
                  {isSelected && <Check className="w-3.5 h-3.5 text-emerald-400 shrink-0 mr-1" />}
                </button>
              );
            })}
          </div>
        </div>

        {/* Current Coordinates Indicator */}
        <div className="p-3 rounded-xl bg-slate-950 text-[11px] text-slate-400 flex items-center justify-between font-mono">
          <div className="flex items-center gap-1.5">
            <MapPin className="w-3.5 h-3.5 text-emerald-400" />
            <span>الموقع الحالي: {currentCoords.city}</span>
          </div>
          <span>({currentCoords.latitude.toFixed(2)}°, {currentCoords.longitude.toFixed(2)}°)</span>
        </div>
      </div>
    </div>
  );
};
