package net.kdt.pojavlaunch.features;

import net.kdt.pojavlaunch.instances.Instance;
import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FranyuModInspector {
    public static final class Conflict { public final String id; public final List<String> files; Conflict(String id,List<String> files){this.id=id;this.files=files;} }
    public static List<File> modFiles(Instance i){ File d=new File(i.getGameDirectory(),"mods"); File[] f=d.listFiles((x,n)->n!=null&&n.toLowerCase().endsWith(".jar")); List<File> r=new ArrayList<>(); if(f!=null)Collections.addAll(r,f); return r; }
    public static List<Conflict> findConflicts(Instance i){ Map<String,List<String>> ids=new HashMap<>(); for(File f:modFiles(i)){String id=readModId(f);if(id==null)id=f.getName().replaceFirst("(?i)\\.jar$","").replaceAll("-[0-9].*$","").toLowerCase();ids.computeIfAbsent(id,k->new ArrayList<>()).add(f.getName());} List<Conflict> r=new ArrayList<>(); for(Map.Entry<String,List<String>> e:ids.entrySet())if(e.getValue().size()>1)r.add(new Conflict(e.getKey(),e.getValue())); return r; }
    private static String readModId(File f){ try(ZipFile z=new ZipFile(f)){String[] names={"fabric.mod.json","quilt.mod.json","META-INF/mods.toml","mcmod.info"}; for(String n:names){ZipEntry e=z.getEntry(n);if(e==null)continue;StringBuilder s=new StringBuilder();try(BufferedReader r=new BufferedReader(new InputStreamReader(z.getInputStream(e),"UTF-8"))){String line;int c=0;while((line=r.readLine())!=null&&c++<300)s.append(line).append('\n');} Matcher m=Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(s);if(m.find())return m.group(1).toLowerCase();m=Pattern.compile("modId\\s*=\\s*\\\"([^\\\"]+)\\\"").matcher(s);if(m.find())return m.group(1).toLowerCase();}}}catch(Throwable ignored){}return null; }
    private FranyuModInspector(){}
}
