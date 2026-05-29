package com.example.vaultify.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.net.Uri;


import org.json.JSONObject;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import okhttp3.RequestBody;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import Recycler.FileAdapter;
import model.FileItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FolderContentActivity extends AppCompatActivity {

    private RecyclerView filesRecyclerView;
    private FileAdapter fileAdapter;
    private ArrayList<FileItem> fileList;
    private TextView tvFolderName;
    private ImageButton btnBack;
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabUpload;

    private String folderId;
    private String folderName;
    private boolean isOwner;
    private androidx.activity.result.ActivityResultLauncher<android.content.Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_content);

        // Get data from Intent
        folderId = getIntent().getStringExtra("folderId");
        folderName = getIntent().getStringExtra("folderName");
        isOwner = getIntent().getBooleanExtra("isOwner", false);

        initViews();
        setupRecyclerView();
        
        filePickerLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        android.net.Uri fileUri = result.getData().getData();
                        uploadFile(fileUri);
                    }
                }
        );

        if (isOwner) {
            fabUpload.setVisibility(View.VISIBLE);
            fabUpload.setOnClickListener(v -> openFilePicker());
        } else {
            fabUpload.setVisibility(View.GONE);
        }
        
        tvFolderName.setText(folderName != null ? folderName : "Folder Content");

        btnBack.setOnClickListener(v -> finish());

        loadFiles();
    }

    private void initViews() {
        filesRecyclerView = findViewById(R.id.filesRecyclerView);
        tvFolderName = findViewById(R.id.tvFolderName);
        btnBack = findViewById(R.id.btnBack);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        progressBar = findViewById(R.id.progressBar);
        fabUpload = findViewById(R.id.fabUpload);
    }

    private void setupRecyclerView() {
        fileList = new ArrayList<>();
        fileAdapter = new FileAdapter(this, fileList, isOwner,folderId);
        filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        filesRecyclerView.setAdapter(fileAdapter);
    }

    private void loadFiles() {
        progressBar.setVisibility(View.VISIBLE);
        filesRecyclerView.setVisibility(View.GONE);
        emptyStateLayout.setVisibility(View.GONE);

        if (folderId == null || folderId.isEmpty()) {
            Toast.makeText(this, "Invalid folder", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String url = "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/list-files?folderId=" + folderId;
        
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    JSONArray jsonArray = new JSONArray(responseBody);
                    
                    ArrayList<FileItem> parsedData = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String id = obj.optString("id");
                        String name = obj.optString("name");
                        String size = obj.optString("size");
                        long uploadedAt = obj.optLong("uploadedAt");
                        
                        // We don't have download URLs yet, so pass empty string or null
                        parsedData.add(new FileItem(id, name, "", size, uploadedAt));
                    }

                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (parsedData.isEmpty()) {
                            emptyStateLayout.setVisibility(View.VISIBLE);
                        } else {
                            fileList.clear();
                            fileList.addAll(parsedData);
                            fileAdapter.notifyDataSetChanged();
                            filesRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "Failed to load files", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Network error", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void openFilePicker() {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        filePickerLauncher.launch(intent);
    }

    private void uploadFile(android.net.Uri fileUri) {
        // This is a placeholder for the actual upload logic
        // You will need to hit an AWS API endpoint (e.g. /generate-upload-url) to get an S3 pre-signed PUT URL
        // and then upload the file binary using OkHttp.
        String fileName = getFileName(fileUri);

        String apiUrl =
                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/upload-url"
                        + "?folderId=" + folderId
                        + "&fileName=" + fileName;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        new Thread(() -> {

            try (Response response =
                         client.newCall(request).execute()) {

                if (response.isSuccessful()
                        && response.body() != null) {

                    String body =
                            response.body().string();

                    JSONObject json =
                            new JSONObject(body);

                    String uploadUrl =
                            json.getString("uploadUrl");

                    uploadToS3(fileUri, uploadUrl);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();
    }
    private String getFileName(Uri uri) {

        String result = null;

        Cursor cursor =
                getContentResolver().query(
                        uri,
                        null,
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            if (cursor.moveToFirst()) {

                int index =
                        cursor.getColumnIndex(
                                OpenableColumns.DISPLAY_NAME
                        );

                result = cursor.getString(index);
            }

            cursor.close();
        }

        return result;
    }

    private void uploadToS3(
            Uri fileUri,
            String uploadUrl) {

        new Thread(() -> {

            try {

                InputStream inputStream =
                        getContentResolver()
                                .openInputStream(fileUri);

                byte[] fileBytes =
                        readBytes(inputStream);

                RequestBody body =
                        RequestBody.create(fileBytes);

                Request uploadRequest =
                        new Request.Builder()
                                .url(uploadUrl)
                                .put(body)
                                .build();

                Response response =
                        new OkHttpClient()
                                .newCall(uploadRequest)
                                .execute();

                runOnUiThread(() -> {

                    if (response.isSuccessful()) {

                        Toast.makeText(
                                this,
                                "Upload Successful",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadFiles();

                    } else {

                        Toast.makeText(
                                this,
                                "Upload Failed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                });

            } catch (Exception e) {

                e.printStackTrace();

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }
    private byte[] readBytes(
            InputStream inputStream)
            throws IOException {

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

        int nRead;

        byte[] data =
                new byte[16384];

        while ((nRead =
                inputStream.read(
                        data,
                        0,
                        data.length)) != -1) {

            buffer.write(
                    data,
                    0,
                    nRead
            );
        }

        return buffer.toByteArray();
    }
}
