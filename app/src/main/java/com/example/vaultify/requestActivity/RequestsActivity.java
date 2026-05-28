package com.example.vaultify.requestActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import Recycler.RequestAdapter;
import model.AccessRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ArrayList<AccessRequest> list = new ArrayList<>();
    RequestAdapter adapter;
    TextView noRequestText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_requests);

        Toast.makeText(this, "Requests screen opened", Toast.LENGTH_SHORT).show();
        recyclerView = findViewById(R.id.requestRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noRequestText = findViewById(R.id.noRequestText);

        adapter = new RequestAdapter(list, this::approveRequest);
        recyclerView.setAdapter(adapter);

        fetchRequests(); // load data

    }
    private void approveRequest(AccessRequest r, String type) {
        try {


            JSONObject json = new JSONObject();
            json.put("requestId", r.requestId);
            json.put("folderId", r.folderId);
            json.put("requesterId", r.requesterId);
            json.put("requesterEmail", r.requesterEmail);

            json.put("ownerId", r.ownerId);
            json.put("type", type); //  NEW

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Log.d("REQUEST_BODY", json.toString());

            Request req = new Request.Builder()
                    .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/approve-request")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response res = new OkHttpClient().newCall(req).execute()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show();
                        fetchRequests(); // refresh
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();

        } catch (Exception e) { e.printStackTrace(); }
    }
    private void fetchRequests() {
        String ownerId = extractUserIdFromToken(
                getSharedPreferences("app", MODE_PRIVATE).getString("token", null)
        );

        Request request = new Request.Builder()
                .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/pending-requests?ownerId=" + ownerId)
                .get()
                .build();

        new Thread(() -> {
            try (Response res = new OkHttpClient().newCall(request).execute()) {
                String body = res.body().string();
                Log.d("API_RESPONSE", body); // 👈 ADD THIS

                Log.d("FETCH_OWNER_ID", ownerId);
                if (body.isEmpty()) {
                    runOnUiThread(() -> {
                        list.clear();
                        adapter.notifyDataSetChanged();
                        noRequestText.setVisibility(View.VISIBLE);
                    });
                    return;
                }
                JSONArray arr = new JSONArray(body);

                ArrayList<AccessRequest> tmp = new ArrayList<>();
                String currentUserId = ownerId; // already extracted

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String requesterId = o.getString("requesterId");
                    String ownerIdFromApi = o.getString("ownerId");

                    // ONLY show valid incoming requests
                    if (!ownerIdFromApi.equals(currentUserId)) continue;

                    //  skip self-requests
                    if (requesterId.equals(currentUserId)) continue;

                    AccessRequest r = new AccessRequest();
                    r.requestId = o.getString("requestId");
                    r.folderId = o.getString("folderId");
                    r.requesterId = requesterId;
                    r.ownerId = ownerIdFromApi;
                    r.status = o.optString("status", "pending");
                    r.requesterEmail = o.optString("requesterEmail", null);

                    tmp.add(r);
                }

                runOnUiThread(() -> {
                    list.clear();
                    list.addAll(tmp);
                    adapter.notifyDataSetChanged();
                    noRequestText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    noRequestText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }
        }).start();
    }
    private String extractUserIdFromToken(String jwt) {
        try {
            if (jwt == null) return "unknown";

            String[] parts = jwt.split("\\.");
            String payload = new String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            );

            JSONObject json = new JSONObject(payload);

            return json.getString("sub"); //  THIS is userId

        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}