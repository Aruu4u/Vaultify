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

    public interface OnDenyClick {
        void onDeny(AccessRequest r);
    }

    ArrayList<AccessRequest> list;
    OnApproveClick listener;
    OnDenyClick denyListener;

    public RequestAdapter(
            ArrayList<AccessRequest> list,
            OnApproveClick l,
            OnDenyClick denyListener
    ) {
        this.list = list;
        this.listener = l;
        this.denyListener = denyListener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView text;
        Button approve, deny;

        VH(View v) {
            super(v);
            text = v.findViewById(R.id.requestText);
            approve = v.findViewById(R.id.approveBtn);
            deny = v.findViewById(R.id.denyBtn);
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

        String name = (r.requesterEmail != null && !r.requesterEmail.isEmpty())
                ? r.requesterEmail
                : r.requesterId;
        String folder = (r.folderName != null && !r.folderName.isEmpty())
                ? r.folderName
                : r.folderId;

        h.text.setText(name + " requested access to " + folder);
        h.approve.setOnClickListener(v -> {

            String[] options = {"1 Hour", "1 Week", "Permanent"};

            new android.app.AlertDialog.Builder(h.itemView.getContext())
                    .setTitle("Select Access Duration")
                    .setItems(options, (dialog, which) -> {

                        String type;
                        if (which == 0) type = "hourly";
                        else if (which == 1) type = "weekly";
                        else type = "permanent";

                        listener.onApprove(r, type);
                    })
                    .show();
        });

        h.deny.setOnClickListener(v -> denyListener.onDeny(r));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
