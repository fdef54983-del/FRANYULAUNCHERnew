<H1 align="center">FranyuLauncher</H1>

* FranyuLauncher is an Android launcher for Minecraft: Java Edition based on PojavLauncher and the MojoLauncher codebase.

## Building

Build the launcher with:

```bash
./gradlew :app_pojavlauncher:assembleFullDebug
```

## FranyuLauncher 1.2

This branch contains the 1.2 development line, including FranyuLauncher branding, resilient Minecraft downloads, session caching, deferred non-critical initialization, and ABI-aware release configuration.

## Known technical debt

The Java source namespace is still inherited from the upstream launcher (`git.artdeell.mojo` and `net.kdt.pojavlaunch`). It is intentionally not renamed in 1.2 because the project contains JNI/native integrations and generated/resource references that depend on the existing fully-qualified class and resource names. A complete namespace migration must be performed as a separate refactor with native/JNI validation rather than changing only Gradle's `applicationId`.

The Ely.by OAuth credentials must be registered for FranyuLauncher before production login is enabled. The launcher does not claim ownership of the upstream `mojolauncher2` OAuth client.

## Credits & third-party components

FranyuLauncher incorporates code and third-party components from the upstream PojavLauncher/MojoLauncher ecosystem. Their licenses and attribution requirements remain applicable to the corresponding components.

See the repository license files for the complete licensing information.
