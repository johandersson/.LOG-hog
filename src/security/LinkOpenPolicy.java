package security;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Policy checks for opening local files from markdown links.
 */
public final class LinkOpenPolicy {
    private static final Set<String> HIGH_RISK_EXTENSIONS = Set.of(
        "exe", "bat", "cmd", "com", "msi", "ps1", "vbs", "js", "jar", "sh", "scr", "pif", "lnk", "url", "reg", "hta"
    );

    private LinkOpenPolicy() {}

    public static boolean isLikelyExecutable(Path file) {
        if (file == null) return false;
        String name = file.getFileName() != null ? file.getFileName().toString() : "";
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return false;
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return HIGH_RISK_EXTENSIONS.contains(ext);
    }

    public static boolean isSafeToOpen(Path file) {
        if (file == null) return false;
        try {
            Path real = file.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
            return java.nio.file.Files.exists(real)
                && java.nio.file.Files.isReadable(real)
                && java.nio.file.Files.isRegularFile(real);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Limit local link opening to expected user-controlled roots to avoid
     * browsing arbitrary system files through markdown content.
     */
    public static boolean isWithinAllowedRoots(Path file) {
        if (file == null) return false;
        try {
            Path real = file.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
            return AppPathPolicy.isWithinUserControlledRoots(real);
        } catch (Exception e) {
            return false;
        }
    }
}
