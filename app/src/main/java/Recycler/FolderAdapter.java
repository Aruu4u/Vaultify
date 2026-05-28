package Recycler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vaultify.R;

import java.util.ArrayList;

import model.Folder;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    ArrayList<Folder> folderList;
    public interface OnFolderActionListener {

        void onFolderClick(Folder folder);

        void onRequestClick(Folder folder, int position);
    }

    OnFolderActionListener listener;
    public FolderAdapter(ArrayList<Folder> folderList,
                         OnFolderActionListener listener) {
        this.folderList = folderList;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, type;

        Button requestBtn;
        public ViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.folderName);
            type = itemView.findViewById(R.id.folderType);
            requestBtn = itemView.findViewById(R.id.requestBtn);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.folder_item, parent, false);
        return new ViewHolder(view);
    }

//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        Folder folder = folderList.get(position);
//
//        holder.name.setText(folder.name);
//
//        holder.itemView.setOnClickListener(v -> {
//            listener.onFolderClick(folder);
//        });
//
//        if (folder.isPublic) {
//            holder.type.setText("Public");
//            holder.requestBtn.setVisibility(View.GONE);
//        } else {
//            holder.type.setText("Private");
//            holder.requestBtn.setVisibility(View.VISIBLE); // show button
//
//            holder.requestBtn.setOnClickListener(v -> {
//                listener.onRequestClick(folder); // 👈 call activity
//            });
//        }
//    }
@Override
public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

    Folder folder = folderList.get(position);

    holder.name.setText(folder.name);

    holder.itemView.setOnClickListener(v -> {
        listener.onFolderClick(folder);
    });

    // ✅ PUBLIC
    if (folder.isPublic) {

        holder.type.setText("Public");
        holder.requestBtn.setVisibility(View.GONE);
    }

    // ✅ HAS ACCESS
    else if (folder.hasAccess && !folder.isPublic) {

        String text =
                "Access: " + folder.accessType;

        if (!folder.accessType.equals("permanent")) {

            long now =
                    System.currentTimeMillis() / 1000;

            long remaining =
                    folder.expiresAt - now;

            if (remaining < 0)
                remaining = 0;

            if (folder.accessType.equals("hourly")) {

                long minutes =
                        remaining / 60;

                text += " • expires in "
                        + minutes + " mins";

            } else {

                long days =
                        remaining / (24 * 3600);

                text += " • expires in "
                        + days + " days";
            }
        }

        holder.type.setText(text);

        holder.requestBtn.setVisibility(View.GONE);
    }

    // ✅ NO ACCESS
    else {

        holder.type.setText("Private");

        holder.requestBtn.setVisibility(View.VISIBLE);

        if (folder.isPending) {
            holder.requestBtn.setText("Request Pending ..");
            holder.requestBtn.setEnabled(false);
            holder.requestBtn.setOnClickListener(null);
        } else {
            holder.requestBtn.setText("Request");
            holder.requestBtn.setEnabled(true);
            holder.requestBtn.setOnClickListener(v -> {
                listener.onRequestClick(folder, holder.getAdapterPosition());
            });
        }
    }
}

    @Override
    public int getItemCount() {
        return folderList.size();
    }
}