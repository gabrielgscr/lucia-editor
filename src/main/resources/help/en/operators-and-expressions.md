# Operators and expressions

## Learning objective
- Use Lucia operators to build clear and correct expressions.

## Key syntax
```lucia
let x: int = 5;
x += 2;
let ok: bool = (x > 3) && true;
let level: string = (x >= 7) ? "high" : "low";
let nick: string = providedName ?? "guest";
```

## Examples
- Arithmetic: `+ - * / %`.
- Assignment: `= += -= *= /= %= ++ --`.
- Comparison: `== != < <= > >=`.
- Logical: `&& || ! not`.
- Conditional: ternary `? :` and null coalescing `??`.

## Common mistakes
- Confusing assignment `=` with equality `==`.
- Misusing precedence in combined logical expressions.

## Suggested practice
- Build a score classifier using arithmetic and ternary expressions.

## Related
- syntax-control-flow
- string-interpolation
