# Operadores y expresiones

## Objetivo de aprendizaje
- Usar operadores de Lucia para expresiones claras y correctas.

## Sintaxis clave
```lucia
let x: int = 5;
x += 2;
let ok: bool = (x > 3) && true;
let level: string = (x >= 7) ? "alto" : "bajo";
let nick: string = providedName ?? "invitado";
```

## Ejemplos
- Aritmeticos: `+ - * / %`.
- Asignacion: `= += -= *= /= %= ++ --`.
- Comparacion: `== != < <= > >=`.
- Logicos: `&& || ! not`.
- Condicionales: ternario `? :` y null coalescing `??`.

## Errores comunes
- Confundir `=` con `==`.
- Ignorar precedencia en expresiones logicas complejas.

## Practica sugerida
- Construye un clasificador de puntaje con operadores y ternario.

## Relacionados
- syntax-control-flow
- string-interpolation
