// ============================================================
// CONFIGURACIÓN
//
// Fotos: CARPETA RAÍZ / RUTA / SIB / FOTO
// Excel: CARPETA RAÍZ / EXPORTACIONES_SIC / ARCHIVO
// ============================================================

const ROOT_FOLDER_ID = "1LT2GR1GhZOBlHlClApoBiqSh7oFoPv9R";

const API_TOKEN = "PON_AQUI_TU_TOKEN_ACTUAL";


// ============================================================
// COMPROBAR QUE LA API FUNCIONA
// ============================================================

function doGet() {
  return jsonResponse({
    ok: true,
    message: "Inventario Vial API funcionando",
    version: "v4 · SIC y SCAP con confirmación SHA-256", scapProtocol: 1
  });
}


// ============================================================
// RECIBIR FOTOGRAFÍA O EXPORTACIÓN SIC
// ============================================================

function doPost(e) {
  let lock = null;
  let acquired = false;

  try {
    // 1. Validar solicitud.
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({
        ok: false,
        error: "Solicitud vacía"
      });
    }

    const data = JSON.parse(e.postData.contents);

    if (
      !data ||
      typeof data !== "object" ||
      Array.isArray(data)
    ) {
      return jsonResponse({
        ok: false,
        error: "Solicitud inválida"
      });
    }

    // 2. Validar token para cualquier tipo de subida.
    if (
      !API_TOKEN ||
      API_TOKEN === "PON_AQUI_TU_TOKEN_ACTUAL" ||
      data.token !== API_TOKEN
    ) {
      return jsonResponse({
        ok: false,
        error: "No autorizado"
      });
    }

    // SCAP tiene un contrato propio; nunca se deriva a las carpetas SIC/SIB.
    if (data.recordKind === "SCAP" || data.action === "uploadScap") {
      return uploadScap(data);
    }

    // 3. Derivar Excel y ZIP ANTES de exigir campos de fotos.
    if (data.action === "uploadSicExport") {
      return uploadSicExport(data);
    }

    // Las fotografías continúan con el flujo original.

    // 4. Validar campos obligatorios de las fotografías.
    const requiredFields = [
      "routeCode",
      "sibCode",
      "fileName",
      "base64"
    ];

    for (const field of requiredFields) {
      if (
        data[field] === undefined ||
        data[field] === null ||
        String(data[field]).trim() === ""
      ) {
        return jsonResponse({
          ok: false,
          error: "Falta " + field
        });
      }
    }

    if (
      typeof data.fileName !== "string" ||
      typeof data.base64 !== "string"
    ) {
      return jsonResponse({
        ok: false,
        error: "fileName y base64 deben ser texto"
      });
    }

    // 5. Normalizar ruta y códigos.
    const routeCode = normalizeFolderName(data.routeCode);
    const sibCode = normalizeSibCode(data.sibCode);

    let sicCode = null;

    if (
      data.sicCode !== undefined &&
      data.sicCode !== null &&
      String(data.sicCode).trim() !== ""
    ) {
      sicCode = normalizeSicCode(data.sicCode);
    }

    if (!/^SIB-\d{2}$/.test(sibCode)) {
      return jsonResponse({
        ok: false,
        error: "sibCode inválido: " + sibCode
      });
    }

    if (sicCode !== null && !isValidSicCode(sicCode)) {
      return jsonResponse({
        ok: false,
        error: "sicCode inválido: " + sicCode
      });
    }

    // 6. Preparar la imagen antes de tomar el bloqueo.
    const bytes = Utilities.base64Decode(data.base64);

    if (!bytes.length) {
      return jsonResponse({
        ok: false,
        error: "Imagen vacía"
      });
    }

    const blob = Utilities.newBlob(
      bytes,
      data.mimeType || "image/jpeg",
      data.fileName
    );

    // 7. Bloqueo compartido.
    // Evita crear carpetas y archivos duplicados por reintentos.
    lock = LockService.getScriptLock();
    acquired = lock.tryLock(30000);

    if (!acquired) {
      return jsonResponse({
        ok: false,
        retryable: true,
        error: "Servidor ocupado. Reintentar subida."
      });
    }

    // 8. Buscar o crear carpetas dentro del bloqueo.
    const rootFolder = DriveApp.getFolderById(ROOT_FOLDER_ID);

    const routeFolder = getOrCreateFolder(
      rootFolder,
      routeCode
    );

    const sibFolder = getOrCreateFolder(
      routeFolder,
      sibCode
    );

    // 9. Comprobar si esta foto ya se había subido.
    const existingFiles = sibFolder.getFilesByName(
      data.fileName
    );

    const alreadyExists = existingFiles.hasNext();

    const file = alreadyExists
      ? existingFiles.next()
      : sibFolder.createFile(blob);

    // 10. Responder a Android.
    return jsonResponse({
      ok: true,
      alreadyExists: alreadyExists,
      fileId: file.getId(),
      fileName: file.getName(),
      routeCode: routeCode,
      sibCode: sibCode,
      sicCode: sicCode,
      routeFolderId: routeFolder.getId(),
      sibFolderId: sibFolder.getId()
    });

  } catch (error) {
    return jsonResponse({
      ok: false,
      error: String(error)
    });

  } finally {
    if (acquired) {
      lock.releaseLock();
    }
  }
}


// ============================================================
// RECIBIR EXPORTACIÓN SIC: EXCEL O ZIP
//
// Se llama desde doPost DESPUÉS de validar el token.
// No requiere routeCode ni sibCode.
// ============================================================

function uploadSicExport(data) {
  let lock = null;
  let acquired = false;

  try {
    // 1. Validar nombre.
    const fileName =
      typeof data.fileName === "string"
        ? data.fileName.trim()
        : "";

    if (
      !fileName ||
      fileName.length > 180 ||
      /[\\/\x00-\x1F]/.test(fileName)
    ) {
      throw new Error("Nombre de archivo inválido");
    }

    if (!/^SIC[-_].+\.(xlsx|zip)$/i.test(fileName)) {
      throw new Error(
        "Se requiere un archivo que empiece por SIC- o SIC_ " +
        "y termine en .xlsx o .zip"
      );
    }

    // 2. Validar contenido.
    if (
      typeof data.base64 !== "string" ||
      !data.base64.trim()
    ) {
      throw new Error("Falta el contenido del archivo");
    }

    // Límite inicial elegido para esta solución.
    // No representa un límite oficial de Google.
    const maxBytes = 10 * 1024 * 1024;

    if (data.base64.length > 4 * Math.ceil(maxBytes / 3)) {
      throw new Error("El archivo supera el límite de 10 MB");
    }

    const bytes = Utilities.base64Decode(data.base64);

    if (!bytes.length || bytes.length > maxBytes) {
      throw new Error("Archivo vacío o mayor de 10 MB");
    }

    // 3. Preparar archivo sin convertirlo a Google Sheets.
    const mimeType = /\.zip$/i.test(fileName)
      ? "application/zip"
      : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    const blob = Utilities.newBlob(
      bytes,
      mimeType,
      fileName
    );

    // 4. Calcular huella del contenido.
    const digest = Utilities.computeDigest(
      Utilities.DigestAlgorithm.SHA_256,
      bytes
    );

    const hash = Utilities.base64EncodeWebSafe(digest);
    const marker = "sic-export-sha256:" + hash;

    // 5. Tomar el mismo bloqueo que utilizan las fotos.
    lock = LockService.getScriptLock();
    acquired = lock.tryLock(30000);

    if (!acquired) {
      return jsonResponse({
        ok: false,
        retryable: true,
        error: "Servidor ocupado. Reintentar subida."
      });
    }

    // 6. Buscar o crear carpeta de exportaciones.
    const root = DriveApp.getFolderById(ROOT_FOLDER_ID);

    const folder = getOrCreateFolder(
      root,
      "EXPORTACIONES_SIC"
    );

    // 7. Detectar reintentos y conflictos de nombre.
    const existing = folder.getFilesByName(fileName);

    let file;
    let alreadyExists = false;

    if (existing.hasNext()) {
      file = existing.next();

      if (existing.hasNext()) {
        throw new Error(
          "Existen varios archivos con ese nombre"
        );
      }

      // Comparar el contenido real permite recuperar incluso
      // una subida cuya respuesta se perdió.
      const previousHash = Utilities.base64EncodeWebSafe(
        Utilities.computeDigest(
          Utilities.DigestAlgorithm.SHA_256,
          file.getBlob().getBytes()
        )
      );

      if (previousHash !== hash) {
        throw new Error(
          "Ya existe otro contenido con ese nombre. " +
          "Genera una nueva versión con otro nombre."
        );
      }

      alreadyExists = true;

    } else {
      file = folder.createFile(blob);
      file.setDescription(marker);
    }

    // 8. Responder con el enlace del archivo.
    return jsonResponse({
      ok: true,
      alreadyExists: alreadyExists,
      fileId: file.getId(),
      fileName: file.getName(),
      fileUrl: file.getUrl(),
      folderId: folder.getId()
    });

  } catch (error) {
    return jsonResponse({
      ok: false,
      error: String(error)
    });

  } finally {
    if (acquired) {
      lock.releaseLock();
    }
  }
}


// ============================================================
// BUSCAR O CREAR CARPETA
// Solo llamar mientras se tiene el bloqueo.
// ============================================================

function getOrCreateFolder(parentFolder, folderName) {
  const folders = parentFolder.getFoldersByName(folderName);

  let selected = null;

  while (folders.hasNext()) {
    const candidate = folders.next();

    // Si existen carpetas duplicadas, elegir siempre
    // la de menor ID. No elimina ni mueve las demás.
    if (
      !selected ||
      candidate.getId() < selected.getId()
    ) {
      selected = candidate;
    }
  }

  return selected || parentFolder.createFolder(folderName);
}


// ============================================================
// NORMALIZAR NOMBRE DE RUTA
// ============================================================

function normalizeFolderName(value) {
  return String(value)
    .trim()
    .toUpperCase()
    .replace(/\//g, "-")
    .replace(/\\/g, "-");
}


// ============================================================
// NORMALIZAR CÓDIGOS
// Acepta: SIB-02, SIB02, 02, 2; SIC-23, SIC23, 23.
// Rechaza códigos malformados como SIC-23texto.
// ============================================================

function normalizeCode(value, prefix) {
  const text = String(value).trim().toUpperCase();

  const pattern = new RegExp(
    "^(?:" + prefix + "\\s*-?\\s*)?(\\d{1,2})$"
  );

  const match = text.match(pattern);

  if (!match) {
    return text;
  }

  return prefix + "-" + match[1].padStart(2, "0");
}


// SCAP v1: ZIP autocontenido (Excel + fotos marcadas + croquis + manifiesto).
// Conserva versiones. ACK solo después de guardar todos los archivos.
function uploadScap(data) {
  let lock = null, acquired = false;
  try {
    const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
    if (data.action !== "uploadScap" || data.recordKind !== "SCAP" || data.protocol !== 1 ||
        !uuid.test(data.scapInspectionId) || !Number.isSafeInteger(data.revision) || data.revision < 1 ||
        typeof data.bridgeCode !== "string" || typeof data.bridgeName !== "string" ||
        typeof data.sha256 !== "string" || !/^[a-f0-9]{64}$/.test(data.sha256) ||
        typeof data.base64 !== "string" || !data.base64.length) {
      return jsonResponse({ok: false, retryable: false, error: "Contrato SCAP inválido"});
    }
    const bytes = Utilities.base64Decode(data.base64);
    if (scapHash(bytes) !== data.sha256) return jsonResponse({ok: false, retryable: false, error: "Huella SCAP incorrecta"});
    const name = "SCAP_" + data.scapInspectionId + "_" + data.revision + ".zip";
    const zip = Utilities.newBlob(bytes, "application/zip", name);
    let entries;
    try { entries = Utilities.unzip(zip); } catch (_) {
      return jsonResponse({ok: false, retryable: false, error: "ZIP SCAP inválido"});
    }
    const names = entries.map(b => b.getName());
    if (new Set(names).size !== names.length || !names.includes("SCAP.xlsx") || !names.includes("manifest.json") ||
        names.some(n => !/^(SCAP\.xlsx|manifest\.json|fotos\/[a-f0-9-]+\.png|croquis\/[a-f0-9-]+\.png)$/.test(n))) {
      return jsonResponse({ok: false, retryable: false, error: "Contenido SCAP inválido"});
    }
    let manifest;
    try { manifest = JSON.parse(entries.find(b => b.getName() === "manifest.json").getDataAsString("UTF-8")); }
    catch (_) { return jsonResponse({ok: false, retryable: false, error: "Manifiesto SCAP inválido"}); }
    if (manifest.protocol !== 1 || manifest.scapInspectionId !== data.scapInspectionId || manifest.revision !== data.revision ||
        manifest.bridgeCode !== data.bridgeCode || !Array.isArray(manifest.files) ||
        manifest.files.length !== entries.length - 1 || new Set(manifest.files.map(f => f.path)).size !== manifest.files.length ||
        manifest.files.some(f => f.path === "manifest.json" || !names.includes(f.path) ||
          scapHash(entries.find(b => b.getName() === f.path).getBytes()) !== f.sha256)) {
      return jsonResponse({ok: false, retryable: false, error: "Archivos SCAP no coinciden con el manifiesto"});
    }
    lock = LockService.getScriptLock(); acquired = lock.tryLock(30000);
    if (!acquired) return jsonResponse({ok: false, retryable: true, error: "Servidor ocupado. Reintentar subida."});
    const root = getOrCreateFolder(DriveApp.getFolderById(ROOT_FOLDER_ID), "SCAP");
    const properties = PropertiesService.getScriptProperties();
    const inspectionKey = "scap.inspection." + data.scapInspectionId;
    const code = data.bridgeCode.trim();
    const codeKey = code ? "scap.bridge." + scapHash(code) : null;
    const previous = properties.getProperty(inspectionKey);
    const byCode = codeKey ? properties.getProperty(codeKey) : null;
    // Si un código ya identifica otra carpeta, no fusionar puentes silenciosamente.
    if (previous && byCode && previous !== byCode) {
      return jsonResponse({ok: false, retryable: false, error: "Código de puente asociado a otra carpeta. Revisar asociación SCAP."});
    }
    const bridge = previous || byCode ? DriveApp.getFolderById(previous || byCode) :
      getOrCreateFolder(root, "PUENTE_" + scapHash(code ? "code:" + code : "inspection:" + data.scapInspectionId));
    properties.setProperty(inspectionKey, bridge.getId());
    if (codeKey) properties.setProperty(codeKey, bridge.getId());
    const inspection = getOrCreateFolder(bridge, data.scapInspectionId);
    const version = getOrCreateFolder(inspection, String(data.revision) + "_" + data.sha256);
    const savedFiles = [];
    for (const blob of entries) {
      const path = blob.getName().split("/");
      const folder = path.length === 2 ? getOrCreateFolder(version, path[0]) : version;
      const saved = scapSaveExact(folder, blob.copyBlob().setName(path[path.length - 1]));
      savedFiles.push({path: blob.getName(), fileId: saved.file.getId(), folderId: folder.getId()});
    }
    const result = scapSaveExact(version, zip);
    return jsonResponse({ok: true, protocol: 1, recordKind: "SCAP", scapInspectionId: data.scapInspectionId,
      revision: data.revision, sha256: data.sha256, fileId: result.file.getId(), fileName: name,
      fileUrl: result.file.getUrl(), folderId: version.getId(), bridgeFolderId: bridge.getId(),
      alreadyExists: result.alreadyExists, fileCount: entries.length, files: savedFiles});
  } catch (_) {
    // No divulgar contenido, credenciales ni detalles internos de Google.
    return jsonResponse({ok: false, retryable: true, error: "No se pudo completar SCAP en Drive. Reintentar."});
  } finally { if (acquired) lock.releaseLock(); }
}

function scapHash(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, value)
    .map(b => (b & 255).toString(16).padStart(2, "0")).join("");
}

function scapSaveExact(folder, blob) {
  const existing = folder.getFilesByName(blob.getName());
  if (existing.hasNext()) {
    const file = existing.next();
    if (existing.hasNext() || scapHash(file.getBlob().getBytes()) !== scapHash(blob.getBytes())) throw Error("Conflicto SCAP");
    return {file: file, alreadyExists: true};
  }
  return {file: folder.createFile(blob), alreadyExists: false};
}

function normalizeSibCode(value) {
  return normalizeCode(value, "SIB");
}


function normalizeSicCode(value) {
  return normalizeCode(value, "SIC");
}


// ============================================================
// VALIDAR SIC
// ============================================================

function isValidSicCode(value) {
  return [
    "SIC-17",
    "SIC-18",
    "SIC-19",
    "SIC-20",
    "SIC-21",
    "SIC-22",
    "SIC-23"
  ].includes(value);
}


// ============================================================
// RESPUESTA JSON
// ============================================================

function jsonResponse(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}
