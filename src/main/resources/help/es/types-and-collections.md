# Tipos y colecciones

## Objetivo de aprendizaje
- Aplicar tipos de Lucia y estructuras de coleccion en codigo real.

## Sintaxis clave
```lucia
let age: int = 20;
let price: float = 12.5;
let name: string = "Ana";
let active: bool = true;
let tags: list<string> = ["a", "b"];
let user: dict<string, any> = {"name": "Ana", "age": 20};
```

## Ejemplos
- Acceso en listas: `tags[0]`.
- Acceso en diccionarios: `user["name"]`.
- Valores temporales: `date(...)`, `datetime(...)`.

## Errores comunes
- Asignar tipos incompatibles.
- Usar indices o claves invalidas.

## Practica sugerida
- Modela un registro de estudiante con `dict<string, any>` y lista de notas.

## Relacionados
- functions-and-builtins
- methods-and-chaining
