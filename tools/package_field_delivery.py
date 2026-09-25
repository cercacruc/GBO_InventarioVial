"""Package tested sources and debug APK without caches, local credentials or temporary files.
Run after: gradlew testDebugUnitTest assembleDebug. Uses Git read-only to audit protected code.
"""
from pathlib import Path
import hashlib
import json
import shutil
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "output"
OUT.mkdir(exist_ok=True)

def git(*args):
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, check=True).stdout

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def normalize(data):
    return data.replace(b"\r\n", b"\n")

def function(text, name):
    start = text.index("fun " + name + "(")
    brace = text.index("{", start)
    depth = 0
    for i in range(brace, len(text)):
        depth += (text[i] == "{") - (text[i] == "}")
        if depth == 0:
            return text[start:i+1]
    raise AssertionError("Unclosed function")

reports = sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"))
assert reports, "Run the unit tests first"
suites = []
for report in reports:
    r = ET.parse(report).getroot()
    suites.append({k:r.get(k) for k in ["name", "tests", "failures", "errors", "skipped"]})
assert all(int(s["failures"]) == 0 and int(s["errors"]) == 0 for s in suites), "Tests must pass"
apk = ROOT / "app/build/outputs/apk/debug/app-debug.apk"
assert apk.is_file()
sources = [p for p in (ROOT / "app/src").rglob("*") if p.is_file()]
last_source = max(p.stat().st_mtime for p in sources)
assert min(p.stat().st_mtime for p in reports) >= last_source, "Test results predate a source edit"
assert apk.stat().st_mtime >= last_source, "APK predates a source edit"

base = git("rev-parse", "HEAD").decode().strip()
main = ROOT / "app/src/main/java/com/tuempresa/inventariovial"
protected = [p for p in (main / "export").glob("*.kt") if p.name not in {"SicExportFormat.kt", "SicExcelWriter.kt"}] + list((main / "gis").glob("*.kt")) + [
    main / "DriveFolderRouter.kt", main / "SicExportScreen.kt", main / "camera/PhotoUtils.kt"]
checks = []
for p in protected:
    rel = p.relative_to(ROOT).as_posix()
    assert normalize(git("show", base + ":" + rel)) == normalize(p.read_bytes()), "Protected file changed: " + rel
    checks.append({"path":rel, "sha256":digest(p), "unchangedVsHead":True})
vm = main / "viewmodel/InventoryViewModel.kt"
old = normalize(git("show", base + ":" + vm.relative_to(ROOT).as_posix())).decode("utf-8")
new = vm.read_text(encoding="utf-8")
assert function(old,"buildDriveFileName") == function(new,"buildDriveFileName"), "Photo naming changed"
writer = main / "export/SicExcelWriter.kt"
writer_old = normalize(git("show", base + ":" + writer.relative_to(ROOT).as_posix())).decode("utf-8")
for name in ["write", "writeWorkbook", "sheetName", "col", "cell", "xml", "styles"]:
    if name == "styles":
        assert writer_old[writer_old.index("private fun styles()"):] == writer.read_text(encoding="utf-8")[writer.read_text(encoding="utf-8").index("private fun styles()"):]
    else:
        assert function(writer_old,name) == function(writer.read_text(encoding="utf-8"),name), "Existing SIC writer changed: " + name
with zipfile.ZipFile(apk) as z:
    assert z.read("assets/scap_template.xlsx") == (ROOT / "docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx").read_bytes()
    assert z.read("assets/scap_field_map.json") == (ROOT / "tools/scap_field_map.json").read_bytes()
baseline_path = ROOT / "tmp/engineering/baseline.json"
if baseline_path.exists():
    baseline = json.loads(baseline_path.read_text(encoding="utf-8"))
    for rel in ["app/src/main/java/com/tuempresa/inventariovial/validation/EndLocationPolicy.kt", "app/src/main/java/com/tuempresa/inventariovial/DriveSync.kt", "app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt"]:
        assert digest(ROOT / rel) == baseline[rel], "Field stability rule changed: " + rel

source_dirs = ["app/src", "app/schemas", "gradle", "docs", "tools", "google-apps-script"]
source_files = [".gitignore", "app/.gitignore", "app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts",
                "gradle.properties", "gradlew", "gradlew.bat", "drive.local.properties.example", "server.local.properties.example"]
source_files += [p.name for p in ROOT.glob("*.md")]
files = {ROOT / f for f in source_files if (ROOT / f).is_file()}
for directory in source_dirs:
    files.update(p for p in (ROOT / directory).rglob("*") if p.is_file() and not any(x in p.parts for x in ["__pycache__", "build", ".gradle", ".git"]))

tracked = set(git("ls-tree", "-r", "--name-only", base).decode().splitlines())
changed = []
for p in sorted(files):
    rel = p.relative_to(ROOT).as_posix()
    if rel.startswith("docs/SCAP_VERIFICATION") or rel == "docs/FIELD_CHANGED_FILES.md":
        continue
    status = "A" if rel not in tracked else ("M" if normalize(git("show", base + ":" + rel)) != normalize(p.read_bytes()) else "")
    if status:
        changed.append({"status":status, "path":rel})

verification = {
    "generatedAtUtc":datetime.now(timezone.utc).isoformat(), "comparisonBase":base,
    "commands":["gradlew.bat testDebugUnitTest", "gradlew.bat assembleDebug"],
    "tests":sum(int(s["tests"]) for s in suites), "failures":0, "errors":0, "suites":suites,
    "roomVersion":6, "scapExample":{"value":2.0700860438436695,"display":"2.070","classification":"REGULAR"},
    "catalogElements":112, "workbookSha256":digest(ROOT / "docs/reference/SCAP_PUENTE_AGUA_BLANCA.xlsx"),
    "protectedFiles":checks, "buildDriveFileNameUnchanged":True,
    "existingSicWriterFunctionsUnchanged":True, "runtimeTemplateAndFieldMapMatchSources":True,
    "engineeringRelease":"docs/ENGINEERING_RELEASE.md", "scapDriveUploadEnabled":False,
    "deviceTest":"Not run: adb devices -l returned no connected devices",
    "liveDriveTest":"Not run: local Drive configuration absent at verification", "changedFiles":changed,
    "debugApkSha256":digest(apk)
}
report_path = ROOT / "docs/SCAP_VERIFICATION.json"
report_path.write_text(json.dumps(verification, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
changes_path = ROOT / "docs/FIELD_CHANGED_FILES.md"
changes_path.write_text("# Archivos de la estabilización\n\nComparación de fuentes con el commit `"+base+"`. "
    "A = añadido; M = modificado. No incluye cachés ni configuración local. La comparación abarca SCAP, estabilidad y decisiones confirmadas de ingeniería; "
    "la estructura del módulo está en SCAP_IMPLEMENTATION.md.\n\n| Estado | Archivo |\n|---|---|\n" +
    "".join("| "+c["status"]+" | `"+c["path"]+"` |\n" for c in changed) +
    "| A / generado | `docs/SCAP_VERIFICATION.json` |\n| A / generado | `docs/FIELD_CHANGED_FILES.md` |\n\n"
    "Artefactos fuera del código: output/GBO_InventarioVial_campo.zip, output/GBO_InventarioVial_campo-debug.apk y output/DELIVERY.json. "
    "Los archivos generados que ya estuvieran versionados no se retiran del índice.\n", encoding="utf-8")
files.update([report_path, changes_path])
zip_path = OUT / "GBO_InventarioVial_campo.zip"
apk_path = OUT / "GBO_InventarioVial_campo-debug.apk"
shutil.copy2(apk, apk_path)
with zipfile.ZipFile(zip_path,"w",zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):
        rel=p.relative_to(ROOT).as_posix()
        assert p.name not in {"drive.local.properties", "server.local.properties", "local.properties"}
        z.write(p, "GBO_InventarioVial/" + rel)
with zipfile.ZipFile(zip_path) as z:
    assert z.testzip() is None
    assert not any("/tmp/" in name or "/build/" in name or "/.gradle/" in name for name in z.namelist())
delivery={"zip":{"name":zip_path.name,"sha256":digest(zip_path),"bytes":zip_path.stat().st_size},
          "apk":{"name":apk_path.name,"sha256":digest(apk_path),"bytes":apk_path.stat().st_size},
          "files":len(files),"tests":verification["tests"],"failures":0}
(OUT / "DELIVERY.json").write_text(json.dumps(delivery,indent=2)+"\n",encoding="utf-8")
print(json.dumps(delivery,indent=2))
