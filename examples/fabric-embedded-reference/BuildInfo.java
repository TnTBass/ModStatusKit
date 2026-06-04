package com.example.yourmod;

/**
 * Example generated build metadata.
 *
 * In a consuming mod, generate this class during Gradle/CI and include the
 * generated source directory in compileJava and sourcesJar. See the root
 * README "Build Metadata Stamping" section for a copy/paste Gradle snippet.
 */
public final class BuildInfo {
    // CHANGE: generate this value from -PbuildNumber, CI, or git.
    public static final String GIT_COMMIT = "abc1234";

    private BuildInfo() {
    }
}
