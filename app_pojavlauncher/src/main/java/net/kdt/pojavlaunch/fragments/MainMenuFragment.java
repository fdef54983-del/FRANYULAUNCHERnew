package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
        Button controls = view.findViewById(R.id.custom_control_button);
        Button installJar = view.findViewById(R.id.install_jar_button);
        Button shareLogs = view.findViewById(R.id.share_logs_button);
        Button openDirectory = view.findViewById(R.id.open_files_button);
        ImageButton editProfile = view.findViewById(R.id.edit_profile_button);
        Button play = view.findViewById(R.id.play_button);

        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        mRecentInstances = view.findViewById(R.id.recent_instances_container);
        mUpdateChecker = new UpdateChecker(requireContext());

        controls.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            startActivity(new Intent(requireContext(), CustomControlsActivity.class));
        });
        installJar.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            runInstallerWithConfirmation();
        });
        editProfile.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            mVersionSpinner.openProfileEditor(requireActivity());
        });
        play.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            launchSelectedInstance();
        });
        shareLogs.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            shareLog(requireContext());
        });
        openDirectory.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
            openGameDirectory(v.getContext());
        });

        View heroCard = view.findViewById(R.id.active_instance_card);
        if (heroCard != null) {
            Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_bottom);
            heroCard.startAnimation(anim);
        }

        refreshSelectedInstance(view);
        renderRecentInstances();
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
        ImageView icon = root.findViewById(R.id.active_instance_icon);
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
            item.setMinHeight(120);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            item.setPadding(24, 18, 24, 18);
            item.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));
            item.setTextSize(14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 4, 0, 8);
            item.setLayoutParams(lp);

            String name = Tools.validOrNullString(instance.name);
            if (name == null) name = safeVersion(instance.versionId);
            String when = instance.lastPlayedAt <= 0 ? "Nunca jugado"
                    : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(instance.lastPlayedAt));
            item.setText(name + "\n" + when);
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    InstanceIconProvider.fetchIcon(getResources(), instance), null, null, null);
            item.setCompoundDrawablePadding(20);
            item.setBackgroundResource(R.drawable.recent_instance_bg);
            item.setOnClickListener(v -> {
                v.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_fast));
                InstanceManager.setSelectedInstance(instance);
                mVersionSpinner.reloadProfiles();
                refreshSelectedInstance(requireView());
                renderRecentInstances();
            });
            mRecentInstances.addView(item);
        }
    }

    private void checkForUpdate() {
    }

    private void showUpdateDialog(UpdateChecker.Update update) {
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
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else {
            Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
        }
    }

}
