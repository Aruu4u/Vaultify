package com.example.vaultify.dashboard;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import Recycler.PermissionAdapter;
import model.PermissionUser;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PermissionActivity
        extends AppCompatActivity {

    TextView folderTitle;

    TextView noActiveUserText;

    RecyclerView recyclerView;

    ArrayList<PermissionUser> list =
            new ArrayList<>();

    PermissionAdapter adapter;

    String folderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_permission
        );

        folderTitle =
                findViewById(R.id.folderTitle);

        recyclerView =
                findViewById(R.id.permissionRecycler);

        noActiveUserText =
                findViewById(R.id.noActiveUserText);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        folderId =
                getIntent().getStringExtra(
                        "folderId"
                );

        String folderName =
                getIntent().getStringExtra(
                        "folderName"
                );

        folderTitle.setText(folderName);

        adapter = new PermissionAdapter(
                list,
                this::revokePermission
        );

        recyclerView.setAdapter(adapter);

        fetchPermissions();
    }

    private void fetchPermissions() {

        OkHttpClient client =
                new OkHttpClient();

        String url =
                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/permissions?folderId="
                        + folderId;

        Request request =
                new Request.Builder()
                        .url(url)
                        .get()
                        .build();

        new Thread(() -> {

            try (Response response =
                         client.newCall(request).execute()) {

                String body =
                        response.body().string();

                JSONArray array =
                        new JSONArray(body);

                ArrayList<PermissionUser> temp =
                        new ArrayList<>();

                for (int i = 0;
                     i < array.length();
                     i++) {

                    JSONObject obj =
                            array.getJSONObject(i);

                    PermissionUser user =
                            new PermissionUser();

                    user.permissionId =
                            obj.getString(
                                    "permissionId"
                            );

                    user.userId =
                            obj.getString(
                                    "userId"
                            );

                    user.email =
                            obj.optString(
                                    "email",
                                    user.userId
                            );

                    user.type =
                            obj.getString("type");

                    user.expiresAt =
                            obj.optLong(
                                    "expiresAt",
                                    0
                            );

                    temp.add(user);
                }

                runOnUiThread(() -> {

                    list.clear();

                    list.addAll(temp);

                    adapter.notifyDataSetChanged();

                    noActiveUserText.setVisibility(
                            list.isEmpty()
                                    ? android.view.View.VISIBLE
                                    : android.view.View.GONE
                    );
                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() -> noActiveUserText.setVisibility(
                        list.isEmpty()
                                ? android.view.View.VISIBLE
                                : android.view.View.GONE
                ));
            }

        }).start();
    }

    private void revokePermission(
            PermissionUser user
    ) {

        try {

            JSONObject json =
                    new JSONObject();

            json.put(
                    "permissionId",
                    user.permissionId
            );

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            MediaType.parse(
                                    "application/json"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(
                                    "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/revoke-permission"
                            )
                            .post(body)
                            .build();

            new Thread(() -> {

                try (Response response =
                             new OkHttpClient()
                                     .newCall(request)
                                     .execute()) {

                    runOnUiThread(() -> {

                        Toast.makeText(
                                this,
                                "Permission Revoked",
                                Toast.LENGTH_SHORT
                        ).show();

                        fetchPermissions();
                    });

                } catch (Exception e) {

                    e.printStackTrace();
                }

            }).start();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
