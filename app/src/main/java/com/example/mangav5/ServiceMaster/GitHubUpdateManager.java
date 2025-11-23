package com.example.mangav5.ServiceMaster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubUpdateManager {
    private final Activity activity;
    private final String repoOwner;
    private final String repoName;
    private final String TAG = "GitHubUpdateManager";

    public GitHubUpdateManager(Activity activity, String repoOwner, String repoName) {
        this.activity = activity;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public void checkForUpdate() {
        Log.d(TAG, "Starting checkForUpdate()");

        new Thread(() -> {
            try {
                String apiUrl = "https://api.github.com/repos/" + repoOwner + "/" + repoName + "/releases/latest";
                Log.d(TAG, "GitHub API URL: " + apiUrl);

                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                reader.close();

                Log.d(TAG, "GitHub response: " + result.toString());

                JSONObject release = new JSONObject(result.toString());
                String latestVersion = release.getString("tag_name");
                Log.d(TAG, "Latest version: " + latestVersion);

                PackageInfo pInfo = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
                String currentVersion = pInfo.versionName;
                Log.d(TAG, "Current version: " + currentVersion);

                boolean updateAvailable = !currentVersion.equals(latestVersion);
                String apkUrl = null;

                if (release.has("assets") && release.getJSONArray("assets").length() > 0) {
                    apkUrl = release.getJSONArray("assets").getJSONObject(0).getString("browser_download_url");
                    Log.d(TAG, "APK URL found: " + apkUrl);
                } else {
                    Log.d(TAG, "No APK attached in release assets");
                }

                String finalApkUrl = apkUrl;
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (updateAvailable && finalApkUrl != null) {
                        Log.d(TAG, "Update available, prompting user");
                        promptUpdate(finalApkUrl, latestVersion);
                    } else {
                        Log.d(TAG, "No update available");
                        Toast.makeText(activity, "You already have the latest version", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (FileNotFoundException e) {
                Log.e(TAG, "No release found on GitHub", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(activity, "No release found on GitHub", Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error checking for update", e);
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(activity, "Failed to check for update", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void promptUpdate(String apkUrl, String latestVersion) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Available")
                .setMessage("A new version (" + latestVersion + ") is available. Do you want to update?")
                .setPositiveButton("Yes", (dialog, which) -> downloadApk(apkUrl))
                .setNegativeButton("No", null)
                .setCancelable(false)
                .show();
    }

    private void downloadApk(String apkUrl) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Downloading update...");
            request.setDescription("Please wait...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MangaAkiku.apk");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setMimeType("application/vnd.android.package-archive");

            DownloadManager manager = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);

            // Optionally, prompt installation automatically when download starts
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Uri apkUri = manager.getUriForDownloadedFile(downloadId);
                installApk(apkUri);
            }, 3000);

        } catch (Exception e) {
            Log.e(TAG, "Error downloading APK", e);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(activity, "Failed to download update", Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void installApk(Uri apkUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Error installing APK", e);
            new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(activity, "Failed to install APK", Toast.LENGTH_SHORT).show()
            );
        }
    }
}
