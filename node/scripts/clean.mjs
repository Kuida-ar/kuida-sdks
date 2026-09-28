// Borra los directorios que se pasan como argumento (multiplataforma, sin rimraf).
import { rmSync } from "node:fs";
for (const dir of process.argv.slice(2)) rmSync(dir, { recursive: true, force: true });
