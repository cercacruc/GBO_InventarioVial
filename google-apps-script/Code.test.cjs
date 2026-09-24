const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const {test} = require('node:test');

function environment() {
  let held = false, releases = 0, reads = 0, sequence = 0;
  const iterator = values => {let i = 0; return {hasNext: () => i < values.length, next: () => values[i++]};};
  function folder(name, id = String(++sequence).padStart(4, '0')) {
    return {name, id, folders: [], files: [], getId() {return id;},
      getFoldersByName(name) {assert.ok(held); reads++; return iterator(this.folders.filter(f => f.name === name));},
      createFolder(name) {assert.ok(held); const child = folder(name); this.folders.push(child); return child;},
      getFilesByName(name) {assert.ok(held); return iterator(this.files.filter(f => f.getName() === name));},
      createFile(blob) {assert.ok(held); if (this.fail) throw Error('Drive failed');
        const file = {getId: () => 'file-' + this.files.length, getName: () => blob.name}; this.files.push(file); return file;}
    };
  }
  const root = folder('root');
  const state = {busy: false, competing: null};
  const context = vm.createContext({
    DriveApp: {getFolderById() {assert.ok(held); reads++; return root;}},
    LockService: {getScriptLock: () => ({tryLock() {
      if (state.busy || held) return false;
      held = true;
      if (state.competing) {const fn = state.competing; state.competing = null; fn();}
      return true;
    }, releaseLock() {assert.ok(held); held = false; releases++;}})},
    Utilities: {base64Decode: text => [...Buffer.from(text, 'base64')], newBlob: (bytes, mime, name) => ({bytes, mime, name})},
    ContentService: {MimeType: {JSON: 'json'}, createTextOutput: text => ({setMimeType: () => JSON.parse(text)})}
  });
  vm.runInContext(fs.readFileSync(__dirname + '/Code.gs', 'utf8').replace('const API_TOKEN = "PON_AQUI_TU_TOKEN_ACTUAL";', 'const API_TOKEN = "test-only";'), context);
  const payload = {token: 'test-only', routeCode: 'li-100', sibCode: '2', sicCode: 'SIC23', fileName: 'photo-1.jpg', base64: 'AQID'};
  const send = (changes = {}) => context.doPost({postData: {contents: JSON.stringify({...payload, ...changes})}});
  return {context, root, state, folder, send, get reads() {return reads;}, get held() {return held;}, get releases() {return releases;}};
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
