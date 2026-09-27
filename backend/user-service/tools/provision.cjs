#!/usr/bin/env node
'use strict';
// Run interactively on a trusted GBO computer. Never pass a PIN on the command line.
const crypto = require('node:crypto');
const readline = require('node:readline/promises');
const { Writable } = require('node:stream');
let hidden = false;
const output = new Writable({ write(chunk, encoding, done) { if (!hidden) process.stdout.write(chunk); done(); } });
const rl = readline.createInterface({ input: process.stdin, output, terminal: Boolean(process.stdin.isTTY) });
async function prompt(label, secret = false) {
  if (secret && !process.stdin.isTTY) throw new Error('El PIN requiere una terminal interactiva.');
  process.stdout.write(label);
  hidden = secret;
  try { return await rl.question(''); } finally { hidden = false; if (secret) process.stdout.write('\n'); }
}
async function main() {
  const mode = process.argv[2];
  if (mode === 'server-key') {
    console.log('SERVER_RANDOM_KEY (guardar solo en Script Properties):\n' + crypto.randomBytes(32).toString('hex'));
  } else if (mode === 'admin') {
    const username = (await prompt('Usuario ADMIN (3–64 letras/números/punto/guion): ')).trim().toLowerCase();
    const displayName = (await prompt('Nombre completo: ')).trim();
    if (!/^[a-z0-9._-]{3,64}$/.test(username) || !displayName || displayName.length > 120) throw new Error('Usuario o nombre inválido.');
    let pin = await prompt('PIN (6–12 dígitos, oculto): ', true);
    let confirmation = await prompt('Repite el PIN: ', true);
    if (!/^[0-9]{6,12}$/.test(pin) || pin !== confirmation) throw new Error('Los PIN no coinciden o son inválidos.');
    const salt = crypto.randomBytes(32);
    const hash = crypto.pbkdf2Sync(pin, salt, 210000, 32, 'sha256');
    pin = ''; confirmation = '';
    console.log('BOOTSTRAP_ADMIN (verificador sensible; no guardar en Git):\n' + JSON.stringify({ username, displayName,
      credential: { salt: salt.toString('base64'), pinHash: hash.toString('base64'), hashIterations: 210000 } }));
    hash.fill(0);
  } else if (mode === 'tablet') {
    const deviceId = (await prompt('Huella completa de la tablet (64 caracteres): ')).trim().toLowerCase();
    if (!/^[a-f0-9]{64}$/.test(deviceId)) throw new Error('Huella inválida.');
    const token = crypto.randomBytes(32).toString('base64url');
    console.log('TABLET_PROVISION (copiar a Script Properties):\n' + JSON.stringify({ deviceId, tokenHash: crypto.createHash('sha256').update(token).digest('hex') }));
    console.log('Token para introducir SOLO en esta tablet; entregar por un canal seguro:\n' + token);
  } else throw new Error('Uso: node backend/user-service/tools/provision.cjs server-key|admin|tablet');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; }).finally(() => rl.close());
