package com.spse.gameresolutionchanger;

import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ExecuteADBCommands {
    private static final String TAG = "GRS_SHELL";

    private ExecuteADBCommands() {
        // Utility class
    }

    public static boolean canRunRootCommands() {
        Process suProcess = null;
        try {
            suProcess = Runtime.getRuntime().exec("su");

            try (DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                 BufferedReader reader = new BufferedReader(new InputStreamReader(suProcess.getInputStream()))) {

                os.writeBytes("id\n");
                os.writeBytes("exit\n");
                os.flush();

                String currUid = reader.readLine();
                boolean rootGranted = currUid != null && currUid.contains("uid=0");
                Log.d(TAG, rootGranted ? "Root access granted" : "Root access rejected: " + currUid);
                return rootGranted;
            }
        } catch (Exception e) {
            Log.d(TAG, "Root access unavailable: " + e.getMessage());
            return false;
        } finally {
            if (suProcess != null) {
                suProcess.destroy();
            }
        }
    }

    public static boolean execute(String command, boolean isSuperUser) {
        return execute(new ArrayList<>(Collections.singletonList(command)), isSuperUser);
    }

    public static boolean execute(List<String> commands, boolean isSuperUser) {
        if (commands == null || commands.isEmpty()) {
            return false;
        }

        Process process = null;
        try {
            // Android 15 compatibility: never exec("").
            // Non-root mode runs commands through app UID shell; WRITE_SECURE_SETTINGS still needs adb grant.
            process = Runtime.getRuntime().exec(isSuperUser ? "su" : "/system/bin/sh");

            try (DataOutputStream os = new DataOutputStream(process.getOutputStream())) {
                for (String command : commands) {
                    if (command == null || command.trim().isEmpty()) {
                        continue;
                    }
                    os.writeBytes(command + "\n");
                    os.flush();
                }
                os.writeBytes("exit\n");
                os.flush();
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;
            if (!success) {
                Log.w(TAG, "Command failed, exitCode=" + exitCode + ", root=" + isSuperUser + ", commands=" + commands);
            }
            return success;
        } catch (IOException e) {
            Log.w(TAG, "Cannot start shell", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.w(TAG, "Shell execution interrupted", e);
        } catch (SecurityException e) {
            Log.w(TAG, "Shell execution blocked", e);
        } catch (Exception e) {
            Log.w(TAG, "Shell execution error", e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return false;
    }
}
