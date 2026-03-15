# Syntax and control flow

## Learning objective
- Write and combine Lucia control structures safely.

## Key syntax
```lucia
if (score >= 90) {
    print("A");
} else if (score >= 70) {
    print("B");
} else {
    print("C");
}
```

## Examples
```lucia
switch (day) {
    case 1: print("Mon");
    case 2: print("Tue");
    default: print("Other");
}

for (let i: int = 0; i < 3; i++) { print(i); }
while (ready) { break; }
do { continue; } while (false);
```

## Common mistakes
- Using `break` inside `switch` (in Lucia, `break` is for loops only).
- Using `return` outside functions.

## Suggested practice
- Implement a menu program using `switch`, loops, and `try/catch/finally`.

## Related
- operators-and-expressions
- diagnostics
