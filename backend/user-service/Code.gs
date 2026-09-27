/* GBO users service. Deploy separately from the existing photo/ZIP service. */
function doPost(e) {
  let result;
  try {
    if (!e || !e.postData || e.postData.contents.length > 8192) fail_('INVALID_REQUEST');
    result = dispatch_(JSON.parse(e.postData.contents));
  } catch (error) {
    // Never return or log payloads, PINs, tokens, hashes or internal errors.
    result = { success: false, error: error && error.safeCode || 'SERVICE_ERROR' };
  }
  return ContentService.createTextOutput(JSON.stringify(result)).setMimeType(ContentService.MimeType.JSON);
}

function doGet() {
  return ContentService.createTextOutput(JSON.stringify({ success: false, error: 'POST_REQUIRED' }))
    .setMimeType(ContentService.MimeType.JSON);
}

function dispatch_(request) {
  const lock = LockService.getScriptLock();
  if (!lock.tryLock(10000)) fail_('SERVICE_ERROR');
  try {
    const allowed = {
      syncUsers: ['clientVersion'], adminAuthenticate: ['username', 'pin'],
      createUser: ['adminToken', 'username', 'displayName', 'role', 'credential'],
      updateUser: ['adminToken', 'id', 'displayName'],
      setUserActive: ['adminToken', 'id', 'active'],
      changeUserRole: ['adminToken', 'id', 'role'],
      resetUserPin: ['adminToken', 'id', 'credential'],
      getAudit: ['adminToken']
    };
    if (!request || !Object.prototype.hasOwnProperty.call(allowed, request.action)) fail_('INVALID_REQUEST');
    keys_(request, ['action', 'deviceId', 'tabletToken'].concat(allowed[request.action]));
    tablet_(request);
    const state = readState_();
    if (request.action === 'syncUsers') {
      if (!Number.isSafeInteger(request.clientVersion) || request.clientVersion < -1 || request.clientVersion > state.version) fail_('INVALID_REQUEST');
      if (request.clientVersion === state.version) return { success: true, changed: false, version: state.version };
      return { success: true, changed: true, version: state.version, users: state.users.map(publicUser_) };
    }
    if (request.action === 'adminAuthenticate') return authenticate_(request, state);
    const admin = admin_(request, state);
    if (request.action === 'getAudit') return { success: true, events: readAudit_() };
    return mutateUser_(request, state, admin);
  } finally { lock.releaseLock(); }
}

function fail_(code) { const error = new Error(code); error.safeCode = code; throw error; }
function keys_(object, allowed) {
  if (!object || Array.isArray(object) || typeof object !== 'object' ||
      Object.keys(object).some(key => allowed.indexOf(key) < 0) || allowed.some(key => !Object.prototype.hasOwnProperty.call(object, key))) fail_('INVALID_REQUEST');
}

function props_() { return PropertiesService.getScriptProperties(); }

// Run once from the owner-only script editor; not exposed by doPost.
function setupUserService() {
  const lock = LockService.getScriptLock(); lock.waitLock(10000);
  try {
    const p = props_();
    if (!/^[a-f0-9]{64}$/.test(p.getProperty('SERVER_RANDOM_KEY') || '')) throw new Error('Configure SERVER_RANDOM_KEY with the local provisioning tool.');
    const book = SpreadsheetApp.openById(p.getProperty('USERS_SPREADSHEET_ID'));
    ['Users', 'Meta', 'Audit'].forEach(name => { if (!book.getSheetByName(name)) book.insertSheet(name); });
    const meta = book.getSheetByName('Meta');
    if (meta.getRange('A1').isBlank()) meta.getRange('A1').setValue(0);
    SpreadsheetApp.flush();
  } finally { lock.releaseLock(); }
}

function bootstrapFirstAdmin() {
  const lock = LockService.getScriptLock(); lock.waitLock(10000);
  try {
    const p = props_();
    if (p.getProperty('BOOTSTRAP_ENABLED') !== 'true') fail_('ADMIN_REQUIRED');
    const state = readState_();
    if (state.users.length || state.version !== 0) fail_('ADMIN_REQUIRED');
    const input = JSON.parse(p.getProperty('BOOTSTRAP_ADMIN') || '{}');
    keys_(input, ['username', 'displayName', 'credential']);
    const user = newUser_(input.username, input.displayName, 'ADMIN', input.credential);
    state.users.push(user);
    commit_(state, auditEvent_(user.id, 'USER_CREATED', user.id, 'OWNER_SETUP'));
    p.deleteProperty('BOOTSTRAP_ADMIN');
    p.setProperty('BOOTSTRAP_ENABLED', 'false');
  } finally { lock.releaseLock(); }
}

function provisionTablet() {
  const lock = LockService.getScriptLock(); lock.waitLock(10000);
  try {
    const p = props_();
    const input = JSON.parse(p.getProperty('TABLET_PROVISION') || '{}');
    keys_(input, ['deviceId', 'tokenHash']);
    if (!/^[a-f0-9]{64}$/.test(input.deviceId) || !/^[a-f0-9]{64}$/.test(input.tokenHash)) fail_('INVALID_REQUEST');
    p.setProperty('TABLET_' + input.deviceId, JSON.stringify({ tokenHash: input.tokenHash, active: true }));
    p.deleteProperty('SESSION_' + input.deviceId);
    p.deleteProperty('TABLET_PROVISION');
  } finally { lock.releaseLock(); }
}

function revokeTablet() {
  const lock = LockService.getScriptLock(); lock.waitLock(10000);
  try {
    const p = props_(), device = p.getProperty('TABLET_REVOKE_ID');
    if (!/^[a-f0-9]{64}$/.test(device || '')) fail_('INVALID_REQUEST');
    const stored = JSON.parse(p.getProperty('TABLET_' + device) || '{}');
    stored.active = false;
    p.setProperty('TABLET_' + device, JSON.stringify(stored));
    p.deleteProperty('SESSION_' + device);
    p.deleteProperty('TABLET_REVOKE_ID');
    appendAudit_(auditEvent_('OWNER', 'TABLET_REVOKED', '', device));
  } finally { lock.releaseLock(); }
}
