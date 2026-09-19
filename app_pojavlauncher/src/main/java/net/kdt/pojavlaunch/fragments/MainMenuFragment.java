package net.kdt.pojavlaunch.fragments;

import static net.kdt.pojavlaunch.Tools.openPath;
import static net.kdt.pojavlaunch.Tools.shareLog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
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
    private View mUpdateCard;
    private TextView mUpdateVersion;
    private TextView mUpdateBody;
    private UpdateChecker mUpdateChecker;

    private final ActivityResultLauncher<Object> mModInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(requireContext(), data);
            });

    public MainMenuFragment(){
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
        Button play = view.findViewById(R.id.play_button);

        mVersionSpinner = view.findViewById(R.id.mc_version_spinner);
        mRecentInstances = view.findViewById(R.id.recent_instances_container);
        mUpdateCard = view.findViewById(R.id.update_card);
        mUpdateVersion = view.findViewById(R.id.update_version);
        mUpdateBody = view.findViewById(R.id.update_body);
        mUpdateChecker = new UpdateChecker(requireContext());

        news.setOnClickListener(v -> Tools.openURL(requireActivity(), Tools.URL_HOME));
        discord.setOnClickListener(v -> Tools.openURL(requireActivity(), getString(R.string.discord_invite)));
        controls.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomControlsActivity.class)));
        installJar.setOnClickListener(v -> runInstallerWithConfirmation());
        editProfile.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        play.setOnClickListener(v -> launchSelectedInstance());
        shareLogs.setOnClickListener(v -> shareLog(requireContext()));
        openDirectory.setOnClickListener(v -> openGameDirectory(v.getContext()));

        view.findViewById(R.id.rail_home).setOnClickListener(v -> {
            v.setSelected(true);
        });
        view.findViewById(R.id.rail_instances).setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));
        view.findViewById(R.id.rail_mods).setOnClickListener(v -> runInstallerWithConfirmation());
        view.findViewById(R.id.rail_settings).setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class, "SETTINGS_FRAGMENT", null));

        news.setOnLongClickListener(v -> {
            Tools.swapFragment(requireActivity(), GamepadMapperFragment.class, GamepadMapperFragment.TAG, null);
            return true;
        });

        Button updateInstall = view.findViewById(R.id.update_install_button);
        Button updateSkip = view.findViewById(R.id.update_skip_button);
        updateInstall.setOnClickListener(v -> mUpdateChecker.installLatest(requireActivity()));
        updateSkip.setOnClickListener(v -> {
            mUpdateChecker.skipLatest();
            mUpdateCard.setVisibility(View.GONE);
        });

        refreshSelectedInstance(view);
        renderRecentInstances();
        checkForUpdate();
    }

    private void launchSelectedInstance() {
        Instance selected = InstanceManager.getSelectedListedInstance();
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
        if(instance == null) return;

        String instanceName = Tools.validOrNullString(instance.name);
        if(instanceName == null) instanceName = getString(R.string.main_instance_default_name);
        name.setText(instanceName);
        details.setText(getString(R.string.main_instance_details, safeVersion(instance.versionId), detectModLoader(instance)));
        mods.setText(getString(R.string.main_instance_mods, countMods(instance)));
    }

    private String safeVersion(String version) {
        if(version == null || version.trim().isEmpty()) return getString(R.string.main_unknown);
        return version;
    }

    private String detectModLoader(Instance instance) {
        if(instance.installer != null) {
            String name = instance.installer.getClass().getSimpleName().toLowerCase();
            if(name.contains("forge")) return "Forge";
            if(name.contains("fabric")) return "Fabric";
            if(name.contains("quilt")) return "Quilt";
            if(name.contains("neoforge")) return "NeoForge";
        }
        File mods = new File(instance.getGameDirectory(), "mods");
        if(mods.isDirectory()) {
            File[] files = mods.listFiles();
            if(files != null) {
                for(File file : files) {
                    String n = file.getName().toLowerCase();
                    if(n.contains("fabric")) return "Fabric";
                    if(n.contains("neoforge")) return "NeoForge";
                    if(n.contains("forge")) return "Forge";
                    if(n.contains("quilt")) return "Quilt";
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
        if(mRecentInstances == null) return;
        mRecentInstances.removeAllViews();
        List<Instance> instances = new ArrayList<>(InstanceManager.getImmutableInstanceList());
        Collections.sort(instances, Comparator.comparingLong((Instance i) -> i.lastPlayedAt).reversed());
        int shown = Math.min(5, instances.size());
        for(int index = 0; index < shown; index++) {
            Instance instance = instances.get(index);
            TextView item = new TextView(requireContext());
            item.setMinHeight(56);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            item.setPadding(16, 8, 16, 8);
            item.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));
            item.setTextSize(14);
            String name = Tools.validOrNullString(instance.name);
            if(name == null) name = safeVersion(instance.versionId);
            String when = instance.lastPlayedAt <= 0 ? getString(R.string.main_never_played)
                    : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(instance.lastPlayedAt));
            item.setText(name + "\n" + when);
            item.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    InstanceIconProvider.fetchIcon(getResources(), instance), null, null, null);
            item.setCompoundDrawablePadding(14);
            item.setBackgroundResource(R.drawable.recent_instance_bg);
            item.setOnClickListener(v -> {
                InstanceManager.setSelectedInstance(instance);
                mVersionSpinner.setSelection(mVersionSpinner.resolveInstanceIndex(instance));
                refreshSelectedInstance(requireView());
                renderRecentInstances();
            });
            mRecentInstances.addView(item);
        }
    }

    private void checkForUpdate() {
        mUpdateChecker.check(update -> {
            if(!isAdded() || update == null) return;
            mUpdateVersion.setText(getString(R.string.update_available_version, update.version));
            String body = TextUtils.isEmpty(update.body) ? getString(R.string.update_no_notes) : update.body.trim();
            mUpdateBody.setText(body);
            mUpdateBody.setMaxLines(6);
            mUpdateBody.setEllipsize(TextUtils.TruncateAt.END);
            mUpdateCard.setVisibility(View.VISIBLE);
        });
    }

    private void openGameDirectory(Context context) {
        File gameDirectory = InstanceManager.getSelectedListedInstance().getGameDirectory();
        if(FileUtils.ensureDirectorySilently(gameDirectory)) {
            openPath(context, gameDirectory, false);
        }else {
            Toast.makeText(context, R.string.gamedir_open_failed, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        if(getView() != null) {
            refreshSelectedInstance(getView());
            renderRecentInstances();
            checkForUpdate();
        }
    }

    private void runInstallerWithConfirmation() {
        if (ProgressKeeper.getTaskCount() == 0) {
            mModInstallerLauncher.launch(null);
        } else Toast.makeText(requireContext(), R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroyView() {
        if(mUpdateChecker != null) mUpdateChecker.close();
        super.onDestroyView();
    }
}
