import fs from 'node:fs/promises';
import { FileBlob, SpreadsheetFile } from '@oai/artifact-tool';
const root = 'C:/PROYECTOS/GBO_InventarioVial';
for (let sic = 17; sic <= 23; sic++) {
  const workbook = await SpreadsheetFile.importXlsx(await FileBlob.load(`${root}/app/build/sic-export-verification/SIC-${sic}.xlsx`));
  const end = sic === 17 ? 'T' : [20,22].includes(sic) ? 'N' : [18,19].includes(sic) ? 'M' : 'L';
  const preview = await workbook.render({sheetName:'PE-22B', range:`A5:${end}14`, scale:1.4, format:'png'});
  await fs.writeFile(`${root}/tmp/export-review/SIC-${sic}.png`, new Uint8Array(await preview.arrayBuffer()));
  console.log(`SIC-${sic}: render OK`);
}
