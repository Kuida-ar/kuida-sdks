// Marca el formato de cada salida para que Node y TypeScript resuelvan bien ESM y CJS.
import { writeFileSync } from "node:fs";
writeFileSync("dist/esm/package.json", JSON.stringify({ type: "module" }, null, 2) + "\n");
writeFileSync("dist/cjs/package.json", JSON.stringify({ type: "commonjs" }, null, 2) + "\n");
