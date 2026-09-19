package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import net.kdt.pojavlaunch.CustomControlsActivity;
import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.contracts.OpenDocumentWithExtension;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceIconProvider;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class MainMenuFragment extends Fragment {
    public static final String TAG = "MainMenuFragment";

    private mcVersionSpinner mVersionSpinner;
    private LinearLayout mRecentInstances;
    private AlertDialog mUpdateDialog;
    private UpdateChecker mUpdateChecker;
    private UpdateChecker.Update mLatestUpdate;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data) -> {
                if (data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Button news = view.findViewById(R.id.news_button);
        Button discord = view.findViewById(R.id.discord_button);
        Button controls = view.findViewById(R.id.custom_control_button);
        Button installJar = view.findViewById(R.id.install_jar_button);
        Button shareLogs = view.findViewById(R.id.share_logs_button);
        Button openDirectory = view.findViewById(R.id.open_files_button);
        ImageButton editProfile = view.findViewById(R.id.edit_profile_button);
        ImageButton railHome = view.findViewById(R.id.rail_home);
        Button play = view.findViewById(R.id.play_button);

        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        mRecentInstances = view.findViewById(R.id.recent_instances_container);
        mUpdateChecker = new UpdateChecker(requireContext());

        news.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        discord.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.discord_invite)));
        controls.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        installJar.setOnClickListener(v -> runInstallerWithConfirmation());
        editProfile.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        play.setOnClickListener(v -> launchSelectedInstance());
        shareLogs.setOnClickListener(v -> shareLog(requireContext()));
        openDirectory.setOnClickListener(v -> openGameDirectory(v.getContext()));

        railHome.setSelected(true);
        railHome.setOnClickListener(v -> v.setSelected(true));
        view.findViewById(R.id.rail_instances).setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        view.findViewById(R.id.rail_mods).setOnClickListener(v -> runInstallerWithConfirmation());
        view.findViewById(R.id.rail_settings).setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, "SETTINGS_FRAGMENT", null));

        news.setOnLongClickListener(v -> {
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });

        refreshSelectedInstance(view);
        renderRecentInstances();
        checkForUpdate();
    }

    private void launchSelectedInstance() {
        Instance selected = InstanceManager.getSelectedListedInstance();
        if (selected == null) {
            Toast.makeText(requireContext(), R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }
        selected.lastPlayedAt = System.currentTimeMillis();
        selected.maybeWrite();
        ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        renderRecentInstances();
    }

    private void refreshSelectedInstance(View root) {
        Instance instance = InstanceManager.getSelectedListedInstance();
        TextView name = root.findViewById(R.id.active_instance_name);
        TextView details = root.findViewById(R.id.active_instance_details);
        TextView mods = root.findViewById(R.id.active_instance_mods);
        if (instance == null) return;

        String instanceName = Tools.validOrNullString(instance.name);
        if (instanceName == null) instanceName = safeVersion(instance.versionId);
        name.setText(instanceName);
        details.setText(safeVersion(instance.versionId) + " • " + detectModLoader(instance));
        mods.setText("Mods: " + countMods(instance));
        android.widget.ImageView icon = root.findViewById(R.id.active_instance_icon);
        android.graphics.drawable.Drawable instanceIcon = InstanceIconProvider.fetchIcon(getResources(), instance);
        if (instanceIcon != null) icon.setImageDrawable(instanceIcon);
    }

    private String safeVersion(String version) {
        return version == null || version.trim().isEmpty() ? "Unknown" : version;
    }

    private String detectModLoader(Instance instance) {
        if (instance.installer != null) {
            String name = instance.installer.getClass().getSimpleName().toLowerCase();
            if (name.contains("neoforge")) return "NeoForge";
            if (name.contains("forge")) return "Forge";
            if (name.contains("fabric")) return "Fabric";
            if (name.contains("quilt")) return "Quilt";
        }
        File mods = new File(instance.getGameDirectory(), "mods");
        if (mods.isDirectory()) {
            File[] files = mods.listFiles();
            if (files != null) {
                for (File file : files) {
                    String n = file.getName().toLowerCase();
                    if (n.contains("neoforge")) return "NeoForge";
                    if (n.contains("fabric")) return "Fabric";
                    if (n.contains("forge")) return "Forge";
                    if (n.contains("quilt")) return "Quilt";
                }
            }
        }
        return "Vanilla";
    }

    private int countMods(Instance instance) {
        File mods = new File(instance.getGameDirectory(), "mods");
        File[] files = mods.isDirectory() ? mods.listFiles((dir, name) ->
                name != null && (name.toLowerCase().endsWith(".jar") || name.toLowerCase().endsWith(".zip"))) : null;
        return files == null ? 0 : files.length;
    }

    private void renderRecentInstances() {
        if (mRecentInstances == null) return;
        mRecentInstances.removeAllViews();
        List<Instance> instances = new ArrayList<>(InstanceManager.getImmutableInstanceList());
        Collections.sort(instances, Comparator.comparingLong((Instance i) -> i.lastPlayedAt).reversed());
        int shown = Math.min(5, instances.size());
        for (int index = 0; index < shown; index++) {
            Instance instance = instances.get(index);
            TextView item = new TextView(requireContext());
            item.setMinHeight(68);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            item.setPadding(16, 8, 16, 8);
            item.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));
            item.setTextSize(14);
            String name = Tools.validOrNullString(instance.name);
            if (name == null) name = safeVersion(instance.versionId);
            String when = instance.lastPlayedAt <= 0 ? "Never played"
                    : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(instance.lastPlayedAt));
            item.setText(name + "\n" + when);
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    InstanceIconProvider.fetchIcon(getResources(), instance), null, null, null);
            item.setCompoundDrawablePadding(14);
            item.setBackgroundResource(R.drawable.recent_instance_bg);
            item.setOnClickListener(v -> {
                InstanceManager.setSelectedInstance(instance);
                mVersionSpinner.reloadProfiles();
                refreshSelectedInstance(requireView());
                renderRecentInstances();
            });
            mRecentInstances.addView(item);
        }
    }

    private void checkForUpdate() {
        mUpdateChecker.check(update -> {
            if (!isAdded() || update == null || mUpdateDialog != null) return;
            mLatestUpdate = update;
            showUpdateDialog(update);
        });
    }

    private void showUpdateDialog(UpdateChecker.Update update) {
        if (!isAdded()) return;

        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(padding, padding, padding, padding);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(22, 25, 29));
        background.setCornerRadius(22 * getResources().getDisplayMetrics().density);
        root.setBackground(background);

        TextView title = new TextView(requireContext());
        title.setText("Nueva actualización");
        title.setTextColor(Color.rgb(80, 220, 150));
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView version = new TextView(requireContext());
        version.setText("FranyuLauncher " + update.version);
        version.setTextColor(Color.WHITE);
        version.setTextSize(16);
        version.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        version.setPadding(0, 6, 0, 12);
        root.addView(version, new LinearLayout.LayoutParams(-1, -2));

        TextView label = new TextView(requireContext());
        label.setText("Novedades");
        label.setTextColor(Color.rgb(210, 215, 220));
        label.setTextSize(13);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(label, new LinearLayout.LayoutParams(-1, -2));

        ScrollView scroll = new ScrollView(requireContext());
        TextView body = new TextView(requireContext());
        body.setText(update.body == null ? "" : update.body);
        body.setTextColor(Color.rgb(190, 195, 200));
        body.setTextSize(14);
        body.setLineSpacing(0, 1.12f);
        body.setPadding(0, 7, 0, 7);
        scroll.addView(body);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, (int) (280 * getResources().getDisplayMetrics().density));
        scrollParams.topMargin = 4;
        scrollParams.bottomMargin = 8;
        root.addView(scroll, scrollParams);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .setNegativeButton("Omitir para después", (d, which) -> {
                    mUpdateChecker.skipVersion(update.version);
                    mLatestUpdate = null;
                })
                .setPositiveButton("Instalar", (d, which) -> {
                    mUpdateChecker.install(update, requireActivity());
                    mLatestUpdate = null;
                })
                .create();

        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positive != null) positive.setTextColor(Color.rgb(80, 220, 150));
            Button negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negative != null) negative.setTextColor(Color.rgb(170, 175, 180));
        });
        dialog.setOnDismissListener(d -> mUpdateDialog = null);
        mUpdateDialog = dialog;
        dialog.show();
    }

    private void openGameDirectory(Context context) {
        Instance selected = InstanceManager.getSelectedListedInstance();
        if (selected == null) {
            Toast.makeText(context, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return;
        }
        File gameDirectory = selected.getGameDirectory();
        if (FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        } else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (mUpdateDialog != null) {
            mUpdateDialog.dismiss();
            mUpdateDialog = null;
        }
        if (mUpdateChecker != null) {
            mUpdateChecker.close();
            mUpdateChecker = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        if (mUpdateChecker != null) mUpdateChecker.resumePendingInstall(requireActivity());
        if (getView() != null) {
            refreshSelectedInstance(getView());
            renderRecentInstances();
            checkForUpdate();
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        if (mUpdateChecker != null) mUpdateChecker.close();
        super.onDestroyView();
    }
}
