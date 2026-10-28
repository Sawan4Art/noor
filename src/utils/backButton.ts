/**
 * مدير زر الرجوع في أندرويد — بطلب المالك (2026-10):
 * الرجوع من صفحات المصحف والصفحات الفرعية للإعدادات بزر الرجوع في الهاتف.
 *
 * الفكرة: مكدس معالجات. أي شاشة داخلية (مصحف بملء الشاشة / صفحة إعدادات فرعية / مودال)
 * تسجّل معالجها، وزر الرجوع ينفّذ أعلى معالج. لو المكدس فاضي:
 * غير التبويب الرئيسي → ارجع للمواقيت، ولو أصلاً فيها → اخرج من التطبيق.
 */
import { App } from '@capacitor/app';

type BackHandler = () => boolean; // true = تمت المعالجة

const stack: BackHandler[] = [];

/** تسجيل معالج رجوع — يرجع دالة إلغاء التسجيل */
export function registerBackHandler(handler: BackHandler): () => void {
  stack.push(handler);
  return () => {
    const idx = stack.indexOf(handler);
    if (idx >= 0) stack.splice(idx, 1);
  };
}

let initialized = false;

/**
 * تهيئة المستمع مرة واحدة من App.tsx.
 * onDefault: يُستدعى لما المكدس فاضي (منطق التنقل بين التبويبات + الخروج).
 */
export function initAndroidBack(onDefault: () => void): void {
  if (initialized) return;
  initialized = true;

  if (typeof window !== 'undefined') {
    // ويب: دعم زر الرجوع في المتصفح بنفس المنطق
    window.addEventListener('popstate', () => {
      const top = stack[stack.length - 1];
      if (top && top()) {
        history.pushState(null, '', location.href);
        return;
      }
      onDefault();
    });
    // بذرة تاريخية عشان popstate يشتغل
    history.pushState(null, '', location.href);
  }

  try {
    App.addListener('backButton', () => {
      const top = stack[stack.length - 1];
      if (top && top()) return;
      onDefault();
    });
  } catch {
    // ليست بيئة كاباسيتور — الويب فقط
  }
}
