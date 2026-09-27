function auditEvent_(admin, action, target, device) {
  return { timestamp: Date.now(), adminUserId: admin, action: action, targetUserId: target, deviceId: device };
}
function auditRequest_(book, event) {
  return { appendCells: { sheetId: book.getSheetByName('Audit').getSheetId(), fields: 'userEnteredValue',
    rows: [{ values: [event.timestamp, event.adminUserId, event.action, event.targetUserId, event.deviceId].map(cell_) }] } };
}
function appendAudit_(event) {
  const book = book_();
  Sheets.Spreadsheets.batchUpdate({ requests: [auditRequest_(book, event)] }, book.getId());
}
function readAudit_() {
  const sheet = book_().getSheetByName('Audit'), last = sheet.getLastRow();
  return last ? sheet.getRange(Math.max(1, last - 199), 1, Math.min(last, 200), 5).getValues() : [];
}
