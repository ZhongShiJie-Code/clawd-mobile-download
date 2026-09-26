import { createHash } from 'node:crypto';
import { copyFileSync, existsSync, mkdirSync, readFileSync, readdirSync, writeFileSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const git = (...args) => execFileSync('git', ['-C', root, ...args], { encoding: 'utf8' }).trim();
if (git('status', '--porcelain')) throw new Error('Source tree is dirty. Commit the exact source before packaging.');
const revision = git('rev-parse', 'HEAD');
const gradle = readFileSync(join(root, 'android/app/build.gradle.kts'), 'utf8');
const versionName = gradle.match(/versionName\s*=\s*"([^"]+)"/)?.[1];
const versionCode = Number(gradle.match(/versionCode\s*=\s*(\d+)/)?.[1]);
if (!versionName || !versionCode) throw new Error('Cannot read application version.');

const sdk = process.env.ANDROID_HOME || join(process.env.HOME, 'Library/Android/sdk');
const toolsRoot = join(sdk, 'build-tools');
const toolVersion = readdirSync(toolsRoot).filter(v => /^\d+\.\d+\.\d+$/.test(v))
  .sort((a, b) => a.localeCompare(b, undefined, { numeric: true })).at(-1);
if (!toolVersion) throw new Error('Android build tools are unavailable.');
const tools = join(toolsRoot, toolVersion);
const apk = join(root, 'android/app/build/outputs/apk/debug/app-debug.apk');
if (!existsSync(apk)) throw new Error('Build the APK first.');
const env = { ...process.env };
if (!env.JAVA_HOME && existsSync('/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home')) {
  env.JAVA_HOME = '/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home';
}
const run = (name, args) => execFileSync(join(tools, name), args, { encoding: 'utf8', env });
const badging = run('aapt', ['dump', 'badging', apk]);
if (!badging.includes(`name='com.clawd.mobile' versionCode='${versionCode}' versionName='${versionName}'`)) {
  throw new Error('APK package/version does not match this source.');
}
const manifest = run('aapt', ['dump', 'xmltree', apk, 'AndroidManifest.xml']);
const embedded = manifest.match(/"clawd\.source_revision"[\s\S]*?android:value[^\n]*?"([0-9a-f]{40})"/)?.[1];
if (embedded !== revision) throw new Error('APK source commit does not match HEAD. Rebuild from this commit.');
const certificate = run('apksigner', ['verify', '--print-certs', apk]);
const signingSha256 = certificate.match(/certificate SHA-256 digest: ([0-9a-f]+)/)?.[1];
const expectedCertificate = readFileSync(join(root, 'ci/expected-signing-sha256.txt'), 'utf8').trim();
if (signingSha256 !== expectedCertificate) throw new Error('APK signing certificate is not compatible with v0.11.6.');

const output = join(root, 'release-artifacts');
mkdirSync(output, { recursive: true });
const apkName = `Clawd-Mobile-${versionName}-debug.apk`;
copyFileSync(apk, join(output, apkName));
const sha256 = createHash('sha256').update(readFileSync(apk)).digest('hex');
const release = {
  packageName: 'com.clawd.mobile', versionName, versionCode,
  sourceRepository: 'https://github.com/ZhongShiJie-Code/clawd-mobile-download',
  sourceCommit: revision, apkName, sha256, signingCertificateSha256: signingSha256,
  buildType: 'debug-compatible', deviceAcceptance: 'not-yet-verified',
};
writeFileSync(join(output, 'release-manifest.json'), JSON.stringify(release, null, 2) + '\n');
writeFileSync(join(output, 'SHA256SUMS'), `${sha256}  ${apkName}\n`);
console.log(JSON.stringify(release, null, 2));
