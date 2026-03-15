# Clases, herencia e imports

## Objetivo de aprendizaje
- Estructurar programas Lucia con clases e imports multiarchivo.

## Sintaxis clave
```lucia
class Animal {
    let name: string;
    constructor(name: string) { this.name = name; }
}

class Dog extends Animal {
    func bark() { print(this.name); }
}
```

## Ejemplos
- Import: `import "./models/pet.lucia";`.
- Herencia simple con `extends`.
- Uso de `this` para estado de instancia.

## Errores comunes
- Clase/funcion desconocida por import faltante.
- Imports ciclicos entre archivos.

## Practica sugerida
- Divide un modelo de dominio en 2-3 archivos con imports y clases.

## Relacionados
- declarations-let-const
- diagnostics
