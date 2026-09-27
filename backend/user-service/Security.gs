function digest_(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, value, Utilities.Charset.UTF_8)
    .map(b => ('0' + ((b + 256) % 256).toString(16)).slice(-2)).join('');
}

function equal_(a, b) {
  if (typeof a !== 'string' || typeof b !== 'string') return false;
  let mismatch = a.length ^ b.length;
  for (let i = 0; i < Math.max(a.length, b.length); i++) mismatch |= (a.charCodeAt(i) || 0) ^ (b.charCodeAt(i) || 0);
  return mismatch === 0;
}

function tablet_(request) {
  if (!/^[a-f0-9]{64}$/.test(request.deviceId || '') || !/^[A-Za-z0-9_-]{43}$/.test(request.tabletToken || '')) fail_('DEVICE_REVOKED');
  const tablet = JSON.parse(props_().getProperty('TABLET_' + request.deviceId) || '{}');
  if (tablet.active !== true || !equal_(tablet.tokenHash, digest_(request.tabletToken))) fail_('DEVICE_REVOKED');
}

// A PRF over a monotonically increasing counter, seeded with 256 bits from Node crypto.randomBytes.
// All callers hold the script lock. No Math.random or undocumented UUID entropy is used for secrets.
function randomToken_() {
  const p = props_(), key = p.getProperty('SERVER_RANDOM_KEY');
  if (!/^[a-f0-9]{64}$/.test(key || '')) fail_('SERVICE_ERROR');
  const counter = Number(p.getProperty('RANDOM_COUNTER') || '0') + 1;
  if (!Number.isSafeInteger(counter)) fail_('SERVICE_ERROR');
  p.setProperty('RANDOM_COUNTER', String(counter));
  return Utilities.base64EncodeWebSafe(Utilities.computeHmacSha256Signature('gbo-admin-v1:' + counter, key, Utilities.Charset.UTF_8)).replace(/=+$/, '');
}

function validateCredential_(credential) {
  keys_(credential, ['salt', 'pinHash', 'hashIterations']);
  if (credential.hashIterations !== 210000 || typeof credential.salt !== 'string' || typeof credential.pinHash !== 'string' ||
      !/^[A-Za-z0-9+/]{43}=$/.test(credential.salt) || !/^[A-Za-z0-9+/]{43}=$/.test(credential.pinHash) ||
      Utilities.base64Decode(credential.salt).length !== 32 || Utilities.base64Decode(credential.pinHash).length !== 32) fail_('INVALID_REQUEST');
  return credential;
}

function verifyPin_(pin, user) {
  const result = CryptoJS.PBKDF2(pin, CryptoJS.enc.Base64.parse(user.salt), {
    keySize: 8, iterations: user.hashIterations, hasher: CryptoJS.algo.SHA256
  }).toString(CryptoJS.enc.Base64);
  return equal_(result, user.pinHash);
}
