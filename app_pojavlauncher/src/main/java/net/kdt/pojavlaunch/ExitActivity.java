package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.shareLog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.R;

import android.content.pm.ActivityInfo;
import java.io.File;
import java.io.RandomAccessFile;

@Keep
public class ExitActivity extends AppCompatActivity {

    @SuppressLint("StringFormatInvalid") //invalid on some translations but valid on most, cant fix that atm
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        int code = -1;
        boolean isSignal = false;
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            code = extras.getInt("code",-1);
            isSignal = extras.getBoolean("isSignal", false);
        }

        StringBuilder messageBuilder = new StringBuilder();
        if(isSignal) {
            messageBuilder.append(getString(R.string.mcn_abort_title));
        } else {
            messageBuilder.append(getString(R.string.mcn_exit_title, code));
        }

        String diagnostic = analyzeCrashReason();
        if(diagnostic != null && !diagnostic.isEmpty()) {
            messageBuilder.append("\n\n").append(diagnostic);
        }

        new AlertDialog.Builder(this)
                .setTitle("Minecraft se detuvo")
                .setMessage(messageBuilder.toString())
                .setPositiveButton(R.string.main_share_logs, (dialog, which) -> shareLog(this))
                .setNegativeButton(android.R.string.ok, (dialog, which) -> ExitActivity.this.finish())
                .setOnDismissListener(dialog -> ExitActivity.this.finish())
                .show();
    }

    private String analyzeCrashReason() {
        try {
            File logFile = new File(Tools.DIR_GAME_HOME, "latestlog.txt");
            if (!logFile.exists() || logFile.length() == 0) return null;
            long readLength = Math.min(logFile.length(), 65536L);
            byte[] bytes = new byte[(int) readLength];
            try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
                raf.seek(logFile.length() - readLength);
                raf.readFully(bytes);
            }
            String logSnippet = new String(bytes);
            if (logSnippet.contains("UnsupportedClassVersionError") || logSnippet.contains("has been compiled by a more recent version")
                    || (logSnippet.contains("LaunchClassLoader") && logSnippet.contains("ClassCastException"))) {
                return "🔍 Causa detectada: Incompatibilidad de versión Java.\n💡 Solución: Minecraft 1.12.1 y versiones anteriores con OptiFine o Forge requieren Java 8 (JRE 8). Hemos configurado automáticamente Java 8 para esta instancia.";
            }
            if (logSnippet.contains("OutOfMemoryError")) {
                return "🔍 Causa detectada: Memoria RAM insuficiente.\n💡 Solución: Aumenta la asignación de memoria RAM en los Ajustes del Launcher o reduce la distancia de renderizado.";
            }
            if (logSnippet.contains("DuplicateModsFoundException") || logSnippet.contains("Found duplicate")) {
                return "🔍 Causa detectada: Mods duplicados.\n💡 Solución: Revisa la carpeta mods y elimina archivos repetidos.";
            }
            if (logSnippet.contains("ModLoadingException") || logSnippet.contains("MixinApplyError") || logSnippet.contains("InvalidModFileException")) {
                return "🔍 Causa detectada: Mod incompatible o corrupto.\n💡 Solución: Uno de los mods instalados no es compatible con esta versión del juego.";
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @SuppressWarnings("unused") //used by native jre_launcher_new
    public static void showExitMessage(Context ctx, int code, boolean isSignal) {
        if((!isSignal && code == 0) || ctx == null) {
            System.exit(0);
            return;
        }

        Object lock = new Object();
        Tools.runOnUiThread(()->{
            Intent i = new Intent(ctx,ExitActivity.class);
            i.putExtra("code",code);
            i.putExtra("isSignal", isSignal);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (InterruptedException e) {
                Log.e("ExitActivity", "Waiting on lock failed: "+e);
            }
        }
        System.exit(0);
    }

}
