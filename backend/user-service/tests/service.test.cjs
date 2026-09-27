'use strict';
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.resolve(__dirname, '..');
let clock = 1_800_000_000_000;
const properties = new Map();
let sheets = [];
const propService = {
  getProperty: key => properties.get(key) ?? null,
  setProperty: (key, value) => { properties.set(key, value); },
  deleteProperty: key => { properties.delete(key); }
};
function getSheet(name) {
  const sheet = sheets.find(s => s.name === name);
  if (!sheet) return null;
  return {
    getSheetId: () => sheet.id,
    getLastRow: () => sheet.rows.length,
    getRange: (row, column, count = 1, cols = 1) => {
      if (row === 'A1') { row = 1; column = 1; }
      return {
        isBlank: () => sheet.rows[row - 1]?.[column - 1] == null,
        setValue: value => { sheet.rows[row - 1] ??= []; sheet.rows[row - 1][column - 1] = value; },
        getValue: () => sheet.rows[row - 1]?.[column - 1],
        getValues: () => Array.from({ length: count }, (_, i) => Array.from({ length: cols }, (_, j) => sheet.rows[row + i - 1]?.[column + j - 1] ?? ''))
      };
    }
  };
}
const book = { getId: () => 'private-test-sheet', getSheetByName: getSheet,
  insertSheet: name => { sheets.push({ id: sheets.length + 1, name, rows: [] }); return getSheet(name); } };
function batchUpdate(batch) {
  const next = structuredClone(sheets);
  for (const request of batch.requests) {
    const change = request.updateCells || request.appendCells;
    const sheet = next.find(s => s.id === (change.sheetId || change.start.sheetId));
    const start = change.start?.rowIndex ?? sheet.rows.length;
    change.rows.forEach((row, index) => { sheet.rows[start + index] = row.values.map(cell => cell.userEnteredValue.stringValue ?? cell.userEnteredValue.numberValue); });
  }
  sheets = next;
}
const context = vm.createContext({
  Date: class extends Date { static now() { return clock; } },
  PropertiesService: { getScriptProperties: () => propService },
  LockService: { getScriptLock: () => ({ tryLock: () => true, waitLock() {}, releaseLock() {} }) },
  SpreadsheetApp: { openById: () => book, flush() {} },
  Sheets: { Spreadsheets: { batchUpdate } },
  Utilities: {
    getUuid: crypto.randomUUID,
    DigestAlgorithm: { SHA_256: 'sha256' }, Charset: { UTF_8: 'utf8' },
    computeDigest: (algorithm, value) => [...crypto.createHash(algorithm).update(value).digest()],
    computeHmacSha256Signature: (value, key) => [...crypto.createHmac('sha256', key).update(value).digest()],
    base64EncodeWebSafe: bytes => Buffer.from(bytes).toString('base64url'),
    base64Decode: value => [...Buffer.from(value, 'base64')]
  }
});
for (const file of ['Crypto.gs', 'Code.gs', 'Users.gs', 'Security.gs', 'Auth.gs', 'Audit.gs']) vm.runInContext(fs.readFileSync(path.join(root, file), 'utf8'), context, { filename: file });
const fixturePin = '783294'; // Synthetic test credential, never provisioned.
function credential(pin = fixturePin) {
  const salt = crypto.randomBytes(32);
  return { salt: salt.toString('base64'), pinHash: crypto.pbkdf2Sync(pin, salt, 210000, 32, 'sha256').toString('base64'), hashIterations: 210000 };
}
const deviceId = 'a'.repeat(64), tabletToken = crypto.randomBytes(32).toString('base64url');
properties.set('SERVER_RANDOM_KEY', crypto.randomBytes(32).toString('hex'));
properties.set('USERS_SPREADSHEET_ID', 'private-test-sheet');
properties.set('BOOTSTRAP_ENABLED', 'true');
properties.set('BOOTSTRAP_ADMIN', JSON.stringify({ username: 'OWNER', displayName: 'Owner test', credential: credential() }));
context.setupUserService(); context.bootstrapFirstAdmin();
properties.set('TABLET_PROVISION', JSON.stringify({ deviceId, tokenHash: crypto.createHash('sha256').update(tabletToken).digest('hex') }));
context.provisionTablet();
const request = (action, fields = {}) => context.dispatch_({ action, deviceId, tabletToken, ...fields });
const rejects = (fn, code) => assert.throws(fn, error => error.safeCode === code);
let passed = 0;
function test(name, fn) { fn(); passed++; console.log('PASS ' + name); }
let adminToken, operatorId, ownerId;
test('bootstrap is explicit, central, disabled after use', () => {
  assert.equal(properties.get('BOOTSTRAP_ENABLED'), 'false');
  assert.equal(properties.has('BOOTSTRAP_ADMIN'), false);
  rejects(() => context.bootstrapFirstAdmin(), 'ADMIN_REQUIRED');
});
test('sync changed and no PIN or tokens in response', () => {
  const result = request('syncUsers', { clientVersion: -1 });
  assert.equal(result.changed, true); assert.equal(result.version, 1);
  assert.equal(result.users[0].username, 'owner'); ownerId = result.users[0].id;
  assert.equal('pin' in result.users[0], false); assert.equal('tabletToken' in result, false);
});
test('same version does not download users', () => {
  const result = request('syncUsers', { clientVersion: 1 });
  assert.equal(result.changed, false); assert.equal('users' in result, false);
});
test('invalid tablet token and unknown fields rejected', () => {
  rejects(() => request('syncUsers', { clientVersion: 1, tabletToken: 'X'.repeat(43) }), 'DEVICE_REVOKED');
  rejects(() => request('syncUsers', { clientVersion: 1, role: 'ADMIN' }), 'INVALID_REQUEST');
});
test('PBKDF2 interoperates with native Node and rejects wrong PIN', () => {
  assert.equal(context.verifyPin_(fixturePin, context.readState_().users[0]), true);
  assert.equal(context.verifyPin_('000000', context.readState_().users[0]), false);
});
test('online admin authentication issues short session', () => {
  const result = request('adminAuthenticate', { username: 'OWNER', pin: fixturePin });
  assert.equal(result.expiresInSeconds, 300); assert.match(result.adminToken, /^[A-Za-z0-9_-]{43}$/);
  adminToken = result.adminToken;
});
test('create operator increments version', () => {
  const result = request('createUser', { adminToken, username: 'field.user', displayName: 'Field User', role: 'OPERATOR', credential: credential() });
  assert.equal(result.version, 2); operatorId = result.id;
});
test('duplicate username is case insensitive without version change', () => {
  rejects(() => request('createUser', { adminToken, username: 'FIELD.USER', displayName: 'Other', role: 'OPERATOR', credential: credential() }), 'DUPLICATE_USERNAME');
  assert.equal(context.readState_().version, 2);
});
test('operator cannot authorize administration', () => {
  rejects(() => request('adminAuthenticate', { username: 'field.user', pin: fixturePin }), 'INVALID_CREDENTIALS');
});
test('last admin cannot be disabled or demoted; snapshot unchanged', () => {
  rejects(() => request('setUserActive', { adminToken, id: ownerId, active: false }), 'LAST_ADMIN');
  rejects(() => request('changeUserRole', { adminToken, id: ownerId, role: 'OPERATOR' }), 'LAST_ADMIN');
  const state = context.readState_(); assert.equal(state.version, 2); assert.equal(state.users[0].active, true);
});
test('disable and enable user increments version', () => {
  assert.equal(request('setUserActive', { adminToken, id: operatorId, active: false }).version, 3);
  assert.equal(context.readState_().users[1].active, false);
  assert.equal(request('setUserActive', { adminToken, id: operatorId, active: true }).version, 4);
});
test('role and name changes are central', () => {
  assert.equal(request('changeUserRole', { adminToken, id: operatorId, role: 'ADMIN' }).version, 5);
  assert.equal(request('updateUser', { adminToken, id: operatorId, displayName: '=not-a-formula' }).version, 6);
  assert.equal(context.readState_().users[1].displayName, '=not-a-formula');
});
test('reset PIN replaces verifier', () => {
  const changed = credential('124578');
  assert.equal(request('resetUserPin', { adminToken, id: operatorId, credential: changed }).version, 7);
  assert.equal(context.verifyPin_('124578', context.readState_().users[1]), true);
  assert.equal(context.verifyPin_(fixturePin, context.readState_().users[1]), false);
});
test('central audit does not contain secret material', () => {
  const serialized = JSON.stringify(request('getAudit', { adminToken }));
  for (const secret of [fixturePin, tabletToken, adminToken, 'pinHash', 'salt']) assert.equal(serialized.includes(secret), false);
  assert.equal(serialized.includes('USER_PIN_RESET'), true);
});
test('expired admin session rejected', () => {
  clock += 300001;
  rejects(() => request('setUserActive', { adminToken, id: operatorId, active: false }), 'SESSION_EXPIRED');
});
test('five failed attempts temporarily block administrative reauthentication', () => {
  request('adminAuthenticate', { username: 'owner', pin: fixturePin });
  for (let i = 0; i < 5; i++) rejects(() => request('adminAuthenticate', { username: 'owner', pin: '000000' }), 'INVALID_CREDENTIALS');
  rejects(() => request('adminAuthenticate', { username: 'owner', pin: fixturePin }), 'RATE_LIMITED');
  clock += 300001;
  adminToken = request('adminAuthenticate', { username: 'owner', pin: fixturePin }).adminToken;
});
test('revoked tablet loses sync and administrative access', () => {
  properties.set('TABLET_REVOKE_ID', deviceId); context.revokeTablet();
  rejects(() => request('syncUsers', { clientVersion: 7 }), 'DEVICE_REVOKED');
  rejects(() => request('getAudit', { adminToken }), 'DEVICE_REVOKED');
});
console.log(`${passed} service tests passed.`);
