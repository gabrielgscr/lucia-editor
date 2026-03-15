# Lucia language overview

## Learning objective
- Understand the complete feature map of Lucia and where each concept fits.

## Key syntax
```lucia
let x: int = 10;
const title: string = "Lucia";
if (x > 0) { print(title); }
```

## Examples
- Control flow: `if`, `else if`, `else`, `switch/case/default`, `while`, `for`, `do/while`, `for...in`.
- Declarations: `let`, `const`.
- Types: `int`, `float`, `bool`, `string`, `date`, `datetime`, `void`, `any`.
- Collections: `list<T>`, `dict<K, V>`, literals, indexing.
- OOP: `class`, `extends`, `constructor`, properties, methods, `this`.
- Modules: `import "./module.lucia";`.
- Expressions: arithmetic, logical, comparison, assignment, ternary, null coalescing `??`.
- Strings: interpolation with `${expr}`.

## Common mistakes
- Mixing concepts before mastering declarations and types.
- Ignoring diagnostics while learning new syntax.

## Suggested practice
- Build a mini app that uses imports, classes, lists, conditionals, and interpolation.

## Related
- syntax-control-flow
- types-and-collections
- oop-and-modules
