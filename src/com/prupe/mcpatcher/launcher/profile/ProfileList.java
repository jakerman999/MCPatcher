package com.prupe.mcpatcher.launcher.profile;

import com.google.gson.Gson;
import com.prupe.mcpatcher.Config;
import com.prupe.mcpatcher.JsonUtils;
import com.prupe.mcpatcher.MCPatcherUtils;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.time.ZonedDateTime;


public class ProfileList {
    Map<String, Profile> profiles = new HashMap<>();
    String clientToken;
    Map<String, Authentication> authenticationDatabase = new HashMap<>();
    Map<String, Object> launcherVersion;
    Map<String, Object> settings;
    String analyticsToken;
    int analyticsFailcount;

    //String selectedProfile;
    Map<String, String> selectedUser;

    public static ProfileList getProfileList() {
        return JsonUtils.parseJson(getProfilesPath(), ProfileList.class);
    }

    public static File getProfilesPath() {
        return MCPatcherUtils.getMinecraftPath(Config.LAUNCHER_JSON);
    }

    private ProfileList() {
    }

    @Override
    public String toString() {
        return String.format("ProfileList{%d profiles, selectedProfile=%s}", profiles.size(), getSelectedProfile());
    }

    public String getSelectedProfile() {
        String selectedProfile = "";

        ZonedDateTime newest = ZonedDateTime.parse("1970-01-01T00:00:00.0000000Z");
        for (Profile p : profiles.values()) {
            ZonedDateTime  lastUsed = ZonedDateTime.parse(p.lastUsed);
            if (lastUsed.isAfter(newest)) {
                newest = lastUsed;
                selectedProfile = p.name;
            }
        }


        return selectedProfile;
    }

    public Profile getProfile(String name) {
        return profiles.get(name);
    }

    public Map<String, Profile> getProfiles() {
        return profiles;
    }

    public void dump(PrintStream output) {
        output.println(toString());
        Gson gson = JsonUtils.newGson();
        gson.toJson(this, ProfileList.class, output);
        output.println();
    }
}
