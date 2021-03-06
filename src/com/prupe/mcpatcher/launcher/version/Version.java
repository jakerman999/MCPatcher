package com.prupe.mcpatcher.launcher.version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.prupe.mcpatcher.*;

import java.io.*;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipFile;

public class Version implements Comparable<Version> {
    private static final String BASE_URL = "http://s3.amazonaws.com/Minecraft.Download/versions/";

    private static final String TAG_ID = "id";
    private static final String TAG_TIME = "time";
    private static final String TAG_RELEASE_TIME = "releaseTime";
    private static final String TAG_LIBRARIES = "libraries";
    private static final String TAG_NAME = "name";
    private static final String TAG_JAR = "jar";
    private static final String TAG_DOWNLOADS = "downloads";

    private static final String LEGACY = "legacy";
    private static final String LEGACY_VALUE = "${auth_player_name} ${auth_session}";

    private static final String USERNAME_SESSION = "username_session";
    private static final String USERNAME_SESSION_VALUE = "--username ${auth_player_name} --session ${auth_session}";

    private static final String USERNAME_SESSION_VERSION = "username_session_version";
    private static final String USERNAME_SESSION_VERSION_VALUE = "--username ${auth_player_name} --session ${auth_session} --version ${version_name}";

    private static final DateFormat[] DATE_FORMATS = new DateFormat[]{
        new SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ssZ"),
        DateFormat.getDateTimeInstance(2, 2, Locale.US),
    };

    String id;
    String type = "release";
    String processArguments = USERNAME_SESSION_VERSION;
    String minecraftArguments = "";
    String mainClass = "net.minecraft.client.main.Main";
    int minimumLauncherVersion;
    String assets = "";
    String inheritsFrom;
    String jar;
    List<Library> libraries = new ArrayList<Library>();

    public static Version getLocalVersion(String id) {
        InputStream input = null;
        File local = getJarPath(id);
        if (!local.isFile()) {
            return null;
        }
        try {
            fetchJson(id, false);
            local = getJsonPath(id);
            if (!local.isFile()) {
                return null;
            }
            input = new FileInputStream(local);
            InputStreamReader reader = new InputStreamReader(input);
            return JsonUtils.newGson().fromJson(reader, Version.class);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        } finally {
            MCPatcherUtils.close(input);
        }
    }

    public static Version getLocalVersionIfComplete(String id) {
        File jar = getJarPath(id);
        File json = getJsonPath(id);
        if (!jar.isFile() || !json.isFile()) {
            return null;
        }
        Version version = getLocalVersion(id);
        if (version == null || !version.isComplete()) {
            return null;
        } else {
            return version;
        }
    }

    public static void fetchJson(String id, boolean forceRemote) throws PatcherException {
        File path = getJsonPath(id);
        if (forceRemote || !path.isFile()) {
            Util.fetchURL(getJsonURL(id), path, forceRemote, Util.LONG_TIMEOUT, Util.JSON_SIGNATURE);
        }
    }

    public static File getJsonPath(String id) {
        return MCPatcherUtils.getMinecraftPath("versions", id, id + ".json");
    }

    public static URL getJsonURL(String id) {
        return Util.newURL(BASE_URL + id + "/" + id + ".json");
    }

    public static void fetchJar(String id, boolean forceRemote) throws PatcherException {
        Util.fetchURL(getJarURL(id), getJarPath(id), forceRemote, Util.LONG_TIMEOUT, Util.JAR_SIGNATURE);
    }

    public static File getJarPath(String id) {
        return MCPatcherUtils.getMinecraftPath("versions", id, id + ".jar");
    }

    public static URL getJarURL(String id) {
        return Util.newURL(BASE_URL + id + "/" + id + ".jar");
    }

    public static boolean deleteLocalFiles(String id) {
        File baseDir = getJsonPath(id).getParentFile();
        File[] list = baseDir.listFiles();
        if (list != null) {
            for (File f : list) {
                if (f.isDirectory()) {
                    File[] list1 = baseDir.listFiles();
                    if (list1 != null) {
                        for (File f1 : list1) {
                            f1.delete();
                        }
                    }
                }
                f.delete();
            }
        }
        baseDir.delete();
        return !baseDir.exists();
    }

    private Version() {
    }

    @Override
    public String toString() {
        return String.format("Version{%s, %s}", type, id);
    }

    public String getId() {
        return id;
    }

    public boolean isSnapshot() {
        return "snapshot".equals(type);
    }

    public File getJsonPath() {
        return getJsonPath(id);
    }

    public File getJarPath() {
        if (MCPatcherUtils.isNullOrEmpty(jar)) {
            return getJarPath(id);
        } else {
            return getJarPath(jar);
        }
    }

    public List<Library> getLibraries() {
        if (MCPatcherUtils.isNullOrEmpty(inheritsFrom)) {
            return libraries;
        } else {
            return addLibraries(new ArrayList<Library>());
        }
    }

    private List<Library> addLibraries(List<Library> allLibraries) {
        if (libraries != null) {
            allLibraries.addAll(libraries);
        }
        if (!MCPatcherUtils.isNullOrEmpty(inheritsFrom)) {
            Version parentVersion = getLocalVersion(inheritsFrom);
            if (parentVersion != null) {
                parentVersion.addLibraries(allLibraries);
            }
        }
        return allLibraries;
    }

    public boolean isComplete() {
        return getJsonPath().isFile() && getJarPath().isFile();
    }

    public boolean isPatched() {
        File jar = getJarPath();
        ZipFile zip = null;
        try {
            zip = new ZipFile(jar);
            if (zip.getEntry(Config.MCPATCHER_PROPERTIES) != null) {
                return true;
            }
        } catch (IOException e) {
            Logger.log(e);
        } finally {
            MCPatcherUtils.close(zip);
        }
        return false;
    }

    public Version copyToNewVersion(JsonObject base, String newid, List<Library> extraLibraries) {
        JsonObject json = JsonUtils.parseJson(getJsonPath());
        if (json == null) {
            return null;
        }
        if (base != null) {
            for (Map.Entry<String, JsonElement> entry : base.entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
        }
        File outPath = getJsonPath(newid);
        json.addProperty(TAG_ID, newid);
        updateDateField(json, TAG_TIME);
        updateDateField(json, TAG_RELEASE_TIME);
        json.remove(TAG_JAR);
        json.remove(TAG_DOWNLOADS);
        if (extraLibraries != null) {
            for (Library library : extraLibraries) {
                addLibrary(json, library);
            }
        }
        outPath.getParentFile().mkdirs();
        if (!JsonUtils.writeJson(json, outPath)) {
            return null;
        }
        return getLocalVersion(newid);
    }

    private static void updateDateField(JsonObject json, String field) {
        JsonElement element = json.get(field);
        if (!element.isJsonPrimitive()) {
            return;
        }
        String oldValue = element.getAsString();
        if (MCPatcherUtils.isNullOrEmpty(oldValue)) {
            return;
        }
        for (DateFormat format : DATE_FORMATS) {
            try {
                String newValue = changeDate(format, oldValue);
                json.addProperty(field, newValue);
                return;
            } catch (ParseException e) {
                // continue
            }
        }
    }

    private static void addLibrary(JsonObject json, Library library) {
        JsonElement element = json.get(TAG_LIBRARIES);
        if (element == null || !element.isJsonArray()) {
            return;
        }
        JsonArray libraries = element.getAsJsonArray();
        String prefix = library.getPackageName() + ":" + library.getName() + ":";
        for (JsonElement lib : libraries) {
            if (lib == null || !lib.isJsonObject()) {
                continue;
            }
            element = lib.getAsJsonObject().get(TAG_NAME);
            if (element == null || !element.isJsonPrimitive()) {
                continue;
            }
            String name = element.getAsString();
            if (MCPatcherUtils.isNullOrEmpty(name)) {
                continue;
            }
            if (name.startsWith(prefix)) {
                return;
            }
        }
        libraries.add(JsonUtils.newGson().toJsonTree(library, Library.class));
    }

    private static String changeDate(DateFormat format, String oldValue) throws ParseException {
        oldValue = oldValue.replaceFirst("(\\d\\d):(\\d\\d)$", "$1$2");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(format.parse(oldValue));
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.SECOND, -1);
        return DATE_FORMATS[0].format(calendar.getTime()).replaceFirst("(\\d\\d)(\\d\\d)$", "$1:$2");
    }

    public void addToClassPath(File libDir, List<File> jars) {
        List<Library> allLibraries = getLibraries();
        if (!MCPatcherUtils.isNullOrEmpty(allLibraries)) {
            for (Library l : allLibraries) {
                l.addToClassPath(libDir, jars);
            }
        }
    }

    public void unpackNatives(File libDir, File destDir) throws IOException {
        List<Library> allLibraries = getLibraries();
        if (!MCPatcherUtils.isNullOrEmpty(allLibraries)) {
            destDir.mkdirs();
            for (Library l : allLibraries) {
                l.unpackNatives(libDir, destDir);
            }
        }
    }

    public void fetchLibraries(File libDir) throws PatcherException {
        List<Library> allLibraries = getLibraries();
        if (!MCPatcherUtils.isNullOrEmpty(allLibraries)) {
            for (Library l : allLibraries) {
                if (!l.exclude()) {
                    l.fetch(libDir);
                }
            }
        }
    }

    public void setGameArguments(Map<String, String> args) {
        args.put("version_name", id);
        args.put("assets_root", MCPatcherUtils.getMinecraftPath("assets").getPath());
        args.put("game_assets", MCPatcherUtils.getMinecraftPath("assets", "virtual", "legacy").getPath());
        args.put("assets_index_name", assets);
    }

    public void addGameArguments(Map<String, String> args, List<String> cmdLine) {
        String argTemplate;
        if (!minecraftArguments.isEmpty()) {
            argTemplate = minecraftArguments;
        } else if (LEGACY.equalsIgnoreCase(processArguments)) {
            argTemplate = LEGACY_VALUE;
        } else if (USERNAME_SESSION.equalsIgnoreCase(processArguments)) {
            argTemplate = USERNAME_SESSION_VALUE;
        } else {
            argTemplate = USERNAME_SESSION_VERSION_VALUE;
        }
        String[] argSplit = argTemplate.split("\\s+");
        for (int i = 0; i < argSplit.length; i++) {
            String s0 = argSplit[i];
            String s1 = i + 1 < argSplit.length ? argSplit[i + 1] : null;
            if (s0.equals("")) {
                // nothing
            } else if (s0.startsWith("--") && s1 != null && s1.startsWith("${") && s1.endsWith("}")) {
                String value = args.get(s1.substring(2, s1.length() - 1));
                if (value == null) {
                    Logger.log(Logger.LOG_MAIN, "WARNING: unknown argument %s %s", s0, s1);
                } else {
                    cmdLine.add(s0);
                    cmdLine.add(value);
                }
                i++;
            } else if (s0.startsWith("${") && s0.endsWith("}")) {
                String value = args.get(s0.substring(2, s0.length() - 1));
                cmdLine.add(value == null ? "" : value);
            } else {
                cmdLine.add(s0);
            }
        }
    }

    public String getMainClass() {
        return mainClass;
    }

    @Override
    public int compareTo(Version o) {
        MinecraftVersion v1 = MinecraftVersion.parseVersion(getId());
        MinecraftVersion v2 = MinecraftVersion.parseVersion(o.getId());
        if (v1 != null && v2 != null) {
            return v1.compareTo(v2);
        } else if (v1 != null) {
            return 1;
        } else if (v2 != null) {
            return -1;
        } else {
            return getId().compareTo(o.getId());
        }
    }
}
