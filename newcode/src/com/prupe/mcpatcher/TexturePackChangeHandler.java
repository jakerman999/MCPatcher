package com.prupe.mcpatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.src.*;

import java.io.*;
import java.util.*;
import java.util.zip.ZipFile;

abstract public class TexturePackChangeHandler {
    private static final MCLogger logger = MCLogger.getLogger("Texture Pack");

    private static final ArrayList<TexturePackChangeHandler> handlers = new ArrayList<TexturePackChangeHandler>();
    private static boolean initializing = true;
    private static boolean changing;
    private static long startTime;
    private static long startMem;

    private static final boolean autoRefreshTextures = Config.getBoolean("autoRefreshTextures", false);
    private static long lastCheckTime;

    private boolean updateNeeded;

    protected final String name;
    protected final int order;

    public TexturePackChangeHandler(String name, int order) {
        this.name = name;
        this.order = order;
    }

    public void initialize() {
        beforeChange();
        afterChange();
    }

    public void refresh() {
        beforeChange();
        afterChange();
    }

    abstract public void beforeChange();

    abstract public void afterChange();

    public void afterChange2() {
    }

    protected void setUpdateNeeded(boolean updateNeeded) {
        this.updateNeeded = updateNeeded;
    }

    public static void scheduleTexturePackRefresh() {
        MCPatcherUtils.getMinecraft().scheduleTexturePackRefresh();
    }

    public static void register(TexturePackChangeHandler handler) {
        if (handler != null) {
            if (Minecraft.getInstance().getResourceBundle() != null) {
                try {
                    logger.info("initializing %s...", handler.name);
                    handler.initialize();
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.severe("%s initialization failed", handler.name);
                }
            }
            handlers.add(handler);
            logger.fine("registered texture pack handler %s, priority %d", handler.name, handler.order);
            Collections.sort(handlers, new Comparator<TexturePackChangeHandler>() {
                public int compare(TexturePackChangeHandler o1, TexturePackChangeHandler o2) {
                    return o1.order - o2.order;
                }
            });
        }
    }

    public static void earlyInitialize(String className, String methodName) {
        try {
            logger.fine("calling %s.%s", className, methodName);
            Class.forName(className).getDeclaredMethod(methodName).invoke(null);
        } catch (Throwable e) {
        }
    }

    public static void checkForTexturePackChange() {
        for (TexturePackChangeHandler handler : handlers) {
            if (handler.updateNeeded) {
                handler.updateNeeded = false;
                try {
                    logger.info("refreshing %s...", handler.name);
                    handler.refresh();
                } catch (Throwable e) {
                    e.printStackTrace();
                    logger.severe("%s refresh failed", handler.name);
                }
            }
        }
    }

    public static void beforeChange1() {
        if (changing) {
            if (initializing) {
                logger.finer("skipping beforeChange1 because we are still initializing");
            } else {
                new RuntimeException("unexpected recursive call to TexturePackChangeHandler").printStackTrace();
            }
            return;
        }
        changing = true;
        startTime = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        startMem = runtime.totalMemory() - runtime.freeMemory();
        logger.fine("%s resource packs:", initializing ? "initializing" : "changing");
        for (IResourcePack pack : TexturePackAPI.getResourcePacks(null)) {
            logger.fine("resource pack: %s", pack);
        }

        for (TexturePackChangeHandler handler : handlers) {
            try {
                logger.info("refreshing %s (pre)...", handler.name);
                handler.beforeChange();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.beforeChange failed", handler.name);
            }
        }

        TextureManager textureManager = MCPatcherUtils.getMinecraft().getTextureManager();
        if (textureManager != null) {
            Set<ResourceAddress> texturesToUnload = new HashSet<ResourceAddress>();
            for (Map.Entry<ResourceAddress, ITexture> entry : textureManager.texturesByName.entrySet()) {
                ResourceAddress resource = entry.getKey();
                if (!resource.getPath().startsWith("dynamic/") && !TexturePackAPI.hasResource(resource)) {
                    texturesToUnload.add(resource);
                }
            }
            for (ResourceAddress resource : texturesToUnload) {
                TexturePackAPI.unloadTexture(resource);
            }
        }
    }

    public static void afterChange1() {
        if (initializing) {
            logger.finer("deferring afterChange1 because we are still initializing");
            initializing = false;
            return;
        }
        for (TexturePackChangeHandler handler : handlers) {
            try {
                logger.info("refreshing %s (post)...", handler.name);
                handler.afterChange();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.afterChange failed", handler.name);
            }
        }

        for (int i = handlers.size() - 1; i >= 0; i--) {
            TexturePackChangeHandler handler = handlers.get(i);
            try {
                handler.afterChange2();
            } catch (Throwable e) {
                e.printStackTrace();
                logger.severe("%s.afterChange2 failed", handler.name);
            }
        }

        System.gc();
        long timeDiff = System.currentTimeMillis() - startTime;
        Runtime runtime = Runtime.getRuntime();
        long memDiff = runtime.totalMemory() - runtime.freeMemory() - startMem;
        logger.info("done (%.3fs elapsed, mem usage %+.1fMB)\n", timeDiff / 1000.0, memDiff / 1048576.0);
        changing = false;
    }

    /*
    private static boolean openTexturePackFile(TexturePackCustom pack) {
        if (pack.zipFile == null) {
            return false;
        }
        if (pack.origZip != null) {
            return true;
        }
        InputStream input = null;
        OutputStream output = null;
        ZipFile newZipFile = null;
        try {
            pack.lastModified = pack.texturePackFile.lastModified();
            pack.tmpFile = File.createTempFile("tmpmc", ".zip");
            pack.tmpFile.deleteOnExit();
            MCPatcherUtils.close(pack.zipFile);
            input = new FileInputStream(pack.texturePackFile);
            output = new FileOutputStream(pack.tmpFile);
            byte[] buffer = new byte[65536];
            while (true) {
                int nread = input.read(buffer);
                if (nread <= 0) {
                    break;
                }
                output.write(buffer, 0, nread);
            }
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
            newZipFile = new ZipFile(pack.tmpFile);
            pack.origZip = pack.zipFile;
            pack.zipFile = newZipFile;
            newZipFile = null;
            logger.fine("copied %s to %s, lastModified = %d", pack.texturePackFile.getPath(), pack.tmpFile.getPath(), pack.lastModified);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            MCPatcherUtils.close(input);
            MCPatcherUtils.close(output);
            MCPatcherUtils.close(newZipFile);
        }
        return true;
    }

    private static void closeTexturePackFile(TexturePackCustom pack) {
        if (pack.origZip != null) {
            MCPatcherUtils.close(pack.zipFile);
            pack.zipFile = pack.origZip;
            pack.origZip = null;
            pack.tmpFile.delete();
            logger.fine("deleted %s", pack.tmpFile.getPath());
            pack.tmpFile = null;
        }
    }

    private static boolean checkFileChange(TexturePackList list, TexturePackCustom pack) {
        return false;
        if (!autoRefreshTextures || !openTexturePackFile(pack)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastCheckTime < 1000L) {
            return false;
        }
        lastCheckTime = now;
        long lastModified = pack.texturePackFile.lastModified();
        if (lastModified == pack.lastModified || lastModified == 0 || pack.lastModified == 0) {
            return false;
        }
        logger.finer("%s lastModified changed from %d to %d", pack.texturePackFile.getName(), pack.lastModified, lastModified);
        ZipFile tmpZip = null;
        try {
            tmpZip = new ZipFile(pack.texturePackFile);
        } catch (IOException e) {
            // file is still being written
            return false;
        } finally {
            MCPatcherUtils.close(tmpZip);
        }
        closeTexturePackFile(pack);
        list.updateAvailableTexturePacks();
        scheduleTexturePackRefresh();
        return true;
    }
    */
}
