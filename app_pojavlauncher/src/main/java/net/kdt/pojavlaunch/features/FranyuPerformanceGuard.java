package net.kdt.pojavlaunch.features;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Debug;
import android.widget.Toast;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import java.io.File;

public final class FranyuPerformanceGuard {
    public static final class State {
        public final boolean lowBattery, slowCharge, thermal;
        public final double temperatureC;
        State(boolean lowBattery, boolean slowCharge, boolean thermal, double temperatureC) { this.lowBattery=lowBattery; this.slowCharge=slowCharge; this.thermal=thermal; this.temperatureC=temperatureC; }
        public boolean reducePerformance(Context c) { return (lowBattery && FranyuFeatureStore.lowBattery(c)) || (slowCharge && FranyuFeatureStore.slowCharge(c) && FranyuFeatureStore.chargeAndPlay(c)) || (thermal && FranyuFeatureStore.thermal(c)); }
    }
    public static State read(Context c) {
        BatteryManager bm=(BatteryManager)c.getSystemService(Context.BATTERY_SERVICE);
        int level=bm==null?100:bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        boolean charging=bm!=null && bm.isCharging();
        long current=charging&&Build.VERSION.SDK_INT>=21?bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW):0;
        boolean slow=charging && Math.abs(current)>0 && Math.abs(current)<900000;
        double temp=readDeviceTemperature();
        return new State(level<=FranyuFeatureStore.lowBatteryThreshold(c),slow,temp>=FranyuFeatureStore.thermalThreshold(c),temp);
    }
    public static double readDeviceTemperature() {
        File root=new File("/sys/class/thermal"); File[] zones=root.listFiles((d,n)->n!=null&&n.startsWith("thermal_zone")); double fallback=-1;
        if(zones!=null) for(File z:zones) try { String type=Tools.read(new File(z,"type").getAbsolutePath()).trim().toLowerCase(); double t=Double.parseDouble(Tools.read(new File(z,"temp").getAbsolutePath()).trim())/1000.0; if(type.contains("skin")||type.contains("battery")||type.contains("pmic")) return t; if(fallback<0) fallback=t; } catch(Throwable ignored) {}
        return fallback;
    }
    public static boolean preLaunchCheck(Context c, Instance i, int javaVersion) {
        if(!FranyuFeatureStore.diagnostics(c)) return true;
        if(!Tools.checkStorageRoot(c)){
            Toast.makeText(c,"Diagnóstico: no se puede acceder al almacenamiento. Concede el permiso e inténtalo de nuevo.",Toast.LENGTH_LONG).show();
            return false;
        }
        File storageProbe=i.getGameDirectory();
        while(storageProbe!=null && !storageProbe.exists()) storageProbe=storageProbe.getParentFile();
        if(storageProbe==null || !storageProbe.canRead() || !storageProbe.canWrite()){
            Toast.makeText(c,"Diagnóstico: la carpeta del juego no está disponible. Revisa el permiso de almacenamiento.",Toast.LENGTH_LONG).show();
            return false;
        }
        long usableSpace=storageProbe.getUsableSpace();
        // Android may report 0 for a path that does not exist yet or is not
        // readable. That is an access problem, not proof that the disk is full.
        if(usableSpace>0 && usableSpace<1024L*1024L*1024L){Toast.makeText(c,"Diagnóstico: queda menos de 1 GB libre.",Toast.LENGTH_LONG).show();return false;}
        if(Tools.getFreeDeviceMemory(c)<384){Toast.makeText(c,"Diagnóstico: hay muy poca RAM libre.",Toast.LENGTH_LONG).show();return false;}
        boolean java=false; try { java=MultiRTUtils.getRuntimes().stream().anyMatch(r->r.javaVersion>=javaVersion); } catch(Throwable ignored) {}
        if(!java){Toast.makeText(c,"Diagnóstico: no hay Java compatible instalado.",Toast.LENGTH_LONG).show();return false;}
        return true;
    }
    public static String applySafeProfile(Context c, Instance i) {
        State s=read(c); if(!s.reducePerformance(c)) return null;
        try { MCOptionUtils.load(i.getGameDirectory().getAbsolutePath()); MCOptionUtils.set("maxFps","30"); MCOptionUtils.set("renderDistance","6"); MCOptionUtils.set("fancyGraphics","false"); MCOptionUtils.set("renderClouds","fast"); MCOptionUtils.save(); return s.temperatureC>=0?"Rendimiento reducido: "+Math.round(s.temperatureC)+"°C":"Rendimiento reducido para proteger batería y temperatura."; } catch(Throwable e) { return "Rendimiento reducido activado."; }
    }
    public static int processRssMb() { Debug.MemoryInfo i=new Debug.MemoryInfo(); Debug.getMemoryInfo(i); return i.getTotalPss()/1024; }
    private FranyuPerformanceGuard() {}
}
