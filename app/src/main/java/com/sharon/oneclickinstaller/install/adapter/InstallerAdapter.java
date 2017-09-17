package com.sharon.oneclickinstaller.install.adapter;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sharon.oneclickinstaller.AppProperties;
import com.sharon.oneclickinstaller.R;

import java.util.ArrayList;
import java.util.List;

public class InstallerAdapter extends RecyclerView.Adapter<InstallerAdapter.MyViewHolder> {

    private InstallerAdapterListener listener;
    private Context mContext;
    private List<AppProperties> appList = new ArrayList<>();
    private SparseBooleanArray selectedItems;

    public InstallerAdapter(Context mContext, List<AppProperties> appList, InstallerAdapterListener listener) {
        this.listener = listener;
        this.mContext = mContext;
        this.appList = appList;
        this.selectedItems = new SparseBooleanArray();

    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_view_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        AppProperties app = appList.get(position);
        holder.appname.setText(app.getAppname());
        holder.installed.setText(app.isAlready_installed() ? mContext.getString(R.string.installed) : mContext.getString(R.string.not_insalled)   );
        holder.installed.setTextColor(app.isAlready_installed() ? Color.GREEN : Color.RED);
        holder.version.setText(app.getVersionname());
        holder.icon.setImageDrawable(app.getIcon());
        holder.itemView.setActivated(selectedItems.get(position, false));
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMessageRowClicked(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items =
                new ArrayList<>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            selectedItems.delete(pos);
        } else {
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);
    }

    public void selectAll(int i) {
        selectedItems.put(i, true);
        notifyItemChanged(i);
    }

    public void removeData(int position) {
        appList.remove(position);
    }

    public void clearSelections() {
        selectedItems.clear();
        notifyDataSetChanged();
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public interface InstallerAdapterListener {
        void onIconClicked(int position);

        void onIconImportantClicked(int position);

        void onMessageRowClicked(int position);

        void onRowLongClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        TextView appname, installed, version;
        ImageView icon;
        RelativeLayout relativeLayout;

        private MyViewHolder(View view) {
            super(view);
            appname = (TextView) view.findViewById(R.id.installer_backup_appname);
            installed = (TextView) view.findViewById(R.id.installed_backedup);
            version = (TextView) view.findViewById(R.id.version);
            icon = (ImageView) view.findViewById(R.id.installer_backup_appicon);
            relativeLayout = (RelativeLayout) view.findViewById(R.id.installer_backup_list_row_rlayout);
            relativeLayout.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View view) {
            listener.onRowLongClicked(getAdapterPosition());
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            return true;
        }
    }
}
