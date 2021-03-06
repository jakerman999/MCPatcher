package com.prupe.mcpatcher;

import com.prupe.mcpatcher.converter.TexturePackConverter15;
import com.prupe.mcpatcher.converter.TexturePackConverter16;
import javassist.bytecode.ClassFile;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Main program entry point.
 */
final public class MCPatcher {
    /**
     * Major MCPatcher version number
     */
    public static final int MAJOR_VERSION = 5;
    /**
     * Minor MCPatcher version number
     */
    public static final int MINOR_VERSION = 0;
    /**
     * MCPatcher release number
     */
    public static final int RELEASE_VERSION = 4;
    /**
     * MCPatcher patch level
     */
    public static final int PATCH_VERSION = 0;
    /**
     * MCPatcher beta version if > 0
     */
    public static final int BETA_VERSION = 2;
    /**
     * MCPatcher version as a string
     */
    public static final String VERSION_STRING =
        String.format("%d.%d.%d", MAJOR_VERSION, MINOR_VERSION, RELEASE_VERSION) +
            (PATCH_VERSION > 0 ? String.format("_%02d", PATCH_VERSION) : "") +
            (BETA_VERSION > 0 ? "-beta" + BETA_VERSION : "");

    private static final String OVERRIDE_VERSION_STRING = null;

    static final String DISPLAY_VERSION_STRING = OVERRIDE_VERSION_STRING == null ?
        VERSION_STRING : OVERRIDE_VERSION_STRING;

    public static final String API_VERSION = "20140413";

    static ProfileManager profileManager;
    static MinecraftJar minecraft = null;
    static ModList modList;
    static final Properties patcherProperties = new Properties();
    private static final Set<String> modifiedClasses = new HashSet<String>();
    private static final Set<String> addedClasses = new HashSet<String>();
    private static final Set<String> conflictClasses = new HashSet<String>();

    private static boolean ignoreSavedMods;
    private static boolean ignoreBuiltInMods;
    private static boolean ignoreCustomMods;
    private static boolean ignoreImpliedMods;
    private static boolean enableAllMods;
    static boolean showInternal;
    static boolean experimentalMods;

    static UserInterface ui;

    private MCPatcher() {
    }

    /**
     * MCPatcher entry point.
     * <p/>
     * Valid parameters:<br>
     * -version: print version string and exit<br>
     * -loglevel n: set log level to n (0-7)<br>
     * -mcdir path: specify path to minecraft<br>
     * -auto: apply all applicable mods to the default minecraft.jar and exit (no GUI)<br>
     * -profile: select specified profile<br>
     * -ignoresavedmods: do not load mods from mcpatcher.xml<br>
     * -ignorebuiltinmods: do not load mods built into mcpatcher<br>
     * -ignorecustommods: do not load mods from the mcpatcher-mods directory<br>
     * -ignoreimpliedmods: do not load handlers for special mods like forge<br>
     * -enableallmods: enable all valid mods instead of selected mods from last time<br>
     * -showinternal: show hidden "internal" mods<br>
     * -experimental: load mods considered "experimental"<br>
     * -dumpjson: read contents of .json files and exit<br>
     * -dumplibs: dump libraries from specified version in a format suitable for eclipse<br>
     * -noproxy: do use system proxy settings<br>
     * -convert15 &lt;path&gt;: convert a texture pack from 1.4 to 1.5<br>
     * -convert16 &lt;path&gt;: convert a texture pack from 1.5 to 1.6<br>
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int exitStatus = 1;
        boolean guiEnabled = true;
        String enteredMCDir = null;
        String profile = null;
        String mcVersion = null;
        boolean dumpJson = false;
        boolean dumpLibs = false;
        boolean useSystemProxy = true;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-loglevel") && i + 1 < args.length) {
                i++;
                try {
                    Logger.setLogLevel(Integer.parseInt(args[i]));
                } catch (NumberFormatException e) {
                }
            } else if (args[i].equals("-version")) {
                System.out.println(DISPLAY_VERSION_STRING);
                System.exit(0);
            } else if (args[i].equals("-mcdir") && i + 1 < args.length) {
                i++;
                enteredMCDir = args[i];
            } else if (args[i].equals("-auto")) {
                guiEnabled = false;
            } else if (args[i].equals("-profile") && i + 1 < args.length) {
                i++;
                profile = args[i].trim();
            } else if (args[i].equals("-mcversion") && i + 1 < args.length) {
                i++;
                mcVersion = args[i].trim();
            } else if (args[i].equals("-ignoresavedmods")) {
                ignoreSavedMods = true;
            } else if (args[i].equals("-ignorebuiltinmods")) {
                ignoreBuiltInMods = true;
            } else if (args[i].equals("-ignorecustommods")) {
                ignoreCustomMods = true;
            } else if (args[i].equals("-ignoreimpliedmods")) {
                ignoreImpliedMods = true;
            } else if (args[i].equals("-enableallmods")) {
                enableAllMods = true;
            } else if (args[i].equals("-showinternal")) {
                showInternal = true;
            } else if (args[i].equals("-experimental")) {
                experimentalMods = true;
            } else if (args[i].equals("-dumpjson")) {
                guiEnabled = false;
                dumpJson = true;
            } else if (args[i].equals("-dumplibs")) {
                guiEnabled = false;
                dumpLibs = true;
            } else if (args[i].equals("-noproxy")) {
                useSystemProxy = false;
            } else if (args[i].equals("-convert15") && i + 1 < args.length) {
                i++;
                try {
                    if (new TexturePackConverter15(new File(args[i])).convert(new UserInterface.CLI())) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(1);
            } else if (args[i].equals("-convert16") && i + 1 < args.length) {
                i++;
                try {
                    if (new TexturePackConverter16(new File(args[i])).convert(new UserInterface.CLI())) {
                        System.exit(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(1);
            }
        }

        if (useSystemProxy) {
            System.setProperty("java.net.useSystemProxies", "true");
        }

        if (guiEnabled) {
            ui = new MainForm();
        } else {
            ui = new UserInterface.CLI();
        }

        if (!ui.locateMinecraftDir(enteredMCDir)) {
            System.exit(exitStatus);
        }
        Config config = Config.getInstance();
        profileManager = new ProfileManager(config);
        profileManager.setRemote(guiEnabled && config.fetchRemoteVersionList);

        if (dumpJson) {
            try {
                profileManager.refresh(ui);
                profileManager.dumpJson(System.out);
                exitStatus = 0;
            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.exit(exitStatus);
        }

        if (dumpLibs) {
            try {
                profileManager.refresh(ui);
                profileManager.dumpLibs(System.out, mcVersion);
            } catch (Throwable e) {
                e.printStackTrace();
            }
            System.exit(exitStatus);
        }

        ui.show();

        Util.logOSInfo();

        if (!MCPatcherUtils.isNullOrEmpty(profile)) {
            if (config.profiles.containsKey(profile)) {
                config.selectedProfile = profile;
            } else if (ui.shouldExit()) {
                System.out.printf("ERROR: profile '%s' not found\n", profile);
                System.exit(exitStatus);
            }
            if (!MCPatcherUtils.isNullOrEmpty(mcVersion)) {
                try {
                    profileManager.refresh(ui);
                    if (profileManager.getInputVersions().contains(mcVersion)) {
                        profileManager.selectInputVersion(mcVersion);
                    } else if (ui.shouldExit()) {
                        System.out.printf("ERROR: local version '%s' not found\n", mcVersion);
                        System.exit(exitStatus);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    if (ui.shouldExit()) {
                        System.exit(exitStatus);
                    }
                }
            }
        }
        if (!VERSION_STRING.equals(config.patcherVersion)) {
            config.patcherVersion = VERSION_STRING;
            config.betaWarningShown = false;
        }
        if (BETA_VERSION > 0 && !config.betaWarningShown) {
            ui.showBetaWarning();
            config.betaWarningShown = true;
        }

        if (ui.go(profileManager)) {
            exitStatus = 0;
        }

        if (ui.shouldExit()) {
            saveProperties();
            System.exit(exitStatus);
        }
    }

    static void saveProperties() {
        if (!ignoreSavedMods && modList != null) {
            modList.updateProperties();
            Config.save();
        }
    }

    static void checkInterrupt() throws InterruptedException {
        Thread.sleep(0);
    }

    static void refreshMinecraftPath() throws Exception {
        if (minecraft != null) {
            minecraft.closeStreams();
            minecraft = null;
        }
        try {
            ui.setStatusText("Opening %s", profileManager.getInputJar().getName());
            minecraft = new MinecraftJar(profileManager);
            minecraft.logVersion();
        } catch (Exception e) {
            minecraft = null;
            throw e;
        }
    }

    static void refreshModList() throws Exception {
        ui.setStatusText("Loading mods...");
        if (modList != null) {
            modList.close();
        }
        patcherProperties.clear();
        modList = new ModList(minecraft.getVersion());
        if (!ignoreSavedMods) {
            modList.loadSavedMods();
        }
        if (!ignoreBuiltInMods) {
            modList.loadBuiltInMods(false);
        }
        if (!ignoreCustomMods) {
            modList.loadCustomMods(MCPatcherUtils.getMinecraftPath("mcpatcher-mods"));
        }
        if (!ignoreImpliedMods) {
            try {
                modList.loadImpliedMods(profileManager, ui);
            } catch (Throwable e) {
                Logger.log(e);
            }
        }
    }

    static void checkModApplicability() throws IOException, InterruptedException {
        JarFile origJar = minecraft.getInputJar();
        mapModClasses(origJar);
        mapModDependentClasses(origJar);
        checkAllClassesMapped();
        mapModClassMembers(origJar);
        resolveModDependencies();
        printModList();
        modList.enableValidMods(enableAllMods);
    }

    private static void mapModClasses(JarFile origJar) throws IOException, InterruptedException {
        int totalFiles = origJar.size();
        Logger.log(Logger.LOG_JAR);
        Logger.log(Logger.LOG_JAR, "Analyzing %s (%d files)", origJar.getName(), totalFiles);
        ui.setStatusText("Analyzing %s...", minecraft.getInputFile().getName());

        int procFiles = 0;
        for (JarEntry entry : Collections.list(origJar.entries())) {
            ui.updateProgress(++procFiles, origJar.size());
            String name = entry.getName();

            if (MinecraftJar.isClassFile(name)) {
                ClassFile classFile = minecraft.getClassFile(entry);

                for (Mod mod : modList.getAll()) {
                    for (ClassMod classMod : mod.getClassMods()) {
                        if (!classMod.prerequisiteClasses.isEmpty()) {
                            continue;
                        }
                        try {
                            if (classMod.matchClassFile(name, classFile)) {
                                checkInterrupt();
                                if (!classMod.global) {
                                    Logger.log(Logger.LOG_CLASS, "%s matches %s", classMod.getDeobfClass(), name);
                                    for (Map.Entry<String, ClassMap.MemberEntry> e : mod.classMap.getMethodMap(classMod.getDeobfClass()).entrySet()) {
                                        Logger.log(Logger.LOG_METHOD, "%s matches %s %s", e.getKey(), e.getValue().name, e.getValue().type);
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            throw e;
                        } catch (Throwable e) {
                            classMod.addError(e.toString());
                            Logger.log(e);
                        }
                    }
                }
            }
        }
    }

    private static void mapModDependentClasses(JarFile origJar) throws IOException, InterruptedException {
        ArrayList<ClassMod> todoList = new ArrayList<ClassMod>();
        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.getClassMods()) {
                if (classMod.okToApply() && !classMod.prerequisiteClasses.isEmpty()) {
                    todoList.add(classMod);
                }
            }
        }
        int numTodo = todoList.size();
        if (numTodo > 0) {
            Logger.log(Logger.LOG_JAR);
            Logger.log(Logger.LOG_JAR, "Analyzing %s (%d dependent classes)", origJar.getName(), numTodo);
            ui.setStatusText("Mapping remaining classes...");
            ui.updateProgress(0, numTodo);
            boolean keepGoing = true;
            for (int pass = 2; keepGoing && !todoList.isEmpty() && pass < 100; pass++) {
                keepGoing = mapModDependentClasses(origJar, todoList, pass);
                ui.updateProgress(numTodo - todoList.size(), numTodo);
            }
            for (ClassMod classMod : todoList) {
                if (!classMod.prerequisiteClasses.isEmpty()) {
                    classMod.addError("prerequisite class " + classMod.prerequisiteClasses.get(0) + " not matched");
                }
            }
        }
    }

    private static boolean mapModDependentClasses(JarFile origJar, ArrayList<ClassMod> todoList, int pass) throws IOException, InterruptedException {
        boolean progress = false;
        boolean done = true;
        classMod:
        for (Iterator<ClassMod> iterator = todoList.iterator(); iterator.hasNext(); ) {
            ClassMod classMod = iterator.next();
            if (!classMod.okToApply()) {
                continue;
            }
            Mod mod = classMod.mod;
            HashMap<String, String> classMap = mod.classMap.getClassMap();
            for (Iterator<String> iterator1 = classMod.prerequisiteClasses.iterator(); iterator1.hasNext(); ) {
                String reqClass = iterator1.next();
                done = false;
                if (classMap.get(reqClass) == null) {
                    continue classMod;
                }
                for (ClassMod classMod1 : mod.classMods) {
                    if (classMod1.getDeobfClass().equals(reqClass) && classMod1.targetClasses.size() != 1) {
                        continue classMod;
                    }
                }
                iterator1.remove();
            }
            List<JarEntry> candidateEntries;
            String targetClass = classMap.get(classMod.getDeobfClass());
            if (targetClass == null) {
                candidateEntries = Collections.list(origJar.entries());
            } else {
                JarEntry entry = origJar.getJarEntry(ClassMap.classNameToFilename(targetClass));
                if (entry == null) {
                    classMod.addError("maps to non-existent class " + targetClass);
                    continue;
                }
                candidateEntries = new ArrayList<JarEntry>();
                candidateEntries.add(entry);
            }
            for (JarEntry entry : candidateEntries) {
                if (!MinecraftJar.isClassFile(entry.getName())) {
                    continue;
                }
                ClassFile classFile = minecraft.getClassFile(entry);
                try {
                    if (classMod.matchClassFile(entry.getName(), classFile)) {
                        checkInterrupt();
                        if (!classMod.global) {
                            Logger.log(Logger.LOG_CLASS, "%s matches %s (pass %d)", classMod.getDeobfClass(), entry.getName(), pass);
                            for (Map.Entry<String, ClassMap.MemberEntry> e : mod.classMap.getMethodMap(classMod.getDeobfClass()).entrySet()) {
                                Logger.log(Logger.LOG_METHOD, "%s matches %s %s", e.getKey(), e.getValue().name, e.getValue().type);
                            }
                        }
                        progress = true;
                    }
                } catch (InterruptedException e) {
                    throw e;
                } catch (Throwable e) {
                    classMod.addError(e.toString());
                    Logger.log(e);
                }
            }
            if (progress) {
                iterator.remove();
            }
        }
        return progress && !done;
    }

    private static void checkAllClassesMapped() throws IOException, InterruptedException {
        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.getClassMods()) {
                classMod.pruneTargetClasses();
                if (!classMod.global && classMod.okToApply()) {
                    checkInterrupt();
                    if (classMod.targetClasses.size() > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (String s : classMod.targetClasses) {
                            sb.append(" ");
                            sb.append(s);
                        }
                        classMod.addError(String.format("multiple classes matched:%s", sb.toString()));
                        Logger.log(Logger.LOG_MOD, "multiple classes matched %s:%s", classMod.getDeobfClass(), sb.toString());
                    } else if (classMod.targetClasses.size() == 0) {
                        String bestInfo;
                        if (classMod.bestMatch == null) {
                            bestInfo = "";
                        } else {
                            bestInfo = String.format(" (best match: %s, %d signatures [failed: %s])",
                                classMod.bestMatch, classMod.bestMatchCount + 1, classMod.classSignatures.get(classMod.bestMatchCount + 1).mapSource
                            );
                        }
                        classMod.addError("no classes matched" + bestInfo);
                        Logger.log(Logger.LOG_MOD, "no classes matched %s%s", classMod.getDeobfClass(), bestInfo);
                    }
                }
            }
        }
    }

    private static void mapModClassMembers(JarFile origJar) throws IOException, InterruptedException {
        Logger.log(Logger.LOG_JAR);
        Logger.log(Logger.LOG_JAR, "Analyzing %s (methods and fields)", origJar.getName());
        int numMappings = 0;
        int mappingProgress = 0;
        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.classMods) {
                if (!classMod.global && classMod.okToApply()) {
                    numMappings += classMod.memberMappers.size();
                }
            }
        }
        ui.setStatusText("Mapping class members...");
        ui.updateProgress(mappingProgress, numMappings);
        for (Mod mod : modList.getAll()) {
            for (ClassMod classMod : mod.getClassMods()) {
                checkInterrupt();
                try {
                    if (!classMod.global && classMod.okToApply()) {
                        String name = ClassMap.classNameToFilename(classMod.getTargetClasses().get(0));
                        Logger.log(Logger.LOG_CLASS, "%s (%s)", classMod.getDeobfClass(), name);
                        ClassFile classFile = minecraft.getClassFile(new ZipEntry(name));
                        classMod.addToConstPool = false;
                        classMod.mapClassMembers(name, classFile);
                        mappingProgress += classMod.memberMappers.size();
                        ui.updateProgress(mappingProgress, numMappings);
                    }
                } catch (Throwable e) {
                    classMod.addError(e.toString());
                    Logger.log(e);
                }
            }
        }
    }

    private static void resolveModDependencies() throws IOException, InterruptedException {
        boolean didSomething = true;
        while (didSomething) {
            didSomething = false;
            for (Mod mod : modList.getAll()) {
                if (mod.okToApply()) {
                    for (Mod.Dependency dep : mod.dependencies) {
                        Mod dmod = modList.get(dep.name);
                        checkInterrupt();
                        if (dep.required && (dmod == null || !dmod.okToApply())) {
                            mod.addError(String.format("requires %s, which cannot be applied", dep.name));
                            if (dmod != null) {
                                for (String err : dmod.getErrors()) {
                                    mod.addError(err);
                                }
                            }
                            didSomething = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void printModList() {
        Logger.log(Logger.LOG_MAIN);
        Logger.log(Logger.LOG_MAIN, "%d available mods:", modList.getAll().size());
        for (Mod mod : modList.getAll()) {
            Logger.log(Logger.LOG_MAIN, "[%3s] %s %s - %s", (mod.okToApply() ? "YES" : "NO"), mod.getName(), mod.getVersion(), mod.getDescription());
            for (ClassMod cm : mod.classMods) {
                if (cm.okToApply()) {
                } else if (cm.targetClasses.size() == 0) {
                    Logger.log(Logger.LOG_MOD, "no classes matched %s", cm.getDeobfClass());
                } else if (cm.targetClasses.size() > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (String s : cm.targetClasses) {
                        sb.append(" ");
                        sb.append(s);
                    }
                    Logger.log(Logger.LOG_MOD, "multiple classes matched %s:%s", cm.getDeobfClass(), sb.toString());
                }
            }
        }
    }

    static void showClassMaps(PrintStream out, boolean extended) {
        if (minecraft == null) {
            out.println("No minecraft jar selected.");
            out.println("Click Browse to choose the input file.");
            out.println();
        } else {
            for (Mod mod : modList.getAll()) {
                if (!mod.getClassMap().getClassMap().isEmpty()) {
                    out.printf("%s\n", mod.getName());
                    mod.getClassMap().print(out, "    ", extended);
                    out.println();
                }
            }
        }
    }

    static void showPatchResults(PrintStream out) {
        if (modList == null || !modList.isApplied()) {
            out.println("No patches applied yet.");
            out.println("Click Patch on the Mods tab.");
            out.println();
        } else {
            for (Mod mod : modList.getSelected()) {
                if (mod.getClassMods().size() == 0 && mod.filesAdded.isEmpty()) {
                    continue;
                }
                out.printf("%s\n", mod.getName());
                ArrayList<Map.Entry<String, String>> filesAdded = new ArrayList<Map.Entry<String, String>>();
                filesAdded.addAll(mod.filesAdded.entrySet());
                Collections.sort(filesAdded, new Comparator<Map.Entry<String, String>>() {
                    private void split(String s, String[] v) {
                        int slash = s.lastIndexOf('/');
                        if (slash >= 0) {
                            v[0] = s.substring(0, slash);
                            v[1] = s.substring(slash + 1);
                        } else {
                            v[0] = "";
                            v[1] = s;
                        }
                    }

                    public int compare(Map.Entry<String, String> o1, Map.Entry<String, String> o2) {
                        String[] s1 = new String[2];
                        String[] s2 = new String[2];
                        split(o1.getKey(), s1);
                        split(o2.getKey(), s2);
                        int result = s1[0].compareTo(s2[0]);
                        if (result != 0) {
                            return result;
                        }
                        return s1[1].compareTo(s2[1]);
                    }
                });
                for (Map.Entry<String, String> entry : filesAdded) {
                    out.printf("    %s %s\n", entry.getValue(), entry.getKey());
                }
                for (String name : mod.filesToAdd) {
                    if (!mod.filesAdded.containsKey(name)) {
                        out.printf("    WARNING: %s not added (possible conflict)\n", name);
                    }
                }
                ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
                classMods.addAll(mod.getClassMods());
                Collections.sort(classMods, new Comparator<ClassMod>() {
                    public int compare(ClassMod o1, ClassMod o2) {
                        return o1.getDeobfClass().compareTo(o2.getDeobfClass());
                    }
                });
                for (ClassMod classMod : classMods) {
                    List<String> tc = classMod.getTargetClasses();
                    out.printf("    %s", classMod.getDeobfClass());
                    if (tc.size() == 0) {
                    } else if (tc.size() == 1) {
                        out.printf(" (%s)", ClassMap.classNameToFilename(tc.get(0)));
                    } else {
                        out.print(" (multiple matches:");
                        List<String> patchedClasses = new ArrayList<String>();
                        for (ClassPatch classPatch : classMod.patches) {
                            patchedClasses.addAll(classPatch.patchedClasses);
                        }
                        Collections.sort(patchedClasses);
                        for (String s : patchedClasses) {
                            out.print(' ');
                            out.print(s);
                        }
                        out.print(")");
                    }
                    out.println();
                    ArrayList<Map.Entry<String, Integer>> sortedList = new ArrayList<Map.Entry<String, Integer>>();
                    for (int i = 0; i < classMod.patches.size(); i++) {
                        ClassPatch classPatch = classMod.patches.get(i);
                        if (classPatch.numMatches.isEmpty() && !classPatch.optional) {
                            String desc = null;
                            Throwable e = null;
                            try {
                                desc = classPatch.getDescription();
                            } catch (Throwable e1) {
                                e = e1;
                            }
                            if (desc == null) {
                                desc = String.format("patch %d (%s)", i, e == null ? "no description" : e.getMessage());
                            }
                            final String desc2 = desc;
                            sortedList.add(new Map.Entry<String, Integer>() {
                                public String getKey() {
                                    return desc2;
                                }

                                public Integer getValue() {
                                    return 0;
                                }

                                public Integer setValue(Integer value) {
                                    return value;
                                }
                            });
                        } else {
                            sortedList.addAll(classPatch.numMatches.entrySet());
                        }
                    }
                    Collections.sort(sortedList, new Comparator<Map.Entry<String, Integer>>() {
                        public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    });
                    for (Map.Entry<String, Integer> e : sortedList) {
                        out.printf("        [%d] %s\n", e.getValue(), e.getKey());
                    }
                }
                out.println();
            }
        }
    }

    static HashMap<String, ArrayList<Mod>> getConflicts() {
        modList.refreshInternalMods();
        HashMap<String, ArrayList<Mod>> conflicts = new HashMap<String, ArrayList<Mod>>();
        ArrayList<Mod> mods = modList.getSelected();
        conflictClasses.clear();
        for (Mod mod : mods) {
            for (String filename : mod.filesToAdd) {
                ArrayList<Mod> modArray = conflicts.get(filename);
                if (modArray == null) {
                    modArray = new ArrayList<Mod>();
                    conflicts.put(filename, modArray);
                }
                if (MinecraftJar.isClassFile(filename)) {
                    String className = ClassMap.filenameToClassName(filename);
                    for (Mod conflictMod : mods) {
                        if (conflictMod == mod) {
                            break;
                        }
                        for (ClassMod classMod : conflictMod.classMods) {
                            if (classMod.targetClasses.contains(className)) {
                                modArray.add(conflictMod);
                                conflictClasses.add(className);
                            }
                        }
                    }
                }
                modArray.add(mod);
            }
        }
        ArrayList<String> entriesToRemove = new ArrayList<String>();
        for (Map.Entry<String, ArrayList<Mod>> entry : conflicts.entrySet()) {
            if (entry.getValue().size() <= 1) {
                entriesToRemove.add(entry.getKey());
            }
        }
        for (String filename : entriesToRemove) {
            conflicts.remove(filename);
        }
        return conflicts;
    }

    static void patch() throws PatcherException {
        modList.refreshInternalMods();
        modList.setApplied(true);
        modifiedClasses.clear();
        addedClasses.clear();
        try {
            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Patching...");
            ui.setStatusText("Patching %s...", MCPatcher.minecraft.getOutputFile().getName());

            for (Mod mod : modList.getAll()) {
                mod.resetCounts();
                Logger.log(Logger.LOG_MAIN, "[%s] %s %s - %s",
                    (mod.isEnabled() && mod.okToApply() ? "*" : " "), mod.getName(), mod.getVersion(), mod.getDescription()
                );
            }

            if (!modList.getSelected().isEmpty()) {
                Logger.log(Logger.LOG_MAIN);
            }

            profileManager.getOutputJar().getParentFile().mkdirs();
            applyMods();
            writeProperties();
            minecraft.checkOutput();
            minecraft.closeStreams();
            minecraft.clearClassFileCache();
            profileManager.createOutputProfile(modList.getOverrideVersionJson(), modList.getExtraJavaArguments(), modList.getExtraLibraries());

            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Done!");
        } catch (Throwable e) {
            Logger.log(Logger.LOG_MAIN);
            Logger.log(Logger.LOG_MAIN, "Restoring original profile due to previous error");
            unpatch();
            throw new PatcherException.PatchError(e);
        }
    }

    static void unpatch() {
        try {
            if (minecraft != null) {
                minecraft.closeStreams();
                minecraft.clearClassFileCache();
            }
            profileManager.deleteLocalVersion(profileManager.getOutputVersion());
            profileManager.deleteProfile(profileManager.getOutputProfile(), true);
        } catch (Throwable e) {
            Logger.log(e);
        }
    }

    private static void applyMods() throws Exception {
        JarFile origJar = minecraft.getInputJar();
        JarOutputStream outputJar = minecraft.getOutputJar();

        int procFiles = 0;
        for (JarEntry entry : Collections.list(origJar.entries())) {
            checkInterrupt();
            ui.updateProgress(++procFiles, origJar.size());
            String name = entry.getName();
            boolean patched = false;

            if (MinecraftJar.isGarbageFile(name)) {
                continue;
            }
            if (entry.isDirectory()) {
                outputJar.putNextEntry(new ZipEntry(name));
                outputJar.closeEntry();
                continue;
            }

            Mod fromMod = null;
            for (Mod mod : modList.getSelected()) {
                if (mod.filesToAdd.contains(name)) {
                    fromMod = mod;
                }
            }

            InputStream inputStream;
            if (fromMod == null) {
                inputStream = origJar.getInputStream(entry);
            } else {
                inputStream = fromMod.openFile(name);
                if (inputStream == null) {
                    throw new IOException(String.format("could not open %s for %s", name, fromMod.getName()));
                }
                Logger.log(Logger.LOG_MOD, "replacing %s for %s", name, fromMod.getName());
                fromMod.filesAdded.put(name, "replaced");
            }

            if (MinecraftJar.isClassFile(name)) {
                ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
                ClassFile classFile = fromMod == null ? minecraft.getClassFile(entry) : new ClassFile(new DataInputStream(inputStream));
                String className = ClassMap.filenameToClassName(name);

                for (Mod mod : modList.getSelected()) {
                    int i = modList.indexOf(fromMod);
                    int j = modList.indexOf(mod);
                    if (j > 0 && i > j) {
                        continue;
                    }
                    for (ClassMod classMod : mod.getClassMods()) {
                        if (classMod.targetClasses.contains(className)) {
                            classMods.add(classMod);
                        }
                    }
                }

                patched = applyPatches(name, classFile, classMods);
                if (patched) {
                    outputJar.putNextEntry(new ZipEntry(name));
                    modifiedClasses.add(ClassMap.filenameToClassName(name));
                    classFile.compact();
                    classFile.write(new DataOutputStream(outputJar));
                    outputJar.closeEntry();
                }
            }

            if (!patched) {
                outputJar.putNextEntry(new ZipEntry(name));
                MCPatcherUtils.close(inputStream);
                if (fromMod == null) {
                    inputStream = origJar.getInputStream(entry);
                } else {
                    inputStream = fromMod.openFile(name);
                }
                Util.copyStream(inputStream, outputJar);
                outputJar.closeEntry();
            }

            MCPatcherUtils.close(inputStream);
        }

        HashMap<String, Mod> allFilesToAdd = new HashMap<String, Mod>();
        for (Mod mod : modList.getSelected()) {
            for (String name : mod.filesToAdd) {
                if (origJar.getEntry(name) == null) {
                    allFilesToAdd.put(name, mod);
                }
            }
        }
        if (!allFilesToAdd.isEmpty()) {
            ui.setStatusText("Adding files to %s...", minecraft.getOutputFile().getName());
            int filesAdded = 0;
            for (Mod mod : modList.getSelected()) {
                for (String name : mod.filesToAdd) {
                    if (mod == allFilesToAdd.get(name)) {
                        addFile(mod, name, outputJar);
                        ui.updateProgress(++filesAdded, allFilesToAdd.size());
                    }
                }
            }
        }
    }

    private static boolean addFile(Mod mod, String filename, JarOutputStream outputJar) throws Exception {
        String resource = "/" + filename;
        InputStream inputStream = mod.openFile(resource);
        if (inputStream == null) {
            throw new IOException(String.format("could not open %s for %s", resource, mod.getName()));
        }
        Logger.log(Logger.LOG_CLASS, "adding %s for %s", filename, mod.getName());

        try {
            outputJar.putNextEntry(new ZipEntry(filename));
            ClassMap classMap = mod.classMap;
            boolean directCopy = true;
            if (MinecraftJar.isClassFile(filename)) {
                ClassFile classFile = new ClassFile(new DataInputStream(inputStream));
                for (Mod mod1 : modList.getSelected()) {
                    for (ClassMod classMod : mod1.getClassMods()) {
                        if (classMod.matchAddedFiles && classMod.matchClassFile(filename, classFile)) {
                            classMod.targetClasses.add(classFile.getName());
                            ArrayList<ClassMod> classMods = new ArrayList<ClassMod>();
                            classMods.add(classMod);
                            if (applyPatches(filename, classFile, classMods)) {
                                directCopy = false;
                            }
                        }
                    }
                }
                if (!classMap.isEmpty()) {
                    directCopy = false;
                    classMap.apply(classFile);
                }
                if (directCopy) {
                    MCPatcherUtils.close(inputStream);
                    inputStream = mod.openFile(resource);
                    if (inputStream == null) {
                        directCopy = false;
                    }
                }
                if (!directCopy) {
                    classFile.compact();
                    classMap.stringReplace(classFile, outputJar);
                }
                addedClasses.add(ClassMap.filenameToClassName(filename));
            }
            if (directCopy) {
                Util.copyStream(inputStream, outputJar);
            }
            outputJar.closeEntry();
            mod.filesAdded.put(filename, "added");
        } catch (ZipException e) {
            if (!e.toString().contains("duplicate entry")) {
                throw e;
            }
        } finally {
            MCPatcherUtils.close(inputStream);
        }

        return true;
    }

    private static boolean applyPatches(String filename, ClassFile classFile, ArrayList<ClassMod> classMods) throws Exception {
        boolean patched = false;
        for (ClassMod cm : classMods) {
            checkInterrupt();
            if (cm.targetClasses.contains(classFile.getName())) {
                cm.addToConstPool = true;
                cm.classFile = classFile;
                cm.methodInfo = null;
                try {
                    cm.prePatch(filename, classFile);
                } finally {
                    cm.addToConstPool = false;
                }
                if (cm.patches.size() > 0) {
                    Logger.log(Logger.LOG_MOD, "applying %s patch to %s for mod %s", cm.getDeobfClass(), filename, cm.mod.getName());
                }
                for (ClassPatch cp : cm.patches) {
                    if (cp.apply(classFile)) {
                        cp.patchedClasses.add(classFile.getName());
                        patched = true;
                    }
                }
                cm.addToConstPool = true;
                try {
                    cm.postPatch(filename, classFile);
                } finally {
                    cm.addToConstPool = false;
                }
            }
        }
        return patched;
    }

    private static void writeProperties() throws IOException {
        patcherProperties.setProperty(Config.TAG_MINECRAFT_VERSION, minecraft.getVersion().getVersionString());
        patcherProperties.setProperty(Config.TAG_PATCHER_VERSION, MCPatcher.VERSION_STRING);
        modifiedClasses.addAll(conflictClasses);
        writeList(patcherProperties, Config.TAG_MODIFIED_CLASSES, modifiedClasses);
        writeList(patcherProperties, Config.TAG_ADDED_CLASSES, addedClasses);
        minecraft.writeProperties(patcherProperties);
    }

    private static void writeList(Properties properties, String propertyName, Collection<String> data) {
        StringBuilder sb = new StringBuilder();
        List<String> sortedList = new ArrayList<String>();
        sortedList.addAll(data);
        Collections.sort(sortedList);
        for (String s : sortedList) {
            sb.append(' ').append(ClassMap.filenameToClassName(s));
        }
        properties.setProperty(propertyName, sb.toString().trim());
    }
}
