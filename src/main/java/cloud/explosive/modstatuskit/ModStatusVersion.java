package cloud.explosive.modstatuskit;

import java.util.Objects;

/**
 * Normalized public version plus optional diagnostic build metadata.
 */
public final class ModStatusVersion {
    private final String version;
    private final String build;

    private ModStatusVersion(String version, String build) {
        this.version = ModStatusStrings.requireText(version, "version");
        this.build = ModStatusStrings.optionalText(build);
    }

    public static ModStatusVersion of(String version) {
        return of(version, null);
    }

    public static ModStatusVersion of(String version, String build) {
        String normalizedVersion = ModStatusStrings.requireText(version, "version");
        String explicitBuild = ModStatusStrings.optionalText(build);
        int buildSeparator = normalizedVersion.indexOf('+');
        if (buildSeparator >= 0) {
            String baseVersion = normalizedVersion.substring(0, buildSeparator);
            String inlineBuild = normalizedVersion.substring(buildSeparator + 1);
            if (!baseVersion.trim().isEmpty() && !inlineBuild.trim().isEmpty()) {
                return new ModStatusVersion(baseVersion, explicitBuild == null ? inlineBuild : explicitBuild);
            }
        }
        return new ModStatusVersion(normalizedVersion, explicitBuild);
    }

    public String version() {
        return version;
    }

    public String build() {
        return build;
    }

    public String toPayloadString() {
        return build == null ? version : version + "+" + build;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ModStatusVersion)) {
            return false;
        }
        ModStatusVersion that = (ModStatusVersion) other;
        return version.equals(that.version) && Objects.equals(build, that.build);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, build);
    }
}
