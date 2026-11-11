# Historical generated-stub tests

These test sources were originally executed against synthetic Minecraft/Fabric
classes supplied by a temporary external helper. They are retained as historical
regression coverage, not deleted to make a real game build pass.

They intentionally use constructors/helpers that do not exist in the actual game.
Consequently they must not be compiled as Gradle's real `test` source set or
reported as actual Minecraft validation. They were moved out of `src/test/java`
when independent native builds were introduced.

`gradle build -Pminecraft_version=1.20.1` (or `1.21.11`) now runs
`verifyNativeCompatibility` from `src/compatTest/java` against actual Loom-resolved
game classes as part of `check`. That audit is structural/policy testing, NOT a
physical-device benchmark, full Mixin transformation or real gameplay test.
