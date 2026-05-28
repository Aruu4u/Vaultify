package Recycler;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.vaultify.R;
import com.example.vaultify.dashboard.PermissionActivity;

import java.util.ArrayList;

import model.Folder;

public class DashboardAdapter
        extends RecyclerView.Adapter<DashboardAdapter.VH> {

    Context context;
    ArrayList<Folder> list;

    public interface OnOpenFolder {
        void onOpen(Folder folder);
    }

    OnOpenFolder listener;

    public DashboardAdapter(
            Context context,
            ArrayList<Folder> list,
            OnOpenFolder listener
    ) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView folderName;
        TextView folderStatus;
        MaterialButton openFolderBtn;
        ImageButton menuBtn;

        VH(View v) {
            super(v);

            folderName =
                    v.findViewById(R.id.folderName);

            folderStatus =
                    v.findViewById(R.id.folderStatus);

            openFolderBtn =
                    v.findViewById(R.id.openFolderBtn);

            menuBtn =
                    v.findViewById(R.id.menuBtn);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View v = LayoutInflater.from(context)
                .inflate(
                        R.layout.dashboard_folder_item,
                        parent,
                        false
                );

        return new VH(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull VH holder,
            int position
    ) {

        Folder folder = list.get(position);

        holder.folderName.setText(folder.name);

        if (folder.isPublic) {
            holder.folderStatus.setVisibility(View.VISIBLE);
            holder.menuBtn.setVisibility(View.GONE);
            holder.openFolderBtn.setVisibility(View.VISIBLE);
            holder.openFolderBtn.setOnClickListener(v -> {
                listener.onOpen(folder);
            });
        } else {
            holder.folderStatus.setVisibility(View.GONE);
            holder.menuBtn.setVisibility(View.VISIBLE);
            holder.openFolderBtn.setVisibility(View.GONE);
            holder.menuBtn.setOnClickListener(v -> {

                PopupMenu popup =
                        new PopupMenu(context, holder.menuBtn);

                popup.getMenu().add("Open Folder");
                popup.getMenu().add("Permissions");

                popup.setOnMenuItemClickListener(item -> {

                    if (item.getTitle()
                            .equals("Open Folder")) {

                        listener.onOpen(folder);
                    }

                    else {

                        Intent i = new Intent(
                                context,
                                PermissionActivity.class
                        );

                        i.putExtra(
                                "folderId",
                                folder.folderId
                        );

                        i.putExtra(
                                "folderName",
                                folder.name
                        );

                        context.startActivity(i);
                    }

                    return true;
                });

                popup.show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}