# Flujo de ejecutar y compilar

## Objetivo de aprendizaje
- Ejecutar y compilar codigo Lucia de forma consistente en Python y JavaScript.

## Sintaxis clave
```bash
lucia run examples/00_features.lucia
lucia compile examples/00_features.lucia --target js --save
```

## Ejemplos
- `--target <python|javascript|js>` elige target.
- `--save` guarda salida generada.
- `--out <path>` ruta personalizada (requiere `--save`).
- `--debug` muestra traceback completo.

## Errores comunes
- Usar `--out` sin `--save`.
- Ejecutar target JS sin `node` en PATH.

## Practica sugerida
- Ejecuta el mismo archivo en Python y JavaScript y compara resultados.

## Relacionados
- diagnostics
- getting-started
