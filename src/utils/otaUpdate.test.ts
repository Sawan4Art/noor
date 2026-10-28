import test from 'node:test';
import assert from 'node:assert/strict';
import {
  compareVersions,
  isWebUpdateAvailable,
  isApkUpdateAvailable,
  detectApkEnvironment,
  fetchWebManifest,
  fetchLatestRelease,
  OtaManifest,
} from './otaUpdate.ts';

test('compareVersions orders semver correctly', () => {
  assert.equal(compareVersions('1.0.0', '1.0.0'), 0);
  assert.equal(compareVersions('1.1.0', '1.0.9'), 1);
  assert.equal(compareVersions('0.9.9', '1.0.0'), -1);
  assert.equal(compareVersions('0.2.0', '0.1.0'), 1);
  assert.equal(compareVersions('v1.2.3', '1.2.3'), 0, 'v prefix tolerated');
  assert.equal(compareVersions('1.0', '1.0.0'), 0, 'missing segments treated as 0');
});

test('compareVersions returns error marker for malformed input', () => {
  assert.equal(compareVersions('abc', '1.0.0'), -2);
  assert.equal(compareVersions('1.0.0', ''), -2);
});

test('isWebUpdateAvailable detects different buildId only outside dev', () => {
  const manifest: OtaManifest = {
    version: '0.2.0',
    buildId: 'abc123',
    builtAt: '2026-10-29T00:00:00Z',
  };
  assert.equal(isWebUpdateAvailable('abc123', manifest), false, 'same buildId = no update');
  assert.equal(isWebUpdateAvailable('xyz789', manifest), true, 'different buildId = update');
  assert.equal(isWebUpdateAvailable('xyz789', null), false, 'null manifest = no update');
  assert.equal(isWebUpdateAvailable('dev', manifest), false, 'dev mode never nags');
});

test('isApkUpdateAvailable compares versions safely', () => {
  assert.equal(isApkUpdateAvailable('0.1.0', '0.2.0'), true);
  assert.equal(isApkUpdateAvailable('0.1.0', '0.1.0'), false);
  assert.equal(isApkUpdateAvailable('0.2.0', '0.1.0'), false);
  assert.equal(isApkUpdateAvailable('0.1.0', 'garbage'), false, 'malformed = safe no');
  assert.equal(isApkUpdateAvailable('', '0.2.0'), false, 'empty current = safe no');
});

test('detectApkEnvironment returns false outside native Capacitor', () => {
  assert.equal(detectApkEnvironment(), false);
});

test('fetchWebManifest returns null on network failure (fallback)', async () => {
  const result = await fetchWebManifest('http://localhost:9/nope.json', 500);
  assert.equal(result, null);
});

test('fetchLatestRelease returns null on network failure (fallback)', async () => {
  const result = await fetchLatestRelease('http://localhost:9/nope', 500);
  assert.equal(result, null);
});
