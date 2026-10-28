import React, { useState } from 'react';
import {
  Download,
  FileCode,
  Copy,
  Check,
  FolderTree,
  Terminal,
  ExternalLink,
  Sparkles,
} from 'lucide-react';
import {
  ANDROID_KOTLIN_PROJECT_FILES,
  KotlinFile,
  downloadAndroidProjectZip,
} from '../utils/androidKotlinCode';

export const AndroidKotlinTab: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<KotlinFile>(
    ANDROID_KOTLIN_PROJECT_FILES[0]
  );
  const [copied, setCopied] = useState<boolean>(false);
  const [isExporting, setIsExporting] = useState<boolean>(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(selectedFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleDownloadZip = async () => {
    setIsExporting(true);
    try {
      await downloadAndroidProjectZip();
    } catch (e) {
      console.error('Failed to download zip', e);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="p-6 rounded-3xl bg-gradient-to-br from-amber-950/80 via-slate-900 to-emerald-950/60 border border-amber-600/40 shadow-2xl flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 text-amber-300 text-xs font-semibold border border-amber-500/30 mb-2">
            <span>⚡ كود أندرويد كوتلن أصيل (100% Native Kotlin)</span>
          </div>
          <h2 className="text-xl sm:text-3xl font-extrabold text-white leading-tight">
            مشروع تطبيق أندرويد كوتلن كامل (جاهز لـ Android Studio)
          </h2>
          <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-2xl leading-relaxed">
            تم بناء وتجهيز كود المشروع كاملاً بكوتلن مع Jetpack Compose و Glance AppWidgets للويدجتس، وخدمة Foreground Service للمؤذن، و Media3 لتنزيل وتشغيل سور القرآن أوفلاين.
          </p>
        </div>

        {/* Download ZIP Button */}
        <button
          onClick={handleDownloadZip}
          disabled={isExporting}
          className="flex items-center justify-center gap-2 px-6 py-3.5 rounded-2xl bg-gradient-to-r from-amber-500 via-amber-400 to-yellow-500 hover:from-amber-400 hover:to-yellow-400 text-slate-950 font-bold text-sm shadow-xl shadow-amber-950/60 active:scale-95 transition-all shrink-0"
        >
          {isExporting ? (
            <div className="w-5 h-5 border-2 border-slate-950 border-t-transparent rounded-full animate-spin" />
          ) : (
            <Download className="w-5 h-5" />
          )}
          <span>تحميل مشروع أندرويد كوتلن كامل (ZIP)</span>
        </button>
      </div>

      {/* Code Browser Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left: Files List */}
        <div className="lg:col-span-4 space-y-2">
          <div className="p-4 rounded-3xl bg-slate-900/90 border border-slate-800">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3 flex items-center gap-2">
              <FolderTree className="w-4 h-4 text-amber-400" />
              <span>هيكل ملفات مشروع أندرويد (Kotlin)</span>
            </h3>

            <div className="space-y-1.5">
              {ANDROID_KOTLIN_PROJECT_FILES.map((file) => {
                const isSelected = selectedFile.path === file.path;
                return (
                  <button
                    key={file.path}
                    onClick={() => setSelectedFile(file)}
                    className={`w-full flex flex-col items-start p-3 rounded-2xl border text-right transition-all ${
                      isSelected
                        ? 'bg-amber-950/80 border-amber-500 ring-1 ring-amber-400/40 text-white shadow-md'
                        : 'bg-slate-950/60 border-slate-800/80 text-slate-300 hover:border-slate-700'
                    }`}
                  >
                    <div className="flex items-center gap-2 w-full">
                      <FileCode
                        className={`w-4 h-4 shrink-0 ${
                          isSelected ? 'text-amber-400' : 'text-slate-500'
                        }`}
                      />
                      <span className="text-xs font-bold font-mono truncate">
                        {file.filename}
                      </span>
                    </div>
                    <span className="text-[10px] text-slate-400 mt-1 line-clamp-1">
                      {file.description}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Quick instructions box */}
          <div className="p-4 rounded-3xl bg-slate-900/60 border border-slate-800 text-xs text-slate-300 space-y-2">
            <h4 className="font-bold text-amber-300 flex items-center gap-1.5">
              <span>🚀</span>
              <span>طريقة تشغيل المشروع في Android Studio:</span>
            </h4>
            <ol className="list-decimal list-inside space-y-1 text-slate-400 text-[11px] leading-relaxed">
              <li>حمل حزمة الـ ZIP عبر الزر أعلاه وفك ضغطها.</li>
              <li>افتح تطبيق <span className="text-slate-200">Android Studio Ladybug أو أحدث</span>.</li>
              <li>اختر <span className="text-slate-200">Open Project</span> وحدد المجلد.</li>
              <li>اضغط Run لبدء التشغيل على المحاكي أو هاتفك عبر كابل USB.</li>
            </ol>
          </div>
        </div>

        {/* Right: Code Viewer */}
        <div className="lg:col-span-8">
          <div className="rounded-3xl bg-slate-900/90 border border-slate-800 overflow-hidden shadow-2xl">
            {/* Code Header Bar */}
            <div className="p-4 border-b border-slate-800 flex items-center justify-between bg-slate-950">
              <div className="flex items-center gap-2">
                <FileCode className="w-4 h-4 text-amber-400" />
                <span className="font-mono text-xs font-bold text-white">
                  {selectedFile.path}
                </span>
              </div>

              <button
                onClick={handleCopy}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition-colors"
              >
                {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                <span>{copied ? 'تم النسخ!' : 'نسخ الكود'}</span>
              </button>
            </div>

            {/* Code Text with line numbers */}
            <div className="p-4 bg-slate-950 font-mono text-xs text-slate-200 max-h-[560px] overflow-y-auto leading-relaxed dir-ltr text-left">
              <pre className="overflow-x-auto">
                <code>{selectedFile.content}</code>
              </pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
