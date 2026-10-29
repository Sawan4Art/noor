import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { MoonStar, HeartHandshake } from 'lucide-react';
import { APP_VERSION } from '../utils/otaUpdate';
import { AppTheme, ColorMode } from '../utils/themeData';

/**
 * شاشة السبلاش — صفحة دعاء لأبي المتوفي (يوسف: طلبها تكون أول ما التطبيق يفتح،
 * 2026-10-29: من «المرحوم» إلى «المتوفي» بطلب المالك)
 * وتنتهي بجملة الصدقة الجارية اللي طلبها المالك حرفياً.
 * بطلب المالك: لا تُغلق إلا بزر «متابعة» فقط — لا إغلاق تلقائي ولا إغلاق باللمس.
 */

interface SplashDuaProps {
  onDone: () => void;
  currentTheme: AppTheme;
  colorMode: ColorMode;
}

const DUA_TEXT =
  'اللهم اغفر لأبي وارحمه، وعافه واعفُ عنه، وأكرم نُزُله، ووسّع مُدخله، ' +
  'واغسله بالماء والثلج والبَرَد، ونقِّه من الخطايا كما يُنقّى الثوب الأبيض من الدنس، ' +
  'وأبدله داراً خيراً من داره، وأهلاً خيراً من أهله، واجعل قبره روضة من رياض الجنة، ' +
  'ونوِّره في قبره، ووسع له فيه، واجعل هذا العملُ صدقةً جارية في ميزان حسناته.';

export const SplashDua: React.FC<SplashDuaProps> = ({ onDone, currentTheme }) => {
  const [leaving, setLeaving] = useState<boolean>(false);

  // بطلب المالك: مفيش إغلاق تلقائي — الزر بس هو اللي يقفل
  const finish = (): void => {
    setLeaving(true);
    setTimeout(onDone, 450);
  };

  return (
    <AnimatePresence>
      {!leaving && (
        <motion.div
          key="splash"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0, scale: 1.04 }}
          transition={{ duration: 0.45, ease: 'easeInOut' }}
          className="fixed inset-0 z-[100] h-[100dvh] flex flex-col items-center overflow-hidden"
          style={{
            background: `radial-gradient(1200px 800px at 50% -10%, color-mix(in srgb, ${currentTheme.primaryColor} 26%, #020509), #020509 62%)`,
          }}
          dir="rtl"
        >
          {/* هالات ضوء ناعمة */}
          <div
            className="absolute -top-24 right-1/4 w-72 h-72 rounded-full blur-3xl pointer-events-none"
            style={{ background: currentTheme.glowColor }}
          />
          <div
            className="absolute -bottom-32 left-1/5 w-96 h-96 rounded-full blur-3xl pointer-events-none opacity-60"
            style={{ background: currentTheme.glowColor }}
          />

          {/* أعلى الشاشة: البسملة */}
          <motion.div
            initial={{ y: -18, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.25, duration: 0.7 }}
            className="relative z-10 pt-[max(1.25rem,env(safe-area-inset-top))] text-center px-6 shrink-0"
          >
            <p
              className="font-quran"
              style={{
                color: currentTheme.accentColor,
                fontSize: 'clamp(1rem, 3.5vh, 1.5rem)',
              }}
            >
              بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ
            </p>
          </motion.div>

          {/* الوسط: الدعاء — يتقلص ليتسع في أي شاشة بدون سكرول */}
          <div className="relative z-10 flex-1 min-h-0 flex flex-col items-center justify-center px-5 sm:px-8 py-3 max-w-2xl mx-auto text-center">
            <motion.div
              initial={{ scale: 0.6, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              transition={{ delay: 0.45, type: 'spring', stiffness: 200, damping: 16 }}
              className="rounded-3xl flex items-center justify-center shadow-2xl mb-[max(0.75rem,1.5vh)] shrink-0"
              style={{
                width: 'clamp(3.5rem, 9vh, 5rem)',
                height: 'clamp(3.5rem, 9vh, 5rem)',
                background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                boxShadow: `0 12px 40px ${currentTheme.glowColor}`,
              }}
            >
              <MoonStar className="w-1/2 h-1/2 text-white" />
            </motion.div>

            <motion.h1
              initial={{ y: 16, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.7, duration: 0.6 }}
              className="font-extrabold text-white mb-[max(0.6rem,1.4vh)]"
              style={{ fontSize: 'clamp(1.15rem, 3.2vh, 1.875rem)' }}
            >
              دعاء لأبي المتوفي
            </motion.h1>

            <motion.p
              initial={{ y: 16, opacity: 0 }}
              animate={{ y: 0, opacity: 1 }}
              transition={{ delay: 0.95, duration: 0.6 }}
              className="text-slate-200/90 font-medium overflow-hidden"
              style={{
                fontSize: 'clamp(0.72rem, 1.85vh, 1rem)',
                lineHeight: 'clamp(1.55rem, 3.4vh, 2.15rem)',
              }}
            >
              {DUA_TEXT}
            </motion.p>

            <motion.div
              initial={{ scaleX: 0 }}
              animate={{ scaleX: 1 }}
              transition={{ delay: 1.35, duration: 0.7 }}
              className="mt-[max(0.5rem,1.2vh)] h-px w-40 shrink-0"
              style={{
                background: `linear-gradient(to left, transparent, ${currentTheme.accentColor}, transparent)`,
              }}
            />
          </div>

          {/* نهاية الشاشة: الصدقة الجارية + زر المتابعة — ثابتة أسفل */}
          <motion.div
            initial={{ y: 24, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 1.85, duration: 0.7 }}
            className="relative z-10 pb-[max(1.25rem,env(safe-area-inset-bottom))] px-6 text-center space-y-[max(0.75rem,1.6vh)] shrink-0"
          >
            <p
              className="font-bold"
              style={{
                color: currentTheme.accentColor,
                fontSize: 'clamp(0.9rem, 2.3vh, 1.125rem)',
              }}
            >
              هذا التطبيق صدقة جارية على روح المتوفي بأذن الله أبي
            </p>

            <div className="flex items-center justify-center gap-2 text-[11px] text-slate-500">
              <HeartHandshake className="w-3.5 h-3.5" />
              <span>تطوير: Yousef Sawan</span>
              <span className="opacity-40">•</span>
              <span>نور المسلم v{APP_VERSION}</span>
            </div>

            <motion.button
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 2.2, type: 'spring', stiffness: 260, damping: 18 }}
              onClick={(e) => {
                e.stopPropagation();
                finish();
              }}
              whileTap={{ scale: 0.95 }}
              className="px-10 py-3.5 rounded-2xl text-sm font-extrabold text-white shadow-2xl transition-transform animate-pulse"
              style={{
                background: `linear-gradient(135deg, ${currentTheme.primaryColor}, ${currentTheme.accentColor})`,
                boxShadow: `0 10px 36px ${currentTheme.glowColor}`,
              }}
            >
              متابعة إلى التطبيق
            </motion.button>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
};
