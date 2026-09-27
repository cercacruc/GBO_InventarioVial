# Cryptographic dependency

CryptoJS **4.2.0**, MIT, upstream https://github.com/brix/crypto-js .
`../Crypto.gs` concatenates the unmodified npm distribution files `core.js`,
`enc-base64.js`, `sha256.js`, `hmac.js`, `pbkdf2.js` in that order, with a provenance header.
No runtime downloads, remote Apps Script library or Node dependencies are required.
The application supplies salts explicitly. CryptoJS random generation is never used.

Source archive: https://registry.npmjs.org/crypto-js/-/crypto-js-4.2.0.tgz

SHA-256 archive: `2d288a658b3eae000d7fadfdfdf5fe2bec3952cb19212360db8c7686c2b6ce09`

SHA-256 `Crypto.gs` (UTF-8, CRLF module source): `49c3efc1f2813c738cd12e14d02d5eeb6859bdc5db43982ec34657d83df86894`

PBKDF2-HMAC-SHA256 uses 210,000 iterations, a 32-byte salt and a 32-byte key.
`tests/service.test.cjs` cross-checks the bundled implementation against Node's native PBKDF2.
Measure Apps Script latency and quotas in the actual deployment before fleet rollout.
