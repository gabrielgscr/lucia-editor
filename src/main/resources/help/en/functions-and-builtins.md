# Functions and built-ins

## Learning objective
- Define functions and use built-ins with correct arguments and return values.

## Key syntax
```lucia
func add(a: int, b: int) -> int {
    return a + b;
}

print(add(2, 3));
```

## Examples
- Core built-ins: `print`, `len`, `input`, `str`, `int`, `float`, `bool`, `type_of`.
- Utility built-ins: `abs`, `min`, `max`, `pow`, `round`, `floor`, `ceil`, `random`.

## Common mistakes
- Wrong argument count.
- Return type mismatch.
- Calling non-callable values.

## Suggested practice
- Create helper functions for grade average and pass/fail decision.

## Related
- builtins-math-date
- diagnostics
