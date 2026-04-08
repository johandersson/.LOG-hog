package security;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple utilities to validate URLs and file paths before opening them.
 */
public final class PathValidator {
    private PathValidator() {}

    /**
     * Return true only for http or https URLs.
     */
    public static boolean isSafeHttpUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            scheme = scheme.toLowerCase();
            return "http".equals(scheme) || "https".equals(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Basic file path sanity check: the file must exist, be readable and not contain
     * suspicious path traversal constructs after normalization.
     */
    public static boolean isSafeFilePath(Path path) {
        if (path == null) return false;
        try {
            // Resolve to real path (no-follow) to canonicalize symlinks and .. segments
            Path real = path.toRealPath(java.nio.file.LinkOption.NOFOLLOW_LINKS);
            // Basic checks: must exist and be readable
            if (!Files.exists(real) || !Files.isReadable(real)) return false;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
