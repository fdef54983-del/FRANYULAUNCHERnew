package net.kdt.pojavlaunch;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import net.kdt.pojavlaunch.fragments.UpdateChecker;

/**
 * Blocks the launcher UI briefly while the public GitHub release is checked.
 * The launcher continues normally when there is no newer release or the
 * network request fails. When a newer release exists, the user gets a single
 * floating update panel with the release notes and explicit actions.
 */
public final class UpdateStartupGate implements AutoCloseable {
    private static final int EMERALD = Color.rgb(31, 174, 118);
    private static final int DARK = Color.rgb(10, 18, 14);
    private static final int PANEL = Color.rgb(20, 31, 25);
    private static final int TEXT = Color.rgb(242, 247, 244);
    private static final int MUTED = Color.rgb(170, 184, 176);

    private final Activity activity;
    private final UpdateChecker checker;
    private FrameLayout overlay;
    private boolean finished;
    private boolean closed;

    public UpdateStartupGate(Activity activity) {
        this.activity = activity;
        this.checker = new UpdateChecker(activity);
    }

    public void start(Runnable continueToLauncher) {
        if (closed || activity.isFinishing()) return;
        showLoading();
        checker.check(update -> {
            if (closed || activity.isFinishing()) return;
            if (update == null) {
                finish(continueToLauncher);
            } else {
                showUpdate(update, continueToLauncher);
            }
        });
    }

    private void showLoading() {
        overlay = new FrameLayout(activity);
        overlay.setBackgroundColor(DARK);
        overlay.setClickable(true);
        overlay.setFocusable(true);

        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(28);
        box.setPadding(padding, padding, padding, padding);

        TextView logo = text("F", 52, EMERALD, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        box.addView(logo, new LinearLayout.LayoutParams(-1, dp(70)));

        TextView title = text("FranyuLauncher", 22, TEXT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        box.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView status = text("Buscando actualizaciones...", 14, MUTED, Typeface.NORMAL);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(8);
        box.addView(status, statusParams);

        ProgressBar progress = new ProgressBar(activity);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        progressParams.topMargin = dp(22);
        box.addView(progress, progressParams);

        FrameLayout.LayoutParams boxParams = new FrameLayout.LayoutParams(dp(310), -2, Gravity.CENTER);
        overlay.addView(box, boxParams);
        addOverlay();
    }

    private void showUpdate(UpdateChecker.Update update, Runnable continueToLauncher) {
        if (overlay == null) showLoading();
        overlay.removeAllViews();

        View dim = new View(activity);
        dim.setBackgroundColor(Color.argb(145, 0, 0, 0));
        overlay.addView(dim, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(22), dp(22), dp(22), dp(18));
        GradientDrawable background = new GradientDrawable();
        background.setColor(PANEL);
        background.setCornerRadius(dp(24));
        background.setStroke(dp(1), Color.rgb(44, 90, 67));
        panel.setBackground(background);

        TextView title = text("Nueva actualización", 22, TEXT, Typeface.BOLD);
        panel.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView version = text("FranyuLauncher " + update.version, 15, EMERALD, Typeface.BOLD);
        LinearLayout.LayoutParams versionParams = new LinearLayout.LayoutParams(-1, -2);
        versionParams.topMargin = dp(5);
        panel.addView(version, versionParams);

        TextView label = text("Novedades", 13, MUTED, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(18);
        panel.addView(label, labelParams);

        android.widget.ScrollView notesScroll = new android.widget.ScrollView(activity);
        TextView notes = text(cleanBody(update.body), 14, TEXT, Typeface.NORMAL);
        notes.setLineSpacing(0, 1.12f);
        notes.setPadding(0, dp(7), 0, dp(7));
        notesScroll.addView(notes);
        LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(-1, dp(220));
        notesParams.topMargin = dp(4);
        panel.addView(notesScroll, notesParams);

        LinearLayout actions = new LinearLayout(activity);
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        actions.setOrientation(LinearLayout.HORIZONTAL);

        Button skip = new Button(activity);
        skip.setText("Omitir");
        skip.setTextColor(MUTED);
        skip.setAllCaps(false);
        skip.setMinHeight(dp(48));
        skip.setOnClickListener(v -> {
            checker.skipVersion(update.version);
            finish(continueToLauncher);
        });

        Button install = new Button(activity);
        install.setText("Instalar");
        install.setTextColor(Color.WHITE);
        install.setAllCaps(false);
        install.setMinHeight(dp(48));
        GradientDrawable installBg = new GradientDrawable();
        installBg.setColor(EMERALD);
        installBg.setCornerRadius(dp(14));
        install.setBackground(installBg);
        install.setOnClickListener(v -> {
            install.setEnabled(false);
            skip.setEnabled(false);
            install.setText("Descargando...");
            checker.install(update, activity);
        });

        actions.addView(skip, new LinearLayout.LayoutParams(dp(100), dp(52)));
        LinearLayout.LayoutParams installParams = new LinearLayout.LayoutParams(dp(120), dp(52));
        installParams.leftMargin = dp(8);
        actions.addView(install, installParams);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, dp(58));
        actionsParams.topMargin = dp(8);
        panel.addView(actions, actionsParams);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER);
        panelParams.leftMargin = dp(20);
        panelParams.rightMargin = dp(20);
        overlay.addView(panel, panelParams);
    }

    private String cleanBody(String body) {
        if (body == null || body.trim().isEmpty()) return "Esta actualización no incluye notas adicionales.";
        return body.trim();
    }

    private TextView text(String value, float size, int color, int style) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private void addOverlay() {
        ViewGroup content = activity.findViewById(android.R.id.content);
        if (content != null && overlay != null && overlay.getParent() == null) {
            content.addView(overlay, new ViewGroup.LayoutParams(-1, -1));
        }
    }

    private void finish(Runnable continueToLauncher) {
        if (finished || closed) return;
        finished = true;
        removeOverlay();
        if (!activity.isFinishing() && continueToLauncher != null) continueToLauncher.run();
    }

    private void removeOverlay() {
        if (overlay != null) {
            ViewGroup parent = (ViewGroup) overlay.getParent();
            if (parent != null) parent.removeView(overlay);
            overlay = null;
        }
    }

    private int dp(int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    @Override
    public void close() {
        closed = true;
        checker.close();
        removeOverlay();
    }
}
