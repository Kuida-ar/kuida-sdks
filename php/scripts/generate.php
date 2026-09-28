<?php

/**
 * Genera los modelos de src/Model/ a partir del OpenAPI de Kuida.
 *
 *   php scripts/generate.php            # lee ../openapi/kuida-v1.json
 *   php scripts/generate.php ruta.json  # otro archivo
 *
 * Produce una clase por cada objeto que devuelve la API (respuestas de los
 * endpoints y cuerpos de los webhooks), con PHPDoc `@property-read` tipados,
 * constantes para los valores de enum, y `ObjectTypes.php` con la versión de la
 * API, el mapa `object` → clase y los campos de solo fecha.
 *
 * La salida se commitea. Borra y reescribe todo src/Model/.
 */

set_error_handler(function ($severity, $message, $file, $line) {
    throw new ErrorException($message, 0, $severity, $file, $line);
});

$root = dirname(__DIR__);
$specPath = isset($argv[1]) ? $argv[1] : $root . '/../openapi/kuida-v1.json';
$outDir = $root . '/src/Model';

$raw = file_get_contents($specPath);
if ($raw === false) {
    fwrite(STDERR, "No se pudo leer $specPath\n");
    exit(1);
}
$spec = json_decode($raw, true);
if (!is_array($spec)) {
    fwrite(STDERR, "$specPath no es JSON válido\n");
    exit(1);
}
$schemas = $spec['components']['schemas'];

/**
 * @param array<string, mixed> $schema
 */
function refName(array $schema): ?string
{
    if (isset($schema['$ref']) && strpos($schema['$ref'], '#/components/schemas/') === 0) {
        return substr($schema['$ref'], strlen('#/components/schemas/'));
    }

    return null;
}

/**
 * @param array<string, mixed> $schema
 */
function isListSchema(array $schema): bool
{
    return isset($schema['properties']['object']['const']) && $schema['properties']['object']['const'] === 'list';
}

/**
 * Junta los $ref de un esquema, recursivamente.
 *
 * @param array<string, mixed> $schema
 * @param array<string, array<string, mixed>> $schemas
 * @param array<string, bool> $seen
 */
function collectRefs(array $schema, array $schemas, array &$seen): void
{
    $name = refName($schema);
    if ($name !== null) {
        if (isset($seen[$name])) {
            return;
        }
        $seen[$name] = true;
        collectRefs($schemas[$name], $schemas, $seen);

        return;
    }
    foreach (['anyOf', 'oneOf', 'allOf'] as $k) {
        if (isset($schema[$k])) {
            foreach ($schema[$k] as $sub) {
                collectRefs($sub, $schemas, $seen);
            }
        }
    }
    if (isset($schema['items'])) {
        collectRefs($schema['items'], $schemas, $seen);
    }
    if (isset($schema['properties'])) {
        foreach ($schema['properties'] as $sub) {
            collectRefs($sub, $schemas, $seen);
        }
    }
}

// Objetos que devuelven los endpoints y los webhooks.
$fromPaths = [];
foreach ($spec['paths'] as $ops) {
    foreach ($ops as $method => $op) {
        if (!is_array($op) || !isset($op['responses'])) {
            continue;
        }
        foreach ($op['responses'] as $code => $response) {
            if (substr((string) $code, 0, 1) !== '2' || !isset($response['content']['application/json']['schema'])) {
                continue;
            }
            collectRefs($response['content']['application/json']['schema'], $schemas, $fromPaths);
        }
    }
}
$fromWebhooks = [];
foreach (isset($spec['webhooks']) ? $spec['webhooks'] : [] as $hook) {
    foreach ($hook as $op) {
        if (isset($op['requestBody']['content']['application/json']['schema'])) {
            collectRefs($op['requestBody']['content']['application/json']['schema'], $schemas, $fromWebhooks);
        }
    }
}

$classes = [];
foreach (array_keys($fromPaths + $fromWebhooks) as $name) {
    $schema = $schemas[$name];
    if ($name === 'Error' || isListSchema($schema) || !isset($schema['properties'])) {
        continue;
    }
    $classes[$name] = $schema;
}
ksort($classes);

/**
 * @param array<string, mixed> $schema
 * @param array<string, array<string, mixed>> $classes
 * @param array<string, array<string, mixed>> $schemas
 */
function phpType(array $schema, array $classes, array $schemas): string
{
    $name = refName($schema);
    if ($name !== null) {
        if (isset($classes[$name])) {
            return $name;
        }
        if (isListSchema($schemas[$name])) {
            return '\\Kuida\\Collection';
        }

        return phpType($schemas[$name], $classes, $schemas);
    }
    foreach (['anyOf', 'oneOf'] as $k) {
        if (isset($schema[$k])) {
            $types = [];
            foreach ($schema[$k] as $sub) {
                foreach (explode('|', phpType($sub, $classes, $schemas)) as $t) {
                    $types[$t] = true;
                }
            }

            return implode('|', array_keys($types));
        }
    }
    $type = isset($schema['type']) ? $schema['type'] : null;
    if ($type === null && array_key_exists('const', $schema)) {
        $type = gettype($schema['const']) === 'integer' ? 'integer' : 'string';
    }
    if (is_array($type)) {
        $types = [];
        foreach ($type as $t) {
            $sub = $schema;
            $sub['type'] = $t;
            $types[] = phpType($sub, $classes, $schemas);
        }

        return implode('|', array_unique($types));
    }
    switch ($type) {
        case 'string':
            return 'string';
        case 'integer':
            return 'int';
        case 'number':
            return 'float';
        case 'boolean':
            return 'bool';
        case 'null':
            return 'null';
        case 'array':
            $item = isset($schema['items']) ? phpType($schema['items'], $classes, $schemas) : 'mixed';

            return strpos($item, '|') !== false ? '(' . $item . ')[]' : $item . '[]';
        case 'object':
            return '\\Kuida\\KuidaObject';
        default:
            return 'mixed';
    }
}

/**
 * Clase de los objetos anidados en una propiedad, si es un modelo conocido.
 *
 * @param array<string, mixed> $schema
 * @param array<string, array<string, mixed>> $classes
 */
function nestedClass(array $schema, array $classes): ?string
{
    $name = refName($schema);
    if ($name === null && isset($schema['items'])) {
        $name = refName($schema['items']);
    }

    return $name !== null && isset($classes[$name]) ? $name : null;
}

function oneLine(string $text): string
{
    return trim((string) preg_replace('/\s+/', ' ', str_replace('*/', '*\\/', $text)));
}

function constName(string $prop, string $value): string
{
    $p = strtoupper((string) preg_replace('/([a-z0-9])([A-Z])/', '$1_$2', $prop));
    $v = strtoupper(trim((string) preg_replace('/[^A-Za-z0-9]+/', '_', $value), '_'));

    return $p . '_' . ($v === '' ? 'ALL' : $v);
}

function export(string $value): string
{
    return "'" . str_replace(["\\", "'"], ["\\\\", "\\'"], $value) . "'";
}

$header = "<?php\n\n// Generado por scripts/generate.php desde openapi/kuida-v1.json. No editar a mano.\n\nnamespace Kuida\\Model;\n\n";

if (is_dir($outDir)) {
    $old = glob($outDir . '/*.php');
    foreach ($old === false ? [] : $old as $file) {
        unlink($file);
    }
} else {
    mkdir($outDir, 0777, true);
}

$mapping = [];
foreach ($classes as $name => $schema) {
    $required = isset($schema['required']) ? $schema['required'] : [];
    $doc = [];
    $summary = isset($schema['description']) ? oneLine($schema['description']) : null;
    $objectName = isset($schema['properties']['object']['const']) ? $schema['properties']['object']['const'] : null;
    $doc[] = $summary !== null ? $summary : 'Objeto `' . ($objectName !== null ? $objectName : $name) . '` de la API de Kuida.';
    $doc[] = '';
    $consts = [];
    $nested = [];
    foreach ($schema['properties'] as $prop => $propSchema) {
        $type = phpType($propSchema, $classes, $schemas);
        if (!in_array($prop, $required, true) && strpos($type, 'null') === false) {
            $type .= '|null';
        }
        $desc = isset($propSchema['description']) ? oneLine($propSchema['description']) : '';
        $enum = isset($propSchema['enum']) ? $propSchema['enum'] : null;
        if ($enum === null && isset($propSchema['anyOf'])) {
            foreach ($propSchema['anyOf'] as $sub) {
                if (isset($sub['enum'])) {
                    $enum = $sub['enum'];
                }
            }
        }
        if (is_array($enum)) {
            $values = array_values(array_filter($enum, 'is_string'));
            $desc = trim($desc . ' Valores: `' . implode('`, `', $values) . '`.');
            foreach ($values as $value) {
                $consts[constName($prop, $value)] = $value;
            }
        }
        $fmt = isset($propSchema['format']) ? $propSchema['format'] : null;
        if ($fmt === null && isset($propSchema['anyOf'])) {
            foreach ($propSchema['anyOf'] as $sub) {
                if (isset($sub['format'])) {
                    $fmt = $sub['format'];
                }
            }
        }
        if ($fmt === 'date-time' && strpos($desc, 'ISO') === false) {
            $desc = trim($desc . ' Fecha y hora ISO 8601.');
        }
        if (!in_array($prop, $required, true)) {
            $desc = trim($desc . ' Puede no venir.');
        }
        $doc[] = '@property-read ' . $type . ' $' . $prop . ($desc !== '' ? ' ' . $desc : '');
        $nestedName = nestedClass($propSchema, $classes);
        if ($nestedName !== null) {
            $nested[$prop] = $nestedName;
        }
    }

    $body = '';
    if ($objectName !== null) {
        $body .= "    /** Valor del campo `object`. */\n    const OBJECT_NAME = " . export($objectName) . ";\n\n";
    }
    foreach ($consts as $c => $v) {
        $body .= '    const ' . $c . ' = ' . export($v) . ";\n";
    }
    if ($consts !== []) {
        $body .= "\n";
    }
    if ($nested !== []) {
        $body .= "    const NESTED_TYPES = [\n";
        foreach ($nested as $prop => $cls) {
            $body .= '        ' . export($prop) . ' => ' . $cls . "::class,\n";
        }
        $body .= "    ];\n\n";
    }
    $body = rtrim($body, "\n");

    $php = $header . "/**\n";
    foreach ($doc as $line) {
        $php .= rtrim(' * ' . $line) . "\n";
    }
    $php .= " */\nclass " . $name . " extends \\Kuida\\KuidaObject\n{\n" . ($body !== '' ? $body . "\n" : '') . "}\n";
    file_put_contents($outDir . '/' . $name . '.php', $php);

    // `object` → clase, solo para lo que devuelven los endpoints (los webhooks
    // se construyen explícitamente con WebhookEvent).
    if ($objectName !== null && isset($fromPaths[$name]) && !isset($mapping[$objectName])) {
        $mapping[$objectName] = $name;
    }
}
ksort($mapping);

// Campos de solo fecha (`format: date`) en cualquier esquema.
$dateOnly = [];
$walk = function (array $schema) use (&$walk, &$dateOnly) {
    if (isset($schema['properties'])) {
        foreach ($schema['properties'] as $prop => $sub) {
            if (isset($sub['format']) && $sub['format'] === 'date') {
                $dateOnly[$prop] = true;
            }
            $walk($sub);
        }
    }
    foreach (['anyOf', 'oneOf', 'allOf'] as $k) {
        if (isset($schema[$k])) {
            foreach ($schema[$k] as $sub) {
                $walk($sub);
            }
        }
    }
    if (isset($schema['items'])) {
        $walk($schema['items']);
    }
};
foreach ($schemas as $schema) {
    $walk($schema);
}
ksort($dateOnly);

$php = $header . "/**\n * Datos del OpenAPI que usa el SDK en tiempo de ejecución.\n */\nfinal class ObjectTypes\n{\n";
$php .= "    /** Versión de la API (`info.version` del OpenAPI). */\n";
$php .= '    const API_VERSION = ' . export($spec['info']['version']) . ";\n\n";
$php .= "    /**\n     * Clase de cada valor de `object`.\n     *\n     * @var array<string, class-string<\\Kuida\\KuidaObject>>\n     */\n";
$php .= "    const MAPPING = [\n";
foreach ($mapping as $object => $cls) {
    $php .= '        ' . export($object) . ' => ' . $cls . "::class,\n";
}
$php .= "    ];\n\n";
$php .= "    /**\n     * Campos `format: date` (AAAA-MM-DD): un `DateTimeInterface` se manda sin hora.\n     *\n     * @var string[]\n     */\n";
$php .= "    const DATE_ONLY_FIELDS = [\n";
foreach (array_keys($dateOnly) as $field) {
    $php .= '        ' . export($field) . ",\n";
}
$php .= "    ];\n}\n";
file_put_contents($outDir . '/ObjectTypes.php', $php);

echo 'Generados ' . count($classes) . ' modelos + ObjectTypes en src/Model/' . PHP_EOL;
