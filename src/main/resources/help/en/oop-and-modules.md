# Classes, inheritance, and modules

## Learning objective
- Structure Lucia programs with classes and multi-file imports.

## Key syntax
```lucia
class Animal {
    let name: string;
    constructor(name: string) { this.name = name; }
}

class Dog extends Animal {
    func bark() { print(this.name); }
}
```

## Examples
- Import syntax: `import "./models/pet.lucia";`.
- Single inheritance with `extends`.
- Use `this` for instance state.

## Common mistakes
- Unknown class/function due to missing imports.
- Circular imports between files.

## Suggested practice
- Split a small domain model into 2-3 files using imports and classes.

## Related
- declarations-let-const
- diagnostics
