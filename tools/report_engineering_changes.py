"""Compare this delivery with the read-only snapshot taken before the engineering task."""
from pathlib import Path
import hashlib
import json
import subprocess

root = Path(__file__).resolve().parents[1]
baseline_file = root / 'tmp/engineering/baseline.json'
integration_plan = root / 'docs/INTEGRATION_SCAP_PLAN.json'
if baseline_file.exists():
    baseline = json.loads(baseline_file.read_text(encoding='utf-8'))
    reference = 'Estado del workspace anterior al cierre de ingeniería.'
elif integration_plan.exists():
    plan = json.loads(integration_plan.read_text(encoding='utf-8'))
    baseline = plan['officialTrackedHashes']
    reference = 'Base oficial anterior a la integración SCAP: ' + plan['officialHead'] + '.'
else:
    raise SystemExit('Falta una línea base: tmp/engineering/baseline.json o docs/INTEGRATION_SCAP_PLAN.json. No se inventa una comparación.')
existing_at_head = set(subprocess.run(['git','ls-tree','-r','--name-only','HEAD'],cwd=root,check=True,capture_output=True,text=True).stdout.splitlines())
def source_path(path):
    parts = Path(path).parts
    return not any(p in {'.git','.gradle','.kotlin','.idea','build','tmp','output','__pycache__'} for p in parts) and Path(path).name not in {'local.properties','drive.local.properties','server.local.properties'} and Path(path).suffix.lower() not in {'.apk','.zip','.lock','.pyc'}
paths = {path for path in baseline if source_path(path)}
for folder in ['app/src', 'app/schemas', 'docs', 'tools', 'gradle', 'google-apps-script']:
    paths.update(p.relative_to(root).as_posix() for p in (root/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
paths.update(['app/build.gradle.kts'])
changes = []
for path in sorted(paths):
    if path == 'docs/ENGINEERING_CHANGED_FILES.md':
        continue
    file = root / path
    current = hashlib.sha256(file.read_bytes()).hexdigest() if file.exists() else None
    old = baseline.get(path)
    if current != old:
        changes.append(('D' if current is None else 'A' if old is None and path not in existing_at_head else 'M', path))
text = '# Archivos del cierre de ingeniería\n\n'
text += reference + ' '
text += 'No incluye cachés, temporales, APK/ZIP ni configuración privada. A = añadido; M = modificado; D = eliminado.\n\n'
text += '| Estado | Archivo |\n|---|---|\n'
text += ''.join(f'| {status} | [{path}](../{path}) |\n' for status, path in changes)
text += '| A / generado | [docs/ENGINEERING_CHANGED_FILES.md](ENGINEERING_CHANGED_FILES.md) |\n\n'
text += 'Resumen funcional, limitaciones y pasos de tablet: [ENGINEERING_RELEASE.md](ENGINEERING_RELEASE.md). '
text += 'Verificación: [SCAP_VERIFICATION.json](SCAP_VERIFICATION.json) y [SCAP_EXCEL_VALIDATION.json](SCAP_EXCEL_VALIDATION.json).\n'
(root/'docs/ENGINEERING_CHANGED_FILES.md').write_text(text,encoding='utf-8')
print(json.dumps({'changedFiles':len(changes)+1,'report':'docs/ENGINEERING_CHANGED_FILES.md'}))
