function authenticate_(request, state) {
  if (typeof request.pin !== 'string' || !/^[0-9]{6,12}$/.test(request.pin)) fail_('INVALID_CREDENTIALS');
  const username = username_(request.username);
  const p = props_(), key = 'ATTEMPTS_' + request.deviceId;
  const throttle = JSON.parse(p.getProperty(key) || '{"failures":0,"blockedUntil":0}');
  const now = Date.now();
  if (throttle.blockedUntil > now) fail_('RATE_LIMITED');
  const user = state.users.find(row => row.username === username);
  // Charge each attempt before doing the expensive derivation, including interrupted executions.
  const failures = throttle.blockedUntil ? 1 : throttle.failures + 1;
  p.setProperty(key, JSON.stringify({ failures: failures, blockedUntil: failures >= 5 ? now + 300000 : 0 }));
  const dummy = { salt: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=', pinHash: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=', hashIterations: 210000 };
  const valid = verifyPin_(request.pin, user || dummy);
  if (!valid || !user || !user.active || user.role !== 'ADMIN') {
    appendAudit_(auditEvent_('', 'ADMIN_AUTH_FAILED', '', request.deviceId));
    fail_('INVALID_CREDENTIALS');
  }
  p.deleteProperty(key);
  const token = randomToken_();
  p.setProperty('SESSION_' + request.deviceId, JSON.stringify({
    tokenHash: digest_(token), userId: user.id, userRevision: user.updatedAt, expiresAt: now + 300000
  }));
  appendAudit_(auditEvent_(user.id, 'ADMIN_AUTH_SUCCESS', '', request.deviceId));
  return { success: true, adminToken: token, expiresInSeconds: 300 };
}

function admin_(request, state) {
  if (!/^[A-Za-z0-9_-]{43}$/.test(request.adminToken || '')) fail_('SESSION_EXPIRED');
  const session = JSON.parse(props_().getProperty('SESSION_' + request.deviceId) || '{}');
  if (!(session.expiresAt > Date.now()) || !equal_(session.tokenHash, digest_(request.adminToken))) fail_('SESSION_EXPIRED');
  const user = state.users.find(row => row.id === session.userId);
  if (!user || !user.active || user.role !== 'ADMIN' || user.updatedAt !== session.userRevision) fail_('ADMIN_REQUIRED');
  return user;
}
