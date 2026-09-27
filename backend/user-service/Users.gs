function username_(value) {
  if (typeof value !== 'string') fail_('INVALID_REQUEST');
  const normalized = value.trim().toLowerCase();
  if (!/^[a-z0-9._-]{3,64}$/.test(normalized)) fail_('INVALID_REQUEST');
  return normalized;
}
function name_(value) {
  if (typeof value !== 'string' || !value.trim() || value.trim().length > 120) fail_('INVALID_REQUEST');
  return value.trim();
}
function role_(value) { if (value !== 'ADMIN' && value !== 'OPERATOR') fail_('INVALID_REQUEST'); return value; }
function publicUser_(user) {
  // Explicit allowlist: no sessions, tablet tokens, PINs or additional internal data.
  const result = {};
  ['id', 'username', 'displayName', 'role', 'active', 'salt', 'pinHash', 'hashIterations', 'createdAt', 'updatedAt'].forEach(key => { result[key] = user[key]; });
  return result;
}
function newUser_(username, displayName, role, credential) {
  validateCredential_(credential);
  return { id: Utilities.getUuid(), username: username_(username), displayName: name_(displayName), role: role_(role),
    active: true, salt: credential.salt, pinHash: credential.pinHash, hashIterations: credential.hashIterations,
    createdAt: Date.now(), updatedAt: Date.now() };
}
function mutateUser_(request, state, admin) {
  let user, action;
  if (request.action === 'createUser') {
    const username = username_(request.username);
    if (state.users.some(row => row.username === username)) fail_('DUPLICATE_USERNAME');
    if (state.users.length >= 1000) fail_('INVALID_REQUEST');
    user = newUser_(username, request.displayName, request.role, request.credential);
    state.users.push(user); action = 'USER_CREATED';
  } else {
    user = state.users.find(row => row.id === request.id);
    if (!user) fail_('INVALID_REQUEST');
    if (request.action === 'updateUser') { user.displayName = name_(request.displayName); action = 'USER_UPDATED'; }
    if (request.action === 'setUserActive') {
      if (typeof request.active !== 'boolean') fail_('INVALID_REQUEST');
      user.active = request.active; action = user.active ? 'USER_ENABLED' : 'USER_DISABLED';
    }
    if (request.action === 'changeUserRole') { user.role = role_(request.role); action = 'USER_ROLE_CHANGED'; }
    if (request.action === 'resetUserPin') {
      const credential = validateCredential_(request.credential);
      user.salt = credential.salt; user.pinHash = credential.pinHash; user.hashIterations = credential.hashIterations;
      action = 'USER_PIN_RESET';
    }
    user.updatedAt = Math.max(Date.now(), user.updatedAt + 1);
  }
  if (!state.users.some(row => row.active && row.role === 'ADMIN')) fail_('LAST_ADMIN');
  commit_(state, auditEvent_(admin.id, action, user.id, request.deviceId));
  return { success: true, id: user.id, version: state.version };
}

function book_() { return SpreadsheetApp.openById(props_().getProperty('USERS_SPREADSHEET_ID')); }
function readState_() {
  const book = book_(), users = book.getSheetByName('Users'), meta = book.getSheetByName('Meta');
  if (!users || !meta) fail_('SERVICE_ERROR');
  const version = Number(meta.getRange('A1').getValue());
  if (!Number.isSafeInteger(version) || version < 0) fail_('SERVICE_ERROR');
  const rows = users.getLastRow() ? users.getRange(1, 1, users.getLastRow(), 1).getValues().map(row => JSON.parse(row[0])) : [];
  return { version: version, users: rows };
}
function cell_(value) { return { userEnteredValue: typeof value === 'number' ? { numberValue: value } : { stringValue: String(value) } }; }
function commit_(state, event) {
  const book = book_();
  state.version++;
  // One Sheets batch: user snapshot, version and audit either all commit or none commit.
  // stringValue avoids formula injection. Users are soft-deleted, so the range never shrinks.
  Sheets.Spreadsheets.batchUpdate({ requests: [
    { updateCells: { start: { sheetId: book.getSheetByName('Users').getSheetId(), rowIndex: 0, columnIndex: 0 },
      rows: state.users.map(user => ({ values: [cell_(JSON.stringify(publicUser_(user)))] })), fields: 'userEnteredValue' } },
    { updateCells: { start: { sheetId: book.getSheetByName('Meta').getSheetId(), rowIndex: 0, columnIndex: 0 },
      rows: [{ values: [cell_(state.version)] }], fields: 'userEnteredValue' } },
    auditRequest_(book, event)
  ] }, book.getId());
}
