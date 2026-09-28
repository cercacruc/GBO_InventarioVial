const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const {test} = require('node:test');
const crypto = require('node:crypto');

function environment() {
  let held = false, releases = 0, reads = 0, sequence = 0;
  const iterator = values => {let i = 0; return {hasNext: () => i < values.length, next: () => values[i++]};};
  const registry = new Map(), properties = new Map();
  function blob(bytes, mime, name) {
    return {bytes, mime, name, getBytes() {return bytes;}, getName() {return this.name;},
      getDataAsString() {return Buffer.from(bytes).toString('utf8');},
      setName(n) {this.name = n; return this;}, copyBlob() {return blob(bytes, mime, this.name);}};
  }
  function folder(name, id = String(++sequence).padStart(4, '0')) {
    const f = {name, id, folders: [], files: [], getId() {return id;},
      getFoldersByName(name) {assert.ok(held); reads++; return iterator(this.folders.filter(f => f.name === name));},
      createFolder(name) {assert.ok(held); const child = folder(name); this.folders.push(child); return child;},
      getFilesByName(name) {assert.ok(held); return iterator(this.files.filter(f => f.getName() === name));},
      createFile(blob) {assert.ok(held); if (this.fail) throw Error('Drive failed');
        const fileId = 'file-' + (++sequence);
        const file = {getId: () => fileId, getName: () => blob.name, getBlob: () => blob,
          getUrl: () => 'https://drive.google.com/file/d/' + fileId, setDescription() {}}; this.files.push(file); return file;}
    };
    registry.set(id, f); return f;
  }
  const root = folder('root');
  const state = {busy: false, competing: null};
  const context = vm.createContext({
    DriveApp: {getFolderById(id) {assert.ok(held); reads++; return registry.get(id) || root;}},
    PropertiesService: {getScriptProperties: () => ({getProperty: key => properties.get(key) || null, setProperty: (key, value) => properties.set(key, value)})},
    LockService: {getScriptLock: () => ({tryLock() {
      if (state.busy || held) return false;
      held = true;
      if (state.competing) {const fn = state.competing; state.competing = null; fn();}
      return true;
    }, releaseLock() {assert.ok(held); held = false; releases++;}})},
    Utilities: {base64Decode: text => [...Buffer.from(text, 'base64')], newBlob: blob,
      DigestAlgorithm: {SHA_256: 'sha256'}, computeDigest: (_, bytes) => [...crypto.createHash('sha256').update(typeof bytes === 'string' ? bytes : Buffer.from(bytes)).digest()],
      base64EncodeWebSafe: bytes => Buffer.from(bytes).toString('base64url'),
      unzip: b => JSON.parse(Buffer.from(b.bytes).toString()).map(f => blob([...Buffer.from(f.text)], 'application/octet-stream', f.name))},
    ContentService: {MimeType: {JSON: 'json'}, createTextOutput: text => ({setMimeType: () => JSON.parse(text)})}
  });
  vm.runInContext(fs.readFileSync(__dirname + '/Code.gs', 'utf8').replace('const API_TOKEN = "PON_AQUI_TU_TOKEN_ACTUAL";', 'const API_TOKEN = "test-only";'), context);
  const payload = {token: 'test-only', routeCode: 'li-100', sibCode: '2', sicCode: 'SIC23', fileName: 'photo-1.jpg', base64: 'AQID'};
  const send = (changes = {}) => context.doPost({postData: {contents: JSON.stringify({...payload, ...changes})}});
  return {context, root, state, folder, send, registry, properties, get reads() {return reads;}, get held() {return held;}, get releases() {return releases;}};
}

test('several photos share route and SIB and retry does not create another file', () => {
  const e = environment(); const first = e.send(); const second = e.send({fileName: 'photo-2.jpg'}); const retry = e.send();
  assert.equal(first.ok, true); assert.equal(first.routeFolderId, second.routeFolderId); assert.equal(first.sibFolderId, second.sibFolderId);
  assert.equal(e.root.folders.length, 1); assert.equal(e.root.folders[0].folders.length, 1);
  assert.equal(e.root.folders[0].folders[0].files.length, 2); assert.equal(retry.alreadyExists, true); assert.equal(e.releases, 3);
});
test('competing request cannot reach Drive and can retry after the first finishes', () => {
  const e = environment(); let competitor;
  e.state.competing = () => {competitor = e.send({fileName: 'photo-2.jpg'}); assert.equal(e.reads, 0);};
  assert.equal(e.send().ok, true); assert.equal(competitor.ok, false); assert.equal(competitor.retryable, true);
  assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, true); assert.equal(e.root.folders.length, 1);
});
test('busy lock does not release another execution lock or touch Drive', () => {
  const e = environment(); e.state.busy = true;
  assert.equal(e.send().retryable, true); assert.equal(e.reads, 0); assert.equal(e.releases, 0);
});
test('Drive failure releases lock and a later request can proceed', () => {
  const e = environment(); e.send(); const target = e.root.folders[0].folders[0]; target.fail = true;
  assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, false); assert.equal(e.held, false);
  target.fail = false; assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, true);
});
test('duplicate existing folders are selected consistently without deleting either', () => {
  const e = environment(); const a = e.folder('LI-100', 'a'); const z = e.folder('LI-100', 'z');
  e.root.folders.push(z, a); assert.equal(e.send().routeFolderId, 'a');
  e.root.folders.reverse(); assert.equal(e.send({fileName: 'photo-2.jpg'}).routeFolderId, 'a');
  assert.equal(e.root.folders.length, 2); assert.equal(z.files.length, 0);
});
test('unauthorized and invalid codes do not access Drive', () => {
  const e = environment();
  for (const changes of [{token: 'wrong'}, {routeCode: ' '}, {sibCode: '2garbage'}, {sicCode: 'SIC-23text'}, {sicCode: '24'}, {base64: ''}]) {
    assert.equal(e.send(changes).ok, false);
  }
  assert.equal(e.reads, 0); assert.equal(e.releases, 0);
});
test('SIC remains optional and canonical codes remain compatible with Android', () => {
  const e = environment(); const result = e.send({sicCode: null});
  assert.equal(result.ok, true); assert.equal(result.routeCode, 'LI-100'); assert.equal(result.sibCode, 'SIB-02'); assert.equal(result.sicCode, null);
});
test('malformed JSON is handled without creating folders', () => {
  const e = environment(); assert.equal(e.context.doPost({postData: {contents: '{'}}).ok, false);
  assert.equal(e.context.doPost(null).ok, false); assert.equal(e.reads, 0);
});

const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
function scap(changes = {}, extra = []) {
  const p = {action: 'uploadScap', recordKind: 'SCAP', protocol: 1,
    scapInspectionId: '00000000-0000-0000-0000-000000000001', revision: 100,
    bridgeCode: 'PE-3N/P-001', bridgeName: 'Puente de prueba', ...changes};
  const entries = [{name: 'SCAP.xlsx', text: 'synthetic workbook'}, ...extra];
  const manifest = {...p, files: entries.map(f => ({path: f.name, sha256: hash(f.text)}))};
  entries.push({name: 'manifest.json', text: JSON.stringify(manifest)});
  const bytes = Buffer.from(JSON.stringify(entries)); // unzip is mocked; real ZIP tested on Android.
  return {...p, base64: bytes.toString('base64'), sha256: hash(bytes)};
}
test('supplied v3 SIC Excel and ZIP exports retain content validation and idempotency', () => {
  const e = environment(); const p = {action: 'uploadSicExport', fileName: 'SIC_2026.xlsx'};
  const first = e.send(p); assert.equal(first.ok, true); assert.ok(first.fileUrl);
  assert.equal(e.send(p).alreadyExists, true);
  assert.equal(e.send({...p, base64: 'BAUG'}).ok, false);
  assert.equal(e.send({...p, fileName: 'SIC_2026.zip'}).ok, true);
  assert.equal(e.root.folders[0].name, 'EXPORTACIONES_SIC');
});
test('SCAP stores its workbook, separate photos and ZIP; retry confirms the same file', () => {
  const e = environment(), p = scap({}, [{name: 'fotos/00000000-0000-0000-0000-000000000002.png', text: 'image'}]);
  const first = e.send(p), retry = e.send(p);
  assert.equal(first.ok, true); assert.equal(first.sha256, p.sha256); assert.equal(first.revision, 100);
  assert.equal(first.fileId, retry.fileId); assert.equal(retry.alreadyExists, true); assert.equal(first.fileCount, 3);
  assert.equal(e.root.folders[0].name, 'SCAP'); assert.equal(e.registry.get(first.folderId).files.length, 3);
  assert.equal(e.registry.get(first.folderId).folders[0].files.length, 1);
  assert.equal(e.send().ok, true); assert.equal(e.root.folders[1].name, 'LI-100');
});
test('bridge identity uses the full code, groups inspections and never groups only by name', () => {
  const e = environment(); const first = e.send(scap());
  const second = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000002'}));
  assert.equal(first.bridgeFolderId, second.bridgeFolderId); assert.notEqual(first.folderId, second.folderId);
  const third = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000003', bridgeCode: 'PE-3N-P-001'}));
  assert.notEqual(first.bridgeFolderId, third.bridgeFolderId);
});
test('missing code uses inspection UUID; later code retains the folder and can group future inspections', () => {
  const e = environment(); const first = e.send(scap({bridgeCode: ''}));
  const second = e.send(scap({revision: 101}));
  assert.equal(first.bridgeFolderId, second.bridgeFolderId);
  const third = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000002'}));
  assert.equal(second.bridgeFolderId, third.bridgeFolderId);
});
test('conflicting existing identity requires review instead of merging or moving files', () => {
  const e = environment(); e.send(scap());
  const id = '00000000-0000-0000-0000-000000000002'; e.send(scap({scapInspectionId: id, bridgeCode: ''}));
  const conflict = e.send(scap({scapInspectionId: id, revision: 101}));
  assert.equal(conflict.ok, false); assert.equal(conflict.retryable, false); assert.equal(e.held, false);
});
test('SCAP rejects unauthorized requests, hash mismatches, unsafe entries and old protocol before touching Drive', () => {
  const e = environment(); const valid = scap();
  for (const p of [{...valid, token: 'bad'}, {...valid, protocol: 0}, {...valid, sha256: '0'.repeat(64)},
    scap({}, [{name: '../foreign.png', text: 'x'}]), {...valid, action: 'photo'}]) assert.equal(e.send(p).ok, false);
  assert.equal(e.reads, 0);
});
test('SCAP lock contention and partial Drive failures retry without duplicate completed files', () => {
  const e = environment(), p = scap(); e.state.busy = true;
  assert.equal(e.send(p).retryable, true); assert.equal(e.reads, 0); e.state.busy = false;
  const first = e.send(p); const target = e.registry.get(first.folderId);
  target.files.pop(); target.fail = true;
  assert.equal(e.send(p).ok, false); assert.equal(e.held, false);
  target.fail = false; assert.equal(e.send(p).ok, true); assert.equal(target.files.length, 3);
});
