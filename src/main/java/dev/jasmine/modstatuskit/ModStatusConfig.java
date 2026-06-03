package dev.jasmine.modstatuskit;

import java.util.Objects;

/**
 * Static configuration supplied by the consuming mod.
 */
public final class ModStatusConfig {
    private final String modId;
    private final String displayName;
    private final String clientVersion;
    private final String updateUrl;
    private final String payloadNamespace;
    private final String payloadPath;
    private final ModStatusMessages messages;

    private ModStatusConfig(Builder builder) {
        this.modId = requireText(builder.modId, "modId");
        this.displayName = requireText(builder.displayName, "displayName");
        this.clientVersion = requireText(builder.clientVersion, "clientVersion");
        this.updateUrl = optionalText(builder.updateUrl);
        this.payloadNamespace = requireText(builder.payloadNamespace, "payloadNamespace");
        this.payloadPath = requireText(builder.payloadPath, "payloadPath");
        this.messages = builder.messages == null ? ModStatusMessages.defaults() : builder.messages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    public String clientVersion() {
        return clientVersion;
    }

    public String updateUrl() {
        return updateUrl;
    }

    public String payloadNamespace() {
        return payloadNamespace;
    }

    public String payloadPath() {
        return payloadPath;
    }

    public String payloadChannel() {
        return payloadNamespace + ":" + payloadPath;
    }

    public ModStatusMessages messages() {
        return messages;
    }

    public static final class Builder {
        private String modId;
        private String displayName;
        private String clientVersion;
        private String updateUrl;
        private String payloadNamespace;
        private String payloadPath;
        private ModStatusMessages messages;

        public Builder modId(String modId) {
            this.modId = modId;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder clientVersion(String clientVersion) {
            this.clientVersion = clientVersion;
            return this;
        }

        public Builder updateUrl(String updateUrl) {
            this.updateUrl = updateUrl;
            return this;
        }

        public Builder payloadChannel(String namespace, String path) {
            this.payloadNamespace = namespace;
            this.payloadPath = path;
            return this;
        }

        public Builder messages(ModStatusMessages messages) {
            this.messages = Objects.requireNonNull(messages, "messages");
            return this;
        }

        public ModStatusConfig build() {
            return new ModStatusConfig(this);
        }
    }

    private static String requireText(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
