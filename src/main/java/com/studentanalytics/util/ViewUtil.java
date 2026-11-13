package com.studentanalytics.util;

public class ViewUtil {

}
package com.studentanalytics.util;

/**
 * Small helpers used by the JSP views: HTML escaping and number formatting.
 */
public final class ViewUtil {

    private ViewUtil() {
        // utility class, no instances
    }

    /** Escapes user-entered text before it is written into HTML. */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Formats a percentage with two decimals, e.g. 83.50%. */
    public static String pct(double value) {
        return String.format("%.2f%%", value);
    }

    /** Formats marks without a trailing .0 for whole numbers. */
    public static String num(double value) {
        return value == Math.rint(value)
                ? String.format("%.0f", value)
                : String.format("%.1f", value);
    }

    /** CSS class for a grade badge, e.g. "A+" becomes "g-Ap". */
    public static String gradeClass(String grade) {
        return grade == null ? "" : "g-" + grade.replace("+", "p");
    }
}