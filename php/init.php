<?php

/**
 * Autocarga del SDK para instalaciones sin Composer:
 *
 *   require_once '/ruta/a/kuida-php/init.php';
 *   $kuida = new \Kuida\KuidaClient('kd_live_…');
 */
spl_autoload_register(static function (string $class): void {
    $prefix = 'Kuida\\';
    if (strncmp($class, $prefix, strlen($prefix)) !== 0) {
        return;
    }
    $file = __DIR__ . '/src/' . str_replace('\\', '/', substr($class, strlen($prefix))) . '.php';
    if (is_file($file)) {
        require $file;
    }
});
