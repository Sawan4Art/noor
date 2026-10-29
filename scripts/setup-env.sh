#!/usr/bin/env bash
# إعداد بيئة بناء أندرويد داخل الساندبوكس — يُشغَّل في الخلفية ويكتب تقدمه في status
set -x
cd /home/z/my-project
echo "STAGE:extract_jdk" > .setup-tmp/status

# 1) JDK 21
rm -rf jdk21
mkdir -p jdk21
tar xzf .setup-tmp/jdk21.tar.gz -C jdk21 --strip-components=1
export JAVA_HOME="$(pwd)/jdk21"
export PATH="$JAVA_HOME/bin:$PATH"
"$JAVA_HOME/bin/java" -version 2>&1 | head -1 || { echo "FAIL:jdk" > .setup-tmp/status; exit 1; }

echo "STAGE:extract_sdk" > .setup-tmp/status
# 2) Android SDK
mkdir -p android-sdk/cmdline-tools
rm -rf android-sdk/cmdline-tools/latest
unzip -q cmdtools.zip -d android-sdk/cmdline-tools
mv android-sdk/cmdline-tools/cmdline-tools android-sdk/cmdline-tools/latest
export ANDROID_HOME="$(pwd)/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

echo "STAGE:yes_licenses" > .setup-tmp/status
yes | sdkmanager --licenses > /dev/null 2>&1 || true

echo "STAGE:install_platform" > .setup-tmp/status
sdkmanager --install "platforms;android-36" "build-tools;36.0.0" "platform-tools" > /dev/null 2>&1 || { echo "FAIL:sdkmanager" > .setup-tmp/status; exit 1; }

# 3) local.properties
cat > android/local.properties <<EOF
sdk.dir=$(pwd)/android-sdk
EOF

# 4) مرايا Gradle (Aliyun) ضد 429 من Maven Central
mkdir -p ~/.gradle/init.d
cat > ~/.gradle/init.d/mirrors.gradle <<'EOF'
// مرايا عالمية ضد حجب/429 Maven Central (بيئة الساندبوكس)
def ALIYUN_GOOGLE = 'https://maven.aliyun.com/repository/google'
def ALIYUN_CENTRAL = 'https://maven.aliyun.com/repository/central'
def ALIYUN_PLUGIN = 'https://maven.aliyun.com/repository/gradle-plugin'
allprojects {
    repositories {
        maven { url ALIYUN_GOOGLE }
        maven { url ALIYUN_CENTRAL }
        maven { url 'https://jitpack.io' }
        mavenCentral()
        google()
    }
    buildscript {
        repositories {
            maven { url ALIYUN_GOOGLE }
            maven { url ALIYUN_CENTRAL }
            maven { url ALIYUN_PLUGIN }
            mavenCentral()
            google()
        }
    }
}
settingsEvaluated { settings ->
    settings.pluginManagement {
        repositories {
            maven { url ALIYUN_PLUGIN }
            maven { url ALIYUN_GOOGLE }
            maven { url ALIYUN_CENTRAL }
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
EOF

echo "STAGE:keystore" > .setup-tmp/status
# 5) Keystore: يسترجع من الريبو الخاص (noor-secrets) بالتوكن من البيئة
mkdir -p keystore
GH_TOKEN="${GH_TOKEN:-$GITHUB_TOKEN}"
if [ ! -f keystore/noor-release.keystore ] && [ -n "$GH_TOKEN" ]; then
  curl -sL -o keystore/noor-release.keystore \
    -H "Authorization: token $GH_TOKEN" \
    "https://raw.githubusercontent.com/Sawan4Art/noor-secrets/main/noor-release.keystore"
  echo "keystore استُرجع من النسخة الاحتياطية"
elif [ -f keystore/noor-release.keystore ]; then
  echo "keystore موجود"
fi

# 6) ملف .env للتوقيع — القيم السرية من متغيرات البيئة فقط
#    (القيم الفعلية في الريبو الخاص noor-secrets — لا تُكتب هنا أبداً)
if [ ! -f .env ] && [ -f keystore/noor-release.keystore ]; then
  if [ -n "$NOOR_KEYSTORE_PASSWORD" ]; then
    cat > .env <<E
DATABASE_URL=file:$(pwd)/db/custom.db
CAP_KEYSTORE_PATH=$(pwd)/keystore/noor-release.keystore
CAP_KEYSTORE_PASSWORD=$NOOR_KEYSTORE_PASSWORD
CAP_KEY_ALIAS=${NOOR_KEY_ALIAS:-noor}
CAP_KEY_PASSWORD=${NOOR_KEY_PASSWORD:-$NOOR_KEYSTORE_PASSWORD}
E
    echo ".env كُتب من متغيرات البيئة"
  else
    echo "⚠️ عيّن NOOR_KEYSTORE_PASSWORD ليكتب .env — القيم في noor-secrets/README"
    touch .env
  fi
fi

echo "DONE_ALL" > .setup-tmp/status
