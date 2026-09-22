package net.kdt.pojavlaunch;

import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import com.kdt.mcgui.ProgressLayout;
import net.kdt.pojavlaunch.authenticator.accounts.PojavProfile;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.extra.ExtraListener;
import net.kdt.pojavlaunch.features.FranyuModInspector;
import net.kdt.pojavlaunch.features.FranyuPerformanceGuard;
import net.kdt.pojavlaunch.fragments.MainMenuFragment;
import net.kdt.pojavlaunch.fragments.MicrosoftLoginFragment;
import net.kdt.pojavlaunch.fragments.SelectAuthFragment;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.InstanceInstaller;
import net.kdt.pojavlaunch.instances.InstanceManager;
import net.kdt.pojavlaunch.lifecycle.ContextAwareDoneListener;
import net.kdt.pojavlaunch.lifecycle.ContextExecutor;
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.IconCacheJanitor;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.prefs.screens.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;
import net.kdt.pojavlaunch.services.ProgressServiceKeeper;
import net.kdt.pojavlaunch.tasks.AsyncMinecraftDownloader;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.tasks.MinecraftDownloader;
import net.kdt.pojavlaunch.utils.NotificationUtils;
import java.lang.ref.WeakReference;
import git.artdeell.mojo.R;

public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG="SETTINGS_FRAGMENT";
    private FragmentContainerView mFragmentView;
    private ImageButton mSettingsButton;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private NotificationManager mNotificationManager;
    private UpdateStartupGate mUpdateStartupGate;
    private boolean mStartupGateFinished;

    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener=new FragmentManager.FragmentLifecycleCallbacks(){@Override public void onFragmentResumed(@NonNull FragmentManager fm,@NonNull Fragment f){mSettingsButton.setImageDrawable(ContextCompat.getDrawable(getBaseContext(),f instanceof MainMenuFragment?R.drawable.ic_px_sliders:R.drawable.ic_px_home));}};
    private final ExtraListener<String> mBackPreferenceListener=(key,value)->{if(value.equals("true"))onBackPressed();return false;};
    private final ExtraListener<Boolean> mSelectAuthMethod=(key,value)->{if(!value)return false;Fragment f=getSupportFragmentManager().findFragmentById(mFragmentView.getId());if(!(f instanceof MainMenuFragment))return false;Tools.swapFragment(this,SelectAuthFragment.class,SelectAuthFragment.TAG,null);return false;};
    private final View.OnClickListener mSettingButtonListener=v->{Fragment f=getSupportFragmentManager().findFragmentById(mFragmentView.getId());if(f instanceof MainMenuFragment)Tools.swapFragment(this,LauncherPreferenceFragment.class,SETTING_FRAGMENT_TAG,null);else Tools.backToMainMenu(this);};
    private final ExtraListener<Boolean> mLaunchGameListener=(key,value)->{if(mProgressLayout.hasProcesses()){Toast.makeText(this,R.string.tasks_ongoing,Toast.LENGTH_LONG).show();return false;} Instance i=InstanceManager.getSelectedListedInstance();if(i.installer!=null){i.installer.start();return false;}if(!Tools.isValidString(i.versionId)){Toast.makeText(this,R.string.error_no_version,Toast.LENGTH_LONG).show();return false;}if(PojavProfile.getCurrentProfileContent(true)==null){Toast.makeText(this,R.string.no_saved_accounts,Toast.LENGTH_LONG).show();ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD,true);return false;}String id=AsyncMinecraftDownloader.normalizeVersionId(i.versionId);JMinecraftVersionList.Version v=AsyncMinecraftDownloader.getListedVersion(id);int javaVersion=v.javaVersion==null?8:v.javaVersion.majorVersion;if(!FranyuPerformanceGuard.preLaunchCheck(this,i,javaVersion))return false;String safe=FranyuPerformanceGuard.applySafeProfile(this,i);if(safe!=null)Toast.makeText(this,safe,Toast.LENGTH_LONG).show();if(LauncherPreferences.PREF_VERIFY_FILES){java.util.List<FranyuModInspector.Conflict> conflicts=FranyuModInspector.findConflicts(i);if(!conflicts.isEmpty()){new AlertDialog.Builder(this).setTitle("Conflicto de mods").setMessage("Se detectan mods duplicados o con el mismo identificador:\n"+conflicts.get(0).files).setPositiveButton(android.R.string.ok,null).show();return false;}}new MinecraftDownloader().start(this,v,id,new ContextAwareDoneListener(this,id));return false;};
    private final TaskCountListener mDoubleLaunchPreventionListener=taskCount->{if(taskCount>0)Tools.runOnUiThread(()->mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START));return false;};
    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;

    @Override protected boolean shouldIgnoreNotch(){return getResources().getConfiguration().orientation==ORIENTATION_PORTRAIT;}
    @Override public boolean setFullscreen(){return false;}

    @Override protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);
        bindViews();
        mUpdateStartupGate=new UpdateStartupGate(this);
        mUpdateStartupGate.start(this::finishStartupGate);
        FragmentManager fm=getSupportFragmentManager();
        if(fm.getBackStackEntryCount()<1)fm.beginTransaction().setReorderingAllowed(true).addToBackStack("ROOT").add(R.id.container_fragment,MainMenuFragment.class,null,"ROOT").commit();
        IconCacheJanitor.runJanitor();
        mRequestNotificationPermissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(),isAllowed->{if(!isAllowed)handleNoNotificationPermission();else{Runnable r=Tools.getWeakReference(mRequestNotificationPermissionRunnable);if(r!=null)r.run();}});
        getWindow().setBackgroundDrawable(null);
        checkNotificationPermission();
        mNotificationManager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener(mProgressServiceKeeper=new ProgressServiceKeeper(this));
        mSettingsButton.setOnClickListener(mSettingButtonListener);
        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE,mBackPreferenceListener);
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD,mSelectAuthMethod);
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME,mLaunchGameListener);
        new AsyncVersionList().getVersionList(versions->ExtraCore.setValue(ExtraConstants.RELEASE_TABLE,versions));
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
        mProgressLayout.observe(ProgressLayout.INSTANCE_INSTALL);
    }

    private void finishStartupGate(){
        if(mStartupGateFinished)return;
        mStartupGateFinished=true;
    }

    @Override protected void onResume(){super.onResume();ContextExecutor.setActivity(this);InstanceInstaller.postInstallCheck(this);}
    @Override protected void onPause(){super.onPause();ContextExecutor.clearActivity();}
    @Override protected void onStart(){super.onStart();getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener,true);}
    @Override protected void onDestroy(){
        if(mUpdateStartupGate!=null){mUpdateStartupGate.close();mUpdateStartupGate=null;}
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE,mBackPreferenceListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD,mSelectAuthMethod);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME,mLaunchGameListener);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
    }

    @Override public void onBackPressed(){MicrosoftLoginFragment f=(MicrosoftLoginFragment)getVisibleFragment(MicrosoftLoginFragment.TAG);if(f!=null&&f.canGoBack()){f.goBack();return;}if(getVisibleFragment("ROOT")!=null)finish();super.onBackPressed();}
    private Fragment getVisibleFragment(String tag){Fragment f=getSupportFragmentManager().findFragmentByTag(tag);return f!=null&&f.isVisible()?f:null;}
    private void checkNotificationPermission(){if(LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK||checkForNotificationPermission())return;if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.POST_NOTIFICATIONS)){showNotificationPermissionReasoning();return;}askForNotificationPermission(null);}
    private void showNotificationPermissionReasoning(){new AlertDialog.Builder(this).setTitle(R.string.notification_permission_dialog_title).setMessage(R.string.notification_permission_dialog_text).setPositiveButton(android.R.string.ok,(d,w)->askForNotificationPermission(null)).setNegativeButton(android.R.string.cancel,(d,w)->handleNoNotificationPermission()).show();}
    private void handleNoNotificationPermission(){LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK=true;LauncherPreferences.DEFAULT_PREF.edit().putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK,true).apply();Toast.makeText(this,R.string.notification_permission_toast,Toast.LENGTH_LONG).show();}
    public boolean checkForNotificationPermission(){return Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_DENIED;}
    public void askForNotificationPermission(Runnable r){if(Build.VERSION.SDK_INT<33)return;if(r!=null)mRequestNotificationPermissionRunnable=new WeakReference<>(r);mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);}
    private void bindViews(){mFragmentView=findViewById(R.id.container_fragment);mSettingsButton=findViewById(R.id.setting_button);mProgressLayout=findViewById(R.id.progress_layout);}
}
