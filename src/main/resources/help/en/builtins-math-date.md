# Math, date, and datetime built-ins

## Learning objective
- Use Lucia numeric and temporal helpers for practical tasks.

## Key syntax
```lucia
let r: float = sqrt(25);
let n: int = random(1, 10);
let d: date = today();
let dt: datetime = now();
```

## Examples
- Numeric: `sqrt`, `sin`, `cos`, `tan`, `log`, `pow`, `round`, `floor`, `ceil`.
- Date/time constructors: `date(...)`, `datetime(...)`.
- Current values: `today()`, `now()`.

## Common mistakes
- Passing incompatible types to numeric functions.
- Assuming random range is exclusive (it is inclusive).

## Suggested practice
- Build a script that prints a timestamp and evaluates trig expressions.

## Related
- functions-and-builtins
- types-and-collections
