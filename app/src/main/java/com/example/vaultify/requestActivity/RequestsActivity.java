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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import Recycler.RequestAdapter;
import model.AccessRequest;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestsActivity extends AppCompatActivity {

    private static final String REQUEST_STATE_PREFS = "request_state";
    private static final String DENIED_REQUESTS_KEY = "deniedRequestIds";
    private static final String DENIED_REQUEST_PAIRS_KEY = "deniedRequestPairs";

    RecyclerView recyclerView;
    ArrayList<AccessRequest> list = new ArrayList<>();
    RequestAdapter adapter;
    TextView noRequestText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_requests);

        recyclerView = findViewById(R.id.requestRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        noRequestText = findViewById(R.id.noRequestText);

        adapter = new RequestAdapter(list, this::approveRequest, this::denyRequest);
        recyclerView.setAdapter(adapter);

        fetchRequests();

    }
    private void approveRequest(AccessRequest r, String type) {
        try {


            JSONObject json = new JSONObject();
            json.put("requestId", r.requestId);
            json.put("folderId", r.folderId);
            json.put("requesterId", r.requesterId);
            json.put("requesterEmail", r.requesterEmail);

            json.put("ownerId", r.ownerId);
            json.put("type", type);

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
                        fetchRequests();
                    });
                } catch (Exception e) { e.printStackTrace(); }
            }).start();

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void denyRequest(AccessRequest r) {
        try {
            JSONObject json = new JSONObject();
            json.put("requestId", r.requestId);
            json.put("folderId", r.folderId);
            json.put("requesterId", r.requesterId);
            json.put("ownerId", r.ownerId);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request req = new Request.Builder()
                    .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/deny-request")
                    .post(body)
                    .build();

            new Thread(() -> {
                try (Response res = new OkHttpClient().newCall(req).execute()) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Denied", Toast.LENGTH_SHORT).show();
                        cacheDeniedRequestId(r.requestId);
                        cacheDeniedRequestPair(r);
                        removeRequestFromList(r);
                        fetchRequests();
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
                Log.d("API_RESPONSE", body);

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
                Map<String, String> folderNames = fetchFolderNames(new OkHttpClient());
                Set<String> deniedRequestIds = getDeniedRequestIds();
                Set<String> deniedRequestPairs = getDeniedRequestPairs();
                Set<String> visibleRequestPairs = new HashSet<>();
                String currentUserId = ownerId;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String requesterId = o.getString("requesterId");
                    String ownerIdFromApi = o.getString("ownerId");
                    String status = o.optString("status", "pending");

                    // ONLY show valid incoming requests
                    if (!ownerIdFromApi.equals(currentUserId)) continue;

                    // Skip self-requests
                    if (requesterId.equals(currentUserId)) continue;

                    if (!status.equalsIgnoreCase("pending")) continue;

                    AccessRequest r = new AccessRequest();
                    r.requestId = o.getString("requestId");

                    if (deniedRequestIds.contains(r.requestId)) continue;

                    r.folderId = o.getString("folderId");
                    String requestPair = requestPairKey(ownerIdFromApi, requesterId, r.folderId);

                    if (deniedRequestPairs.contains(requestPair)) continue;

                    if (!visibleRequestPairs.add(requestPair)) continue;

                    r.folderName = o.optString(
                            "folderName",
                            o.optString(
                                    "name",
                                    folderNames.getOrDefault(r.folderId, r.folderId)
                            )
                    );
                    r.requesterId = requesterId;
                    r.ownerId = ownerIdFromApi;
                    r.status = status;
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

    private Map<String, String> fetchFolderNames(OkHttpClient client) {
        Map<String, String> folderNames = new HashMap<>();

        Request request = new Request.Builder()
                .url("https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/folders")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.body() == null) return folderNames;

            JSONArray folders = new JSONArray(response.body().string());
            for (int i = 0; i < folders.length(); i++) {
                JSONObject folder = folders.getJSONObject(i);
                folderNames.put(
                        folder.getString("folderId"),
                        folder.optString("name", folder.getString("folderId"))
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return folderNames;
    }

    private Set<String> getDeniedRequestIds() {
        return new HashSet<>(
                getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                        .getStringSet(DENIED_REQUESTS_KEY, new HashSet<>())
        );
    }

    private void cacheDeniedRequestId(String requestId) {
        Set<String> deniedRequestIds = getDeniedRequestIds();
        deniedRequestIds.add(requestId);

        getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(DENIED_REQUESTS_KEY, deniedRequestIds)
                .apply();
    }

    private Set<String> getDeniedRequestPairs() {
        return new HashSet<>(
                getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                        .getStringSet(DENIED_REQUEST_PAIRS_KEY, new HashSet<>())
        );
    }

    private void cacheDeniedRequestPair(AccessRequest request) {
        Set<String> deniedRequestPairs = getDeniedRequestPairs();
        deniedRequestPairs.add(
                requestPairKey(
                        request.ownerId,
                        request.requesterId,
                        request.folderId
                )
        );

        getSharedPreferences(REQUEST_STATE_PREFS, MODE_PRIVATE)
                .edit()
                .putStringSet(DENIED_REQUEST_PAIRS_KEY, deniedRequestPairs)
                .apply();
    }

    private String requestPairKey(String ownerId, String requesterId, String folderId) {
        return ownerId + ":" + requesterId + ":" + folderId;
    }

    private void removeRequestFromList(AccessRequest request) {
        for (int i = list.size() - 1; i >= 0; i--) {
            AccessRequest item = list.get(i);
            boolean sameRequestId = item.requestId.equals(request.requestId);
            boolean sameRequestPair =
                    item.ownerId.equals(request.ownerId)
                            && item.requesterId.equals(request.requesterId)
                            && item.folderId.equals(request.folderId);

            if (sameRequestId || sameRequestPair) {
                list.remove(i);
                adapter.notifyItemRemoved(i);
            }
        }

        noRequestText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String extractUserIdFromToken(String jwt) {
        try {
            if (jwt == null) return "unknown";

            String[] parts = jwt.split("\\.");
            String payload = new String(
                    android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            );

            JSONObject json = new JSONObject(payload);

            return json.getString("sub");

        } catch (Exception e) {
            e.printStackTrace();
            return "unknown";
        }
    }
}
