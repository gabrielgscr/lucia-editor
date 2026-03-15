# String, list, dict methods and chaining

## Learning objective
- Apply method calls and chaining to produce compact readable code.

## Key syntax
```lucia
let text: string = "  Lucia Lang  ";
let parts: list<string> = text.trim().lower().split(" ");
```

## Examples
- String methods: `upper`, `lower`, `trim`, `split`, `join`, `contains`, `starts_with`, `ends_with`, `replace`, `repeat`, `substring`.
- List methods: `append`, `pop`, `contains`, `sort`, `reverse`, `indexOf`, `slice`.
- Dict methods: `keys`, `values`, `contains_key`, `get`.

## Common mistakes
- Calling methods that do not belong to the receiver type.
- Forgetting method argument contracts.

## Suggested practice
- Normalize a list of names and build a CSV output line.

## Related
- types-and-collections
- string-interpolation
