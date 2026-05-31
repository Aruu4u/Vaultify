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
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String REQUEST_STATE_PREFS = "request_state";
    private static final String PENDING_REQUESTS_KEY = "pendingFolderRequests";
    private static final String DENIED_REQUEST_PAIRS_KEY = "deniedRequestPairs";

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

                        // PUBLIC
                        if (folder.isPublic) {

                            Intent intent = new Intent(MainActivity.this, com.example.vaultify.dashboard.FolderContentActivity.class);
                            intent.putExtra("folderId", folder.folderId);
                            intent.putExtra("folderName", folder.name);
                            intent.putExtra("isOwner", folder.ownerId.equals(currentUserId));
                            startActivity(intent);

                            return;
                        }

                        //  OWNER
                        if (folder.ownerId.equals(currentUserId)) {

                            Intent intent = new Intent(MainActivity.this, com.example.vaultify.dashboard.FolderContentActivity.class);
                            intent.putExtra("folderId", folder.folderId);
                            intent.putExtra("folderName", folder.name);
                            intent.putExtra("isOwner", true);
                            startActivity(intent);

                            return;
                        }

                        // CHECK SHARED ACCESS
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

// OWNER HAS ACCESS
                    if (folder.ownerId.equals(currentUserId)) {

                        folder.hasAccess = true;
                        folder.accessType = "permanent";
                    }

// PUBLIC HAS ACCESS
                    else if (folder.isPublic) {

                        folder.hasAccess = true;
                        folder.accessType = "public";
                    }

//  CHECK SHARED ACCESS
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

                    if (folder.hasAccess) {
                        removePendingFolderRequest(currentUserId, folder.folderId);
                    } else if (hasPendingFolderRequest(currentUserId, folder.folderId)) {
                        removeDeniedRequestPair(
                                folder.ownerId,
                                currentUserId,
                                folder.folderId
                        );
                        folder.isPending = true;
                    }

                    parsedList.add(folder);
                }



                runOnUiThread(() -> {
                    folderList.clear();
                    folderList.addAll(parsedList);
                    adapter.notifyDataSetChanged();
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

        builder.setPositiveButton("Create", null);

        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button createButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            createButton.setOnClickListener(v -> {
                String name = nameInput.getText().toString().trim();
                boolean isPublic = isPublicSwitch.isChecked();

                if (name.isEmpty()) {
                    nameInput.setError("Folder name required");
                    return;
                }

                createFolderAPI(name, isPublic);
                dialog.dismiss();
            });
        });
        dialog.show();
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
            json.put("ownerId", userId);

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

                        fetchFolders(); //  refresh list
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

            return json.getString("sub"); // THIS IS IMPORTANT

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

        if (folder.isPending || hasPendingFolderRequest(requesterId, folderId)) {
            folder.isPending = true;
            adapter.notifyItemChanged(position);
            Toast.makeText(this, "Request Pending ..", Toast.LENGTH_SHORT).show();
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

                    String res = response.body().string();
                    Log.d("REQUEST_API_RESPONSE", res);
                    runOnUiThread(() -> {
                        if (response.isSuccessful()) {
                            removeDeniedRequestPair(ownerId, requesterId, folderId);
                            savePendingFolderRequest(requesterId, folderId);
                            Toast.makeText(this, "Request Sent", Toast.LENGTH_SHORT).show();
                            folder.isPending = true;
                            adapter.notifyItemChanged(position);
                        } else {
                            Toast.makeText(this, "Request failed", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Set<String> getPendingFolderRequests() {
        return new HashSet<>(
                getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                        .getStringSet(PENDING_REQUESTS_KEY, new HashSet<>())
        );
    }

    private String pendingFolderRequestKey(String userId, String folderId) {
        return userId + ":" + folderId;
    }

    private boolean hasPendingFolderRequest(String userId, String folderId) {
        return getPendingFolderRequests().contains(
                pendingFolderRequestKey(userId, folderId)
        );
    }

    private void savePendingFolderRequest(String userId, String folderId) {
        Set<String> pendingRequests = getPendingFolderRequests();
        pendingRequests.add(pendingFolderRequestKey(userId, folderId));

        getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(PENDING_REQUESTS_KEY, pendingRequests)
                .apply();
    }

    private void removePendingFolderRequest(String userId, String folderId) {
        Set<String> pendingRequests = getPendingFolderRequests();

        if (!pendingRequests.remove(pendingFolderRequestKey(userId, folderId))) {
            return;
        }

        getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(PENDING_REQUESTS_KEY, pendingRequests)
                .apply();
    }

    private void removeDeniedRequestPair(
            String ownerId,
            String requesterId,
            String folderId
    ) {
        Set<String> deniedRequestPairs = new HashSet<>(
                getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                        .getStringSet(DENIED_REQUEST_PAIRS_KEY, new HashSet<>())
        );

        if (!deniedRequestPairs.remove(
                ownerId + ":" + requesterId + ":" + folderId
        )) {
            return;
        }

        getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(DENIED_REQUEST_PAIRS_KEY, deniedRequestPairs)
                .apply();
    }

    private void checkPermissionAndOpen(Folder folder) {

        SharedPreferences prefs =
                getSharedPreferences("app", MODE_PRIVATE);

        String token = prefs.getString("token", null);

        String currentUserId =
                extractUserIdFromToken(token);

        //  OWNER
        if (folder.ownerId.equals(currentUserId)) {

            Intent intent = new Intent(MainActivity.this, com.example.vaultify.dashboard.FolderContentActivity.class);
            intent.putExtra("folderId", folder.folderId);
            intent.putExtra("folderName", folder.name);
            intent.putExtra("isOwner", true);
            startActivity(intent);

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

                            //  OPEN FOLDER HERE
                            Intent intent = new Intent(MainActivity.this, com.example.vaultify.dashboard.FolderContentActivity.class);
                            intent.putExtra("folderId", folder.folderId);
                            intent.putExtra("folderName", folder.name);
                            intent.putExtra("isOwner", false);
                            startActivity(intent);

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
