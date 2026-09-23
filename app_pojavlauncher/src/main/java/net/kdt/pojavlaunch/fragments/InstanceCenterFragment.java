package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.features.FranyuInstanceCenter;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceIconProvider;
import net.kdt.pojavlaunch.instances.InstanceManager;

import java.io.IOException;
import java.util.List;

public class InstanceCenterFragment extends Fragment {
    public static final String TAG = "InstanceCenterFragment";
    private LinearLayout mList;

    public InstanceCenterFragment() {
        super(R.layout.fragment_franyu_instance_center);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.instance_center_list);
        view.findViewById(R.id.instance_center_new).setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                        ProfileTypeSelectFragment.TAG, null));
        view.findViewById(R.id.instance_center_home).setOnClickListener(v ->
                Tools.backToMainMenu(requireActivity()));
        render();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mList != null) render();
    }

    private void render() {
        mList.removeAllViews();
        List<Instance> instances = FranyuInstanceCenter.allInstances();
        Instance selected = InstanceManager.getSelectedListedInstance();

        if (instances.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("No hay instancias disponibles.");
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text));
            empty.setPadding(20, 24, 20, 24);
            mList.addView(empty);
            return;
        }

        for (Instance instance : instances) {
            mList.addView(createInstanceRow(instance, instance == selected));
        }
    }

    private View createInstanceRow(Instance instance, boolean selected) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(16, 12, 12, 12);
        row.setMinimumHeight(82);
        row.setBackgroundResource(selected ? R.drawable.hero_instance_bg : R.drawable.recent_instance_bg);

        ImageView icon = new ImageView(requireContext());
        icon.setLayoutParams(new LinearLayout.LayoutParams(56, 56));
        android.graphics.drawable.Drawable drawable =
                InstanceIconProvider.fetchIcon(getResources(), instance);
        if (drawable != null) icon.setImageDrawable(drawable);
        icon.setContentDescription(instance.name);

        LinearLayout textBox = new LinearLayout(requireContext());
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(14, 0, 8, 0);
        textBox.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView name = new TextView(requireContext());
        String displayName = Tools.validOrNullString(instance.name);
        if (displayName == null) displayName = Tools.validOrNullString(instance.versionId);
        if (displayName == null) displayName = "Instancia";
        name.setText(displayName);
        name.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_text));
        name.setTextSize(17);
        name.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView details = new TextView(requireContext());
        details.setText(safe(instance.versionId) + " • " + FranyuInstanceCenter.modCount(instance) + " mods");
        details.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary_text));
        details.setTextSize(12);
        details.setPadding(0, 3, 0, 0);

        textBox.addView(name);
        textBox.addView(details);

        TextView edit = new TextView(requireContext());
        edit.setText("Editar");
        edit.setTextColor(ContextCompat.getColor(requireContext(), R.color.emerald_accent));
        edit.setTextSize(13);
        edit.setPadding(10, 10, 10, 10);
        edit.setOnClickListener(v -> {
            InstanceManager.setSelectedInstance(instance);
            Tools.swapFragment(requireActivity(), InstanceEditorFragment.class,
                    InstanceEditorFragment.TAG, null);
        });

        row.addView(icon);
        row.addView(textBox);
        row.addView(edit);

        row.setOnClickListener(v -> select(instance));
        row.setOnLongClickListener(v -> {
            showActions(instance);
            return true;
        });
        return row;
    }

    private void select(Instance instance) {
        InstanceManager.setSelectedInstance(instance);
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        Toast.makeText(requireContext(), "Instancia activa: " + safe(instance.name), Toast.LENGTH_SHORT).show();
        render();
    }

    private void showActions(Instance instance) {
        String name = safe(instance.name);
        String[] actions = {"Seleccionar", "Editar", "Eliminar"};
        new AlertDialog.Builder(requireContext())
                .setTitle(name)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        select(instance);
                    } else if (which == 1) {
                        InstanceManager.setSelectedInstance(instance);
                        Tools.swapFragment(requireActivity(), InstanceEditorFragment.class,
                                InstanceEditorFragment.TAG, null);
                    } else {
                        confirmDelete(instance);
                    }
                }).show();
    }

    private void confirmDelete(Instance instance) {
        if (FranyuInstanceCenter.allInstances().size() <= 1) {
            Toast.makeText(requireContext(), "Debe quedar al menos una instancia.", Toast.LENGTH_LONG).show();
            return;
        }
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar instancia")
                .setMessage("Se eliminarán los archivos de «" + safe(instance.name) + "». Esta acción no se puede deshacer.")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Eliminar", (d, w) -> {
                    try {
                        InstanceManager.removeInstance(instance);
                        render();
                    } catch (IOException e) {
                        Toast.makeText(requireContext(), "No se pudo eliminar la instancia.", Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Sin nombre" : value;
    }
}
