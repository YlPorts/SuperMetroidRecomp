package com.ylports.supermetroid;

/** Small section-aware update: preserves other keys, comments and old saves. */
final class IniSettings {
    static String set(String text, String section, String key, String value) {
        String[] lines = text.split("\n", -1);
        StringBuilder result = new StringBuilder();
        boolean in = false, foundSection = false, written = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                if (in && !written) { result.append(key).append('=').append(value).append('\n'); written = true; }
                in = trimmed.equalsIgnoreCase("[" + section + "]");
                foundSection |= in;
            }
            int equal = trimmed.indexOf('=');
            if (in && equal > 0 && trimmed.substring(0, equal).trim().equalsIgnoreCase(key)) {
                if (!written) result.append(key).append('=').append(value).append('\n');
                written = true;
            } else if (!line.isEmpty()) result.append(line).append('\n');
        }
        if (!foundSection) result.append('[').append(section).append("]\n");
        if (!written) result.append(key).append('=').append(value).append('\n');
        return result.toString();
    }
}
