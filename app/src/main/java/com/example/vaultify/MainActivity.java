package com.example.vaultify;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import Recycler.FolderAdapter;
import model.Folder;
import okhttp3.*;
import com.example.vaultify.requestActivity.RequestsActivity;

import com.example.vaultify.Login_signup.LoginActivity;
import com.example.vaultify.dashboard.DashboardActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    ArrayList<Folder> folderList = new ArrayList<>();
    FolderAdapter adapter;
    //String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String token = prefs.getString("token", null);
        // userId = extractUserIdFromToken(token);

        if (token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
        TextView userEmail = findViewById(R.id.userEmail);
        String email = prefs.getString("email", "User");

        userEmail.setText("Welcome: " + email);
        Button logoutBtn = findViewById(R.id.logoutBtn);

        logoutBtn.setOnClickListener(v -> {
            getSharedPreferences("app", MODE_PRIVATE)
                    .edit()
                    .clear()
                    .apply();

            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        fetchFolders();
        recyclerView = findViewById(R.id.folderRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

//        adapter = new FolderAdapter(folderList, folder -> {
//            sendAccessRequest(folder.folderId, folder.ownerId);
//        });
        adapter = new FolderAdapter(folderList,
                new FolderAdapter.OnFolderActionListener() {

                    @Override
                    public void onFolderClick(Folder folder) {

                        SharedPreferences prefs =
                                getSharedPreferences("app", MODE_PRIVATE);

                        String token = prefs.getString("token", null);

                        String currentUserId =
                                extractUserIdFromToken(token);

                        // ✅ PUBLIC
                        if (folder.isPublic) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Opening public folder",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // ✅ OWNER
                        if (folder.ownerId.equals(currentUserId)) {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Opening your folder",
                                    Toast.LENGTH_SHORT
                            ).show();

                            return;
                        }

                        // ✅ CHECK SHARED ACCESS
                        checkPermissionAndOpen(folder);
                    }

                    @Override
                    public void onRequestClick(Folder folder, int position) {

                        sendAccessRequest(
                                folder,
                                position
                        );
                    }
                });
        recyclerView.setAdapter(adapter);
        Button createBtn = findViewById(R.id.createFolderBtn);

        createBtn.setOnClickListener(v -> showCreateDialog());

        Button btn = findViewById(R.id.requestScreenBtn);

        btn.setOnClickListener(v -> {
            startActivity(new Intent(this, RequestsActivity.class));
        });

        Button dashboardBtn =
                findViewById(R.id.dashboardBtn);

        dashboardBtn.setOnClickListener(v -> {

            startActivity(
                    new Intent(
                            MainActivity.this,
                            DashboardActivity.class
                    )
            );
        });
    }
    private void fetchFolders() {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/folders")
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {

                String res = response.body().string();
                Log.d("API_RESPONSE", res);

                JSONArray array = new JSONArray(res);
                ArrayList<Folder> parsedList = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);

                    Folder folder = new Folder();
                    folder.folderId = obj.getString("folderId");
                    folder.name = obj.getString("name");
                    folder.ownerId = obj.getString("ownerId");
                    folder.isPublic = obj.getBoolean("isPublic");
                    folder.createdAt = obj.getLong("createdAt");

                    SharedPreferences prefs =
                            getSharedPreferences("app", MODE_PRIVATE);

                    String token =
                            prefs.getString("token", null);

                    String currentUserId =
                            extractUserIdFromToken(token);

// ✅ OWNER HAS ACCESS
                    if (folder.ownerId.equals(currentUserId)) {

                        folder.hasAccess = true;
                        folder.accessType = "permanent";
                    }

// ✅ PUBLIC HAS ACCESS
                    else if (folder.isPublic) {

                        folder.hasAccess = true;
                        folder.accessType = "public";
                    }

// ✅ CHECK SHARED ACCESS
                    else {

                        try {

                            String url =
                                    "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/check-permission"
                                            + "?folderId=" + folder.folderId
                                            + "&userId=" + currentUserId;

                            Request permissionRequest =
                                    new Request.Builder()
                                            .url(url)
                                            .get()
                                            .build();

                            Response permissionResponse =
                                    client.newCall(permissionRequest).execute();

                            String permissionBody =
                                    permissionResponse.body().string();

                            JSONObject permissionObj =
                                    new JSONObject(permissionBody);

                            boolean allowed =
                                    permissionObj.optBoolean("allowed", false);

                            if (allowed) {

                                folder.hasAccess = true;

                                folder.accessType =
                                        permissionObj.optString(
                                                "type",
                                                "shared"
                                        );

                                folder.expiresAt =
                                        permissionObj.optLong(
                                                "expiresAt",
                                                0
                                        );
                            }

                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }

                    parsedList.add(folder);
                }



                runOnUiThread(() -> {
                    folderList.clear();
                    folderList.addAll(parsedList);
                    adapter.notifyDataSetChanged();
                    StringBuilder result = new StringBuilder();

                    for (Folder f : folderList) {
                        result.append(f.name).append("\n");
                    }

                    Toast.makeText(this, result.toString(), Toast.LENGTH_LONG).show();
                    Toast.makeText(this, "Folders: " + array.length(), Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showCreateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Folder Name");

        Switch isPublicSwitch = new Switch(this);
        isPublicSwitch.setText("Public Folder");

        layout.addView(nameInput);
        layout.addView(isPublicSwitch);

        builder.setView(layout);

        builder.setTitle("Create Folder");

        builder.setPositiveButton("Create", (dialog, which) -> {
            String name = nameInput.getText().toString();
            boolean isPublic = isPublicSwitch.isChecked();

            createFolderAPI(name, isPublic);
        });

        builder.setNegativeButton("Cancel", null);

        builder.show();
    }
    private void createFolderAPI(String name, boolean isPublic) {

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        String userId = extractUserIdFromToken(token);
        OkHttpClient client = new OkHttpClient();

        try {
            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("isPublic", isPublic);
            json.put("ownerId", userId); // later replace with real user

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com//create-folder")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Folder Created", Toast.LENGTH_SHORT).show();

                        fetchFolders(); // 🔥 refresh list
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private String extractUserIdFromToken(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            String payload = new String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            );

            JSONObject json = new JSONObject(payload);

            return json.getString("sub"); // 👈 THIS IS IMPORTANT

        } catch (Exception e) {
            return "unknown";
        }
    }

    private void sendAccessRequest(Folder folder, int position) {
        String folderId = folder.folderId;
        String ownerId = folder.ownerId;

        SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        String email = prefs.getString("email", "unknown");
        String requesterId = extractUserIdFromToken(token);

        OkHttpClient client = new OkHttpClient();
        if (requesterId.equals(ownerId)) {
            Toast.makeText(this, "You cannot request your own folder", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d("SEND_REQUEST_OWNER", ownerId);
            Log.d("SEND_REQUEST_REQUESTER", requesterId);

            JSONObject json = new JSONObject();
            json.put("folderId", folderId);
            json.put("requesterId", requesterId);
            json.put("ownerId", ownerId);
            json.put("requesterEmail", email);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );
            Log.d("REQUEST_BODY", json.toString());

            Request request = new Request.Builder()
                    .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/request-access")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response response = client.newCall(request).execute()) {

                    String res = response.body().string();   // 👈 GET RESPONSE
                    Log.d("REQUEST_API_RESPONSE", res);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show();
                        folder.isPending = true;
                        adapter.notifyItemChanged(position);
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkPermissionAndOpen(Folder folder) {

        SharedPreferences prefs =
                getSharedPreferences("app", MODE_PRIVATE);

        String token = prefs.getString("token", null);

        String currentUserId =
                extractUserIdFromToken(token);

        // ✅ OWNER
        if (folder.ownerId.equals(currentUserId)) {

            Toast.makeText(
                    this,
                    "Opening owner folder",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String url =
                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/check-permission"
                        + "?folderId=" + folder.folderId
                        + "&userId=" + currentUserId;

        Log.d("CHECK_PERMISSION_URL", url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {

            try (Response response =
                         new OkHttpClient().newCall(request).execute()) {

                String body = response.body().string();

                Log.d("CHECK_PERMISSION", body);

                runOnUiThread(() -> {

                    try {

                        JSONObject obj = new JSONObject(body);

                        boolean allowed =
                                obj.optBoolean("allowed", false);

                        if (allowed) {

                            Toast.makeText(
                                    this,
                                    "Access Granted",
                                    Toast.LENGTH_SHORT
                            ).show();

                            // 🔥 OPEN FOLDER HERE

                        } else {

                            Toast.makeText(
                                    this,
                                    "Access expired or denied",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
}