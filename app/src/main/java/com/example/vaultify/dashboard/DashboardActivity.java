package com.example.vaultify.dashboard;



import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.MainActivity;
import com.example.vaultify.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import Recycler.DashboardAdapter;
import model.Folder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    RecyclerView recyclerView;

    ArrayList<Folder> folderList =
            new ArrayList<>();

    DashboardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dashboard);

        recyclerView =
                findViewById(R.id.dashboardRecycler);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new DashboardAdapter(
                this,
                folderList,
                folder -> {

                    // OPEN FOLDER
                    Intent i = new Intent(
                            DashboardActivity.this,
                            com.example.vaultify.dashboard.FolderContentActivity.class
                    );

                    i.putExtra(
                            "folderId",
                            folder.folderId
                    );
                    i.putExtra(
                            "folderName",
                            folder.name
                    );
                    i.putExtra(
                            "isOwner",
                            true
                    );

                    startActivity(i);
                }
        );

        recyclerView.setAdapter(adapter);

        fetchOwnedFolders();
    }

    private void fetchOwnedFolders() {

        SharedPreferences prefs =
                getSharedPreferences(
                        "app",
                        MODE_PRIVATE
                );

        String token =
                prefs.getString("token", null);

        String currentUserId =
                extractUserIdFromToken(token);

        OkHttpClient client =
                new OkHttpClient();

        Request request =
                new Request.Builder()
                        .url(
                                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/folders"
                        )
                        .get()
                        .build();

        new Thread(() -> {

            try (Response response =
                         client.newCall(request).execute()) {

                String body =
                        response.body().string();

                JSONArray array =
                        new JSONArray(body);

                ArrayList<Folder> temp =
                        new ArrayList<>();

                for (int i = 0;
                     i < array.length();
                     i++) {

                    JSONObject obj =
                            array.getJSONObject(i);

                    Folder folder =
                            new Folder();

                    folder.folderId =
                            obj.getString("folderId");

                    folder.name =
                            obj.getString("name");

                    folder.ownerId =
                            obj.getString("ownerId");

                    folder.isPublic =
                            obj.getBoolean("isPublic");

                    // ONLY OWNER FOLDERS
                    if (folder.ownerId.equals(
                            currentUserId
                    )) {

                        temp.add(folder);
                    }
                }

                runOnUiThread(() -> {

                    folderList.clear();

                    folderList.addAll(temp);

                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {

                e.printStackTrace();
            }

        }).start();
    }

    private String extractUserIdFromToken(
            String jwt
    ) {

        try {

            String[] parts =
                    jwt.split("\\.");

            String payload =
                    new String(
                            android.util.Base64.decode(
                                    parts[1],
                                    android.util.Base64.URL_SAFE
                            )
                    );

            JSONObject json =
                    new JSONObject(payload);

            return json.getString("sub");

        } catch (Exception e) {

            return "unknown";
        }
    }
}