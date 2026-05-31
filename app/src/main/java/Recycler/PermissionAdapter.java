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

import model.PermissionUser;

public class PermissionAdapter
        extends RecyclerView.Adapter<PermissionAdapter.VH> {

    public interface OnRevoke {
        void onRevoke(PermissionUser user);
    }

    ArrayList<PermissionUser> list;

    OnRevoke listener;

    public PermissionAdapter(
            ArrayList<PermissionUser> list,
            OnRevoke listener
    ) {
        this.list = list;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {

        TextView email, access;

        Button revoke;

        VH(View v) {
            super(v);

            email =
                    v.findViewById(R.id.userEmail);

            access =
                    v.findViewById(R.id.accessText);

            revoke =
                    v.findViewById(R.id.revokeBtn);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        View v = LayoutInflater
                .from(parent.getContext())
                .inflate(
                        R.layout.permission_item,
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

        PermissionUser user =
                list.get(position);

        holder.email.setText(user.email);

        String text =
                "Access: " + user.type;

        if (!user.type.equals("permanent")) {

            long now =
                    System.currentTimeMillis() / 1000;

            long remaining =
                    user.expiresAt - now;

            if (remaining < 0)
                remaining = 0;

            long hours =
                    remaining / 3600;

            text +=
                    " - expires in "
                            + hours
                            + " hrs";
        }

        holder.access.setText(text);

        holder.revoke.setOnClickListener(v -> {
            listener.onRevoke(user);
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
