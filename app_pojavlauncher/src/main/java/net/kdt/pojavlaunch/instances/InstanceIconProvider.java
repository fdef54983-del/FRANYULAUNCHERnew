package net.kdt.pojavlaunch.instances;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import git.artdeell.mojo.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class InstanceIconProvider {
    public static final String FALLBACK_ICON_NAME = "default";
    private static final Map<Integer, Drawable> sIconCache = new HashMap<>();
    private static final Map<String, Drawable> sStaticIconCache = new HashMap<>();
    private static final Map<String, Integer> sStaticIcons = new HashMap<>();

    static {
        sStaticIcons.put("default", R.mipmap.ic_launcher);
        sStaticIcons.put("fabric", R.drawable.ic_fabric);
        sStaticIcons.put("quilt", R.drawable.ic_quilt);
        sStaticIcons.put("forge", R.drawable.ic_forge);
        sStaticIcons.put("neoforge", R.drawable.ic_neoforge);
    }

    public static @NonNull Drawable fetchIcon(Resources resources, @NonNull Instance instance) {
        int identityHashCode = System.identityHashCode(instance);
        Drawable cachedIcon = sIconCache.get(identityHashCode);
        if (cachedIcon != null) {
            cachedIcon.clearColorFilter();
            return cachedIcon;
        }
        Drawable instanceIcon = fetchInstanceFileIcon(resources, identityHashCode, instance.getInstanceIconLocation());
        if (instanceIcon != null) return instanceIcon;
        return fetchStaticIcon(resources, identityHashCode, instance.icon);
    }

    public static void dropIcon(@NonNull Instance key) {
        sIconCache.remove(System.identityHashCode(key));
    }

    private static Drawable fetchInstanceFileIcon(Resources resources, int identityHash, File iconLocation) {
        if (!iconLocation.isFile() || !iconLocation.canRead()) return null;
        Bitmap iconBitmap = BitmapFactory.decodeFile(iconLocation.getAbsolutePath());
        if (iconBitmap == null) return null;
        Drawable iconDrawable = new BitmapDrawable(resources, iconBitmap);
        iconDrawable.clearColorFilter();
        sIconCache.put(identityHash, iconDrawable);
        return iconDrawable;
    }

    private static Drawable fetchStaticIcon(Resources resources, int identityHash, String icon) {
        Drawable staticIcon = sStaticIconCache.get(icon);
        if (staticIcon == null) {
            if (icon != null) staticIcon = getStaticIcon(resources, icon);
            if (staticIcon == null) staticIcon = fetchFallbackIcon(resources);
            sStaticIconCache.put(icon, staticIcon);
        }
        staticIcon.clearColorFilter();
        sIconCache.put(identityHash, staticIcon);
        return staticIcon;
    }

    private static @NonNull Drawable fetchFallbackIcon(Resources resources) {
        Drawable fallbackIcon = sStaticIconCache.get(FALLBACK_ICON_NAME);
        if (fallbackIcon == null) {
            fallbackIcon = Objects.requireNonNull(getStaticIcon(resources, FALLBACK_ICON_NAME));
            sStaticIconCache.put(FALLBACK_ICON_NAME, fallbackIcon);
        }
        fallbackIcon.clearColorFilter();
        return fallbackIcon;
    }

    private static Drawable getStaticIcon(Resources resources, @NonNull String icon) {
        int staticIconResource = getStaticIconResource(icon);
        if (staticIconResource == -1) return null;
        Drawable drawable = ResourcesCompat.getDrawable(resources, staticIconResource, null);
        if (drawable != null) {
            drawable.clearColorFilter();
            if (FALLBACK_ICON_NAME.equals(icon)) {
                drawable.setTint(resources.getColor(R.color.emerald_accent));
            }
        }
        return drawable;
    }

    private static int getStaticIconResource(String icon) {
        Integer iconResource = sStaticIcons.get(icon);
        if (iconResource == null) return -1;
        return iconResource;
    }

    public static boolean hasStaticIcon(String name) {
        return sStaticIcons.containsKey(name);
    }
}
