# Declarations with let and const

## Learning objective
- Declare mutable and immutable values correctly.

## Key syntax
```lucia
let total: int = 0;
const pi: float = 3.1416;
```

## Examples
- Use `let` for values that change in loops or state updates.
- Use `const` for fixed values such as config constants.

## Common mistakes
- Redeclaring a symbol in the same scope.
- Reassigning a `const` variable.

## Suggested practice
- Refactor a script to replace magic numbers with `const` values.

## Related
- types-and-collections
- diagnostics
