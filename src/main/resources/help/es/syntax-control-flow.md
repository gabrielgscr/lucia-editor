# Sintaxis y flujo de control

## Objetivo de aprendizaje
- Escribir y combinar estructuras de control de Lucia correctamente.

## Sintaxis clave
```lucia
if (score >= 90) {
    print("A");
} else if (score >= 70) {
    print("B");
} else {
    print("C");
}
```

## Ejemplos
```lucia
switch (day) {
    case 1: print("Lun");
    case 2: print("Mar");
    default: print("Otro");
}

for (let i: int = 0; i < 3; i++) { print(i); }
while (ready) { break; }
do { continue; } while (false);
```

## Errores comunes
- Usar `break` dentro de `switch` (en Lucia, `break` es solo para ciclos).
- Usar `return` fuera de funciones.

## Practica sugerida
- Implementa un menu con `switch`, ciclos y `try/catch/finally`.

## Relacionados
- operators-and-expressions
- diagnostics
