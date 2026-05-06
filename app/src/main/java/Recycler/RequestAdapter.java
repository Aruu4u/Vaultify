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

import model.AccessRequest;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.VH> {

    public interface OnApproveClick {
        void onApprove(AccessRequest r, String type);
    }

    ArrayList<AccessRequest> list;
    OnApproveClick listener;

    public RequestAdapter(ArrayList<AccessRequest> list, OnApproveClick l) {
        this.list = list;
        this.listener = l;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        Button approve;
        VH(View v) {
            super(v);
            text = v.findViewById(R.id.requestText);
            approve = v.findViewById(R.id.approveBtn);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
        View v = LayoutInflater.from(p.getContext())
                .inflate(R.layout.request_item, p, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int i) {
        AccessRequest r = list.get(i);
  //      h.text.setText(r.requesterId + " → " + r.folderId);

        String name = (r.requesterEmail != null && !r.requesterEmail.isEmpty())
                ? r.requesterEmail
                : r.requesterId;

        h.text.setText(name + " requested access");
        h.approve.setOnClickListener(v -> {

            String[] options = {"1 Hour", "1 Week", "Permanent"};

            new android.app.AlertDialog.Builder(h.itemView.getContext())
                    .setTitle("Select Access Duration")
                    .setItems(options, (dialog, which) -> {

                        String type;
                        if (which == 0) type = "hourly";
                        else if (which == 1) type = "weekly";
                        else type = "permanent";

                        listener.onApprove(r, type); // 👈 pass type
                    })
                    .show();
        });
    }

    @Override public int getItemCount() { return list.size(); }
}