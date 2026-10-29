import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { readFileSync, writeFileSync } from 'fs';
import {defineConfig} from 'vite';

/**
 * إضافة التحديث الهوائي (OTA):
 * يحقن __BUILD_ID__ و __APP_VERSION__ داخل الحزمة، ويكتب public/version.json
 * بنفس القيم — فيخدمته وضع الـ dev والـ preview، وينسخه vite تلقائياً داخل dist.
 */

export default defineConfig(() => {
  const pkgVersion = JSON.parse(
    readFileSync(path.resolve(__dirname, 'package.json'), 'utf-8'),
  ) as { version?: string };
  const version = pkgVersion.version ?? '0.0.0';
  const buildId = crypto.randomUUID().slice(0, 12);
  const otaPayload = JSON.stringify(
    { version, buildId, builtAt: new Date().toISOString() },
    null,
    2,
  );
  // يُكتب في public ليخدم في وضع dev/preview، وينسخ داخل dist مع كل بناء
  writeFileSync(path.resolve(__dirname, 'public/version.json'), otaPayload);

  return {
    plugins: [react(), tailwindcss()],
    define: {
      __BUILD_ID__: JSON.stringify(buildId),
      __APP_VERSION__: JSON.stringify(version),
    },
    resolve: {
      alias: {
        '@': path.resolve(__dirname, '.'),
      },
    },
    server: {
       // تعطيل HMR أثناء التعديلات الآلية لمنع الوميض.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
