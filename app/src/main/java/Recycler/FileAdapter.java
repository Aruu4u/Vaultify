package Recycler;

import android.app.DownloadManager;
import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.R;

import org.json.JSONObject;

import java.util.ArrayList;

import model.FileItem;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    Context context;
    ArrayList<FileItem> fileList;
    boolean isOwner;
    private String folderId;

    public FileAdapter(Context context, ArrayList<FileItem> fileList, boolean isOwner,String folderId) {
        this.context = context;
        this.fileList = fileList;
        this.isOwner = isOwner;
        this.folderId = folderId;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName, fileDetails;
        ImageButton downloadBtn, deleteBtn;

        public ViewHolder(View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileDetails = itemView.findViewById(R.id.fileDetails);
            downloadBtn = itemView.findViewById(R.id.downloadBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.file_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem fileItem = fileList.get(position);

        holder.fileName.setText(fileItem.name);

        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                fileItem.uploadedAt * 1000,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );

        String details = fileItem.size + " • " + timeAgo;
        holder.fileDetails.setText(details);
        
        // Simple icon logic based on extension
        if (fileItem.name.toLowerCase().endsWith(".pdf")) {
             holder.fileIcon.setImageResource(android.R.drawable.ic_menu_agenda);
        } else if (fileItem.name.toLowerCase().matches(".*\\.(png|jpg|jpeg|gif)$")) {
             holder.fileIcon.setImageResource(android.R.drawable.ic_menu_gallery);
        } else {
             holder.fileIcon.setImageResource(android.R.drawable.ic_menu_save);
        }

        if (isOwner) {
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setOnClickListener(v -> {
                // Placeholder for Delete logic
                deleteFile(fileItem.name, position);
            });
        } else {
            holder.deleteBtn.setVisibility(View.GONE);
        }

        holder.downloadBtn.setOnClickListener(v -> {
            // Placeholder for Download logic using DownloadManager
            // You will need an API to fetch the pre-signed GET URL here
//            Toast.makeText(context, "Download functionality coming soon! (Awaiting AWS backend)", Toast.LENGTH_SHORT).show();

            generateDownloadUrl(fileItem.name);
        });
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    private void generateDownloadUrl(String fileName) {

        String apiUrl =
                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/download-url"
                        + "?folderId=" + folderId
                        + "&fileName=" + fileName;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {

                if (response.isSuccessful() && response.body() != null) {

                    String body = response.body().string();

                    JSONObject jsonObject = new JSONObject(body);

                    String downloadUrl =
                            jsonObject.getString("downloadUrl");

                    downloadFile(downloadUrl, fileName);

                } else {

                    ((android.app.Activity) context)
                            .runOnUiThread(() ->
                                    Toast.makeText(
                                            context,
                                            "Failed to generate URL",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                }

            } catch (Exception e) {
                e.printStackTrace();

                ((android.app.Activity) context)
                        .runOnUiThread(() ->
                                Toast.makeText(
                                        context,
                                        e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
            }
        }).start();
    }

    private void downloadFile(String downloadUrl,
                              String fileName) {

        DownloadManager.Request request =
                new DownloadManager.Request(
                        android.net.Uri.parse(downloadUrl)
                );

        request.setTitle(fileName);

        request.setNotificationVisibility(
                DownloadManager.Request
                        .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        );

        request.setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                fileName
        );

        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(
                        Context.DOWNLOAD_SERVICE
                );

        downloadManager.enqueue(request);

        ((android.app.Activity) context)
                .runOnUiThread(() ->
                        Toast.makeText(
                                context,
                                "Download started",
                                Toast.LENGTH_SHORT
                        ).show()
                );
    }

    private void deleteFile(String fileName, int position) {

        String apiUrl =
                "https://273rmp34zj.execute-api.ap-south-1.amazonaws.com/delete-url"
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

                if (response.isSuccessful()) {

                    ((android.app.Activity) context)
                            .runOnUiThread(() -> {

                                fileList.remove(position);

                                notifyItemRemoved(position);

                                Toast.makeText(
                                        context,
                                        "File deleted successfully",
                                        Toast.LENGTH_SHORT
                                ).show();
                            });

                } else {

                    ((android.app.Activity) context)
                            .runOnUiThread(() ->
                                    Toast.makeText(
                                            context,
                                            "Delete failed",
                                            Toast.LENGTH_SHORT
                                    ).show()
                            );
                }

            } catch (Exception e) {

                e.printStackTrace();

                ((android.app.Activity) context)
                        .runOnUiThread(() ->
                                Toast.makeText(
                                        context,
                                        e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show()
                        );
            }

        }).start();
    }
}
