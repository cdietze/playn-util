package de.cdietze.playn_util;

import com.google.common.collect.AbstractIterator;
import playn.core.Color;

import java.util.Iterator;

public class ColorUtils {
    /**
     * The golden ratio (≈ 1.61803)
     */
    private static final double PHI = (1 + Math.sqrt(5)) / 2;
    /**
     * The golden ratio conjugate (≈ 0.61803)
     */
    private static final double PHI_CON = (1 + Math.sqrt(5)) / 2 - 1;

    /**
     * Converts a HSV (hue, saturation, value) color into a RGB color. HSV are each in the range [0,1[
     *
     * @see <a href="https://en.wikipedia.org/wiki/HSL_and_HSV#Converting_to_RGB">This Wikipedia article</a>
     */
    public static int hsvToColor(float h, float s, float v) {
        int index = (int) (h * 6);
        float f = h * 6 - index;
        int p = (int) (256 * v * (1 - s));
        int q = (int) (256 * v * (1 - f * s));
        int t = (int) (256 * v * (1 - (1 - f) * s));
        int v2 = (int) (256 * v);
        switch (index) {
            case 0:
                return Color.rgb(v2, t, p);
            case 1:
                return Color.rgb(q, v2, p);
            case 2:
                return Color.rgb(p, v2, t);
            case 3:
                return Color.rgb(p, q, v2);
            case 4:
                return Color.rgb(t, p, v2);
            case 5:
                return Color.rgb(v2, p, q);
            default:
                throw new AssertionError("hsv index  out of range, index: " + index + ", h: " + h);
        }
    }

    /**
     * Returns an iterator for unique colors.
     * <p>
     * It modifies the hue value while saturation and value is kept constant.
     *
     * @see <a href="http://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/">This blog post</a>
     */
    public static Iterator<Integer> uniqueColorGenerator(final float h, final float s, final float v) {
        return new AbstractIterator<Integer>() {
            float c = h;

            @Override
            protected Integer computeNext() {
                c += PHI_CON;
                if (c > 1) c -= 1;
                return hsvToColor(c, s, v);
            }
        };
    }
}
