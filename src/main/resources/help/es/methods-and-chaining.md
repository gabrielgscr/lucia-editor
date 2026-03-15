# Metodos y chaining en string/list/dict

## Objetivo de aprendizaje
- Aplicar metodos y encadenamiento para codigo mas legible.

## Sintaxis clave
```lucia
let text: string = "  Lucia Lang  ";
let parts: list<string> = text.trim().lower().split(" ");
```

## Ejemplos
- String: `upper`, `lower`, `trim`, `split`, `join`, `contains`, `starts_with`, `ends_with`, `replace`, `repeat`, `substring`.
- List: `append`, `pop`, `contains`, `sort`, `reverse`, `indexOf`, `slice`.
- Dict: `keys`, `values`, `contains_key`, `get`.

## Errores comunes
- Llamar metodos que no pertenecen al tipo receptor.
- Ignorar contrato de argumentos.

## Practica sugerida
- Normaliza una lista de nombres y genera una linea CSV.

## Relacionados
- types-and-collections
- string-interpolation
