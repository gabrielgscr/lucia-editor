# Run and compile workflows

## Learning objective
- Execute Lucia code consistently in Python and JavaScript targets.

## Key syntax
```bash
lucia run examples/00_features.lucia
lucia compile examples/00_features.lucia --target js --save
```

## Examples
- `--target <python|javascript|js>` selects output/runtime target.
- `--save` writes generated output.
- `--out <path>` custom output path (requires `--save`).
- `--debug` prints full traceback.

## Common mistakes
- Using `--out` without `--save`.
- Running JS target without `node` in PATH.

## Suggested practice
- Run same Lucia file in Python and JavaScript, compare outputs.

## Related
- diagnostics
- getting-started
