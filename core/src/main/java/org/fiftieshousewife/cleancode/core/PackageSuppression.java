package org.fiftieshousewife.cleancode.core;

import org.fiftieshousewife.cleancode.annotations.HeuristicCode;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class PackageSuppression {

    private static final String OTHER_FILE = "otherFile";

    private final Map<String, Set<HeuristicCode>> codesByPackagePath;

    private PackageSuppression(final Map<String, Set<HeuristicCode>> codesByPackagePath) {
        this.codesByPackagePath = codesByPackagePath;
    }

    public static PackageSuppression empty() {
        return new PackageSuppression(Map.of());
    }

    public static PackageSuppression of(final Map<String, ? extends Iterable<String>> rawConfig) {
        final Map<String, Set<HeuristicCode>> resolved = new HashMap<>();
        rawConfig.forEach((pkg, codes) -> {
            final Set<HeuristicCode> codeSet = EnumSet.noneOf(HeuristicCode.class);
            codes.forEach(c -> codeSet.add(HeuristicCode.valueOf(c)));
            resolved.put(packageToPath(pkg), codeSet);
        });
        return new PackageSuppression(resolved);
    }

    public boolean suppresses(final Finding finding) {
        if (codesByPackagePath.isEmpty()) {
            return false;
        }
        for (final Map.Entry<String, Set<HeuristicCode>> entry : codesByPackagePath.entrySet()) {
            if (!entry.getValue().contains(finding.code())) {
                continue;
            }
            if (matchesPath(finding.sourceFile(), entry.getKey())) {
                return true;
            }
            if (matchesPath(otherFile(finding), entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static String otherFile(final Finding finding) {
        final Map<String, String> metadata = finding.metadata();
        return metadata == null ? null : metadata.get(OTHER_FILE);
    }

    private static boolean matchesPath(final String path, final String packagePath) {
        return path != null && path.contains(packagePath + "/");
    }

    private static String packageToPath(final String packageName) {
        return packageName.replace('.', '/');
    }
}
