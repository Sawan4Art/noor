#!/usr/bin/env python3
"""رفع أسرار التوقيع إلى GitHub Actions Secrets (مشفرة بـ libsodium sealed box).

الأمان: التوكن وكلمات السر تُقرأ من متغيرات البيئة فقط — لا تكتب في أي ملف:
  export GITHUB_TOKEN=ghp_...
  export NOOR_KEYSTORE_PASSWORD=...
  export NOOR_KEY_ALIAS=noor
  export NOOR_KEY_PASSWORD=...
"""
import base64
import json
import os
import sys
import urllib.request

from nacl import encoding, public

REPO = 'Sawan4Art/noor'


def api(path, method='GET', body=None, token=None):
    url = f'https://api.github.com/{path}'
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, method=method, data=data, headers={
        'Authorization': f'token {token}',
        'Accept': 'application/vnd.github+json',
        'Content-Type': 'application/json',
    })
    with urllib.request.urlopen(req) as r:
        raw = r.read()
        return r.status, json.loads(raw) if raw else {}


def main():
    token = os.environ.get('GITHUB_TOKEN', '')
    keystore_path = os.environ.get('NOOR_KEYSTORE_PATH', 'keystore/noor-release.keystore')
    keystore_password = os.environ.get('NOOR_KEYSTORE_PASSWORD', '')
    key_alias = os.environ.get('NOOR_KEY_ALIAS', 'noor')
    key_password = os.environ.get('NOOR_KEY_PASSWORD', '')

    if not token or not keystore_password:
        print('❌ عيّن GITHUB_TOKEN و NOOR_KEYSTORE_PASSWORD (ومن الأفضل NOOR_KEY_ALIAS و NOOR_KEY_PASSWORD)')
        print('   كل القيم موجودة في الريبو الخاص: Sawan4Art/noor-secrets')
        sys.exit(1)
    if not os.path.exists(keystore_path):
        print(f'❌ ملف المفتاح غير موجود: {keystore_path}')
        sys.exit(1)

    # 1) مفتاح الريفو العمومي
    status, key = api(f'repos/{REPO}/actions/secrets/public-key', token=token)
    if status != 200:
        print('FAIL public-key:', status, key)
        sys.exit(1)
    print('public key:', key['key_id'])

    pk = public.PublicKey(key['key'].encode(), encoding.Base64Encoder())
    sealed = public.SealedBox(pk)

    # 2) قيم الأسرار
    keystore_b64 = base64.b64encode(open(keystore_path, 'rb').read()).decode()
    secrets = {
        'KEYSTORE_BASE64': keystore_b64,
        'KEYSTORE_PASSWORD': keystore_password,
        'KEY_ALIAS': key_alias,
        'KEY_PASSWORD': key_password,
    }

    # 3) رفع كل سر مشفراً
    for name, value in secrets.items():
        enc = base64.b64encode(sealed.encrypt(value.encode())).decode()
        status, resp = api(
            f'repos/{REPO}/actions/secrets/{name}',
            method='PUT',
            body={'encrypted_value': enc, 'key_id': key['key_id']},
            token=token,
        )
        print(f'{name}: HTTP {status}')
        if status not in (201, 204):
            print('FAIL:', resp)
            sys.exit(1)

    print('✅ كل الأسرار مرفوعة')


if __name__ == '__main__':
    main()
