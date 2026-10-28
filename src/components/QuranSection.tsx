import React from 'react';
import { MushafInteractiveReader } from './MushafInteractiveReader';

/**
 * تبويب المصحف — إعادة تصميم بطلب المالك (2026-10):
 * المصحف التفاعلي فقط هو اللي يظهر — لا يوجد أي شيء فوقه.
 * اختيار القارئ وتنزيل التلاوات انتقل إلى قسم الإعدادات.
 */
export const QuranSection: React.FC = () => <MushafInteractiveReader />;
