# Built-ins de matematica y fecha

## Objetivo de aprendizaje
- Usar helpers numericos y temporales en ejercicios practicos.

## Sintaxis clave
```lucia
let r: float = sqrt(25);
let n: int = random(1, 10);
let d: date = today();
let dt: datetime = now();
```

## Ejemplos
- Numericos: `sqrt`, `sin`, `cos`, `tan`, `log`, `pow`, `round`, `floor`, `ceil`.
- Constructores: `date(...)`, `datetime(...)`.
- Tiempo actual: `today()`, `now()`.

## Errores comunes
- Enviar tipos incompatibles a funciones numericas.
- Suponer que `random` excluye limites (es inclusivo).

## Practica sugerida
- Crea un script que imprima timestamp y evale expresiones trigonometricas.

## Relacionados
- functions-and-builtins
- types-and-collections
