# Types and collections

## Learning objective
- Apply Lucia types and collection structures in real code.

## Key syntax
```lucia
let age: int = 20;
let price: float = 12.5;
let name: string = "Ana";
let active: bool = true;
let tags: list<string> = ["a", "b"];
let user: dict<string, any> = {"name": "Ana", "age": 20};
```

## Examples
- Access list values: `tags[0]`.
- Access dictionary values: `user["name"]`.
- Use `date(2026, 3, 15)` and `datetime(...)` for temporal values.

## Common mistakes
- Assigning incompatible values to typed variables.
- Using invalid index/key expressions.

## Suggested practice
- Model a student record with `dict<string, any>` and a list of grades.

## Related
- functions-and-builtins
- methods-and-chaining
