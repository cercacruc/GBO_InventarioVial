// Sustituir el contenido de Code.gs y actualizar la implementación existente.
// Conserva la estructura original: RAÍZ / RUTA / SIB / FOTO.
const ROOT_FOLDER_ID = "1LT2GR1GhZOBlHlClApoBiqSh7oFoPv9R";
// Usa exactamente el mismo token que ya utiliza Android. No publiques el token.
const API_TOKEN = "PON_AQUI_TU_TOKEN_ACTUAL";

function doGet() {
  return jsonResponse({ok: true, message: "Inventario Vial API funcionando",
    version: "SIC-17 a SIC-23 · bloqueo de subidas v2"});
}

function doPost(e) {
  let lock = null;
  let acquired = false;
  try {
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({ok: false, error: "Solicitud vacía"});
    }
    const data = JSON.parse(e.postData.contents);
    if (!data || typeof data !== "object" || Array.isArray(data)) {
      return jsonResponse({ok: false, error: "Solicitud inválida"});
    }
    if (!API_TOKEN || API_TOKEN === "PON_AQUI_TU_TOKEN_ACTUAL") {
      return jsonResponse({ok: false, error: "No autorizado"});
    }
    if (data.token !== API_TOKEN) {
      return jsonResponse({ok: false, error: "No autorizado"});
    }
    for (const field of ["routeCode", "sibCode", "fileName", "base64"]) {
      if (data[field] === undefined || data[field] === null || String(data[field]).trim() === "") {
        return jsonResponse({ok: false, error: "Falta " + field});
      }
    }
    if (typeof data.fileName !== "string" || typeof data.base64 !== "string") {
      return jsonResponse({ok: false, error: "fileName y base64 deben ser texto"});
    }
    const routeCode = normalizeFolderName(data.routeCode);
    const sibCode = normalizeSibCode(data.sibCode);
    const sicCode = data.sicCode === undefined || data.sicCode === null || String(data.sicCode).trim() === ""
      ? null : normalizeSicCode(data.sicCode);
    if (!/^SIB-\d{2}$/.test(sibCode)) return jsonResponse({ok: false, error: "sibCode inválido: " + sibCode});
    if (sicCode !== null && !isValidSicCode(sicCode)) return jsonResponse({ok: false, error: "sicCode inválido: " + sicCode});

    // Decodificar antes del bloqueo evita ocuparlo con datos inválidos.
    const bytes = Utilities.base64Decode(data.base64);
    if (!bytes.length) return jsonResponse({ok: false, error: "Imagen vacía"});
    const blob = Utilities.newBlob(bytes, data.mimeType || "image/jpeg", data.fileName);

    // Compartido entre TODAS las solicitudes de este proyecto de Apps Script,
    // incluso si proceden de tablets distintas. Un bloqueo de usuario no basta.
    lock = LockService.getScriptLock();
    acquired = lock.tryLock(30000);
    if (!acquired) {
      // El Android actual ya reintenta respuestas ok:false distintas de No autorizado.
      return jsonResponse({ok: false, retryable: true, error: "Servidor ocupado. Reintentar subida."});
    }

    // Toda la secuencia buscar/crear queda protegida, incluido el archivo.
    const rootFolder = DriveApp.getFolderById(ROOT_FOLDER_ID);
    const routeFolder = getOrCreateFolder(rootFolder, routeCode);
    const sibFolder = getOrCreateFolder(routeFolder, sibCode);
    const files = sibFolder.getFilesByName(data.fileName);
    const alreadyExists = files.hasNext();
    const file = alreadyExists ? files.next() : sibFolder.createFile(blob);
    return jsonResponse({
      ok: true, alreadyExists: alreadyExists, fileId: file.getId(), fileName: file.getName(),
      routeCode: routeCode, sibCode: sibCode, sicCode: sicCode,
      routeFolderId: routeFolder.getId(), sibFolderId: sibFolder.getId()
    });
  } catch (error) {
    return jsonResponse({ok: false, error: String(error)});
  } finally {
    // Se libera también al devolver un archivo existente o ante errores de Drive.
    if (acquired) lock.releaseLock();
  }
}

// Invocar únicamente desde la sección protegida por getScriptLock().
function getOrCreateFolder(parentFolder, folderName) {
  const folders = parentFolder.getFoldersByName(folderName);
  let selected = null;
  while (folders.hasNext()) {
    const candidate = folders.next();
    // Si ya hay duplicados, elección estable, independiente del orden del iterador.
    // No mueve ni borra fotos/carpetas existentes.
    if (!selected || candidate.getId() < selected.getId()) selected = candidate;
  }
  return selected || parentFolder.createFolder(folderName);
}

function normalizeFolderName(value) {
  return String(value).trim().toUpperCase().replace(/\//g, "-").replace(/\\/g, "-");
}

function normalizeCode(value, prefix) {
  const text = String(value).trim().toUpperCase();
  const match = text.match(new RegExp("^(?:" + prefix + "\\s*-?\\s*)?(\\d{1,2})$"));
  return match ? prefix + "-" + match[1].padStart(2, "0") : text;
}

function normalizeSibCode(value) { return normalizeCode(value, "SIB"); }
function normalizeSicCode(value) { return normalizeCode(value, "SIC"); }
function isValidSicCode(value) { return ["SIC-17", "SIC-18", "SIC-19", "SIC-20", "SIC-21", "SIC-22", "SIC-23"].includes(value); }
function jsonResponse(data) {
  return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}
