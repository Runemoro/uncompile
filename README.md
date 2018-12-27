# uncompile

## Why make a new Java decompiler?

FernFlower and Procyon are currently broken. The code they generate doesn't recompile due to many issues with types (especially when unckecked casts are involved). I spent a lot of time trying to fix Procyon's type inference system, but eventually decided it would be better to restart from scratch.

Here are some differences between Procyon and Uncompile:
 - Bytecode is converted to Java AST as soon as possible and all transformations are done on the Java AST rather than bytecode or intermediate representation. This makes type inference easier to implement and debug.
 - Type inference is done in two steps. The first step only infers primitive and raw types and uses casts for all method parameters. The second step tries to generify the code and remove unnecessary casts. This way, if generic inference is broken, it can be turned off for certain methods to make the code compilable again (but a bit less nicer). The type information in the first step will also be useful to determine lower and upper bounds for unknown types to be inferred in the second step.

## What still needs to be done?

### Java Features

 - [ ] Inner classes
 - [ ] Anonymous classes
 - [ ] Lambda functions
 - [ ] Generics

### Improving control flow
 - [ ] `while (true) { if (...) break; ... }` -> `while (...) { ... }`
 - [ ] `else`
 - [ ] Nested `if`s to `if` + `return`
 - [ ] `for` loops
 - [ ] `for (... : ...)` loops
 - [ ] Conditional expressions
