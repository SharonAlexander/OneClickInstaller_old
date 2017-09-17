package com.sharon.oneclickinstaller.backup.adapter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
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

public class BackupAdapter extends RecyclerView.Adapter<BackupAdapter.MyViewHolder> {

    private BackupAdapterListener listener;
    private Context mContext;
    private List<ApplicationInfo> appList = new ArrayList<>();
    private List<AppProperties> backedupAppList = new ArrayList<>();
    private SparseBooleanArray selectedItems;
    private PackageManager packageManager;

    public BackupAdapter(Context mContext, List<ApplicationInfo> appList, ArrayList<AppProperties> backedupAppList, BackupAdapterListener listener) {
        this.listener = listener;
        this.mContext = mContext;
        this.appList = appList;
        this.selectedItems = new SparseBooleanArray();
        this.packageManager = mContext.getPackageManager();
        this.backedupAppList = backedupAppList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.row_view_list, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        ApplicationInfo app = appList.get(position);
        holder.appname.setText(app.loadLabel(packageManager));
        PackageInfo pi = packageManager.getPackageArchiveInfo(app.publicSourceDir, 0);
        holder.version.setText(pi.versionName);
        holder.backedup.setText(R.string.backup_info_no_backup);
        holder.backedup.setTextColor(Color.RED);
        for (AppProperties appProperties : backedupAppList) {
            if (appProperties.getPname().equals(app.packageName)) {
                holder.backedup.setText(R.string.backup_info_backup);
                holder.backedup.setTextColor(Color.GREEN);
                if (appProperties.getVersioncode() < pi.versionCode) {
                    holder.backedup.setText(R.string.backup_info_backup_new_version);
                    holder.backedup.setTextColor(Color.BLUE);
                    break;
                }
                break;
            }
        }
        holder.icon.setImageDrawable(app.loadIcon(packageManager));
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

    public void removeAll(int i) {
        selectedItems.put(i, false);
        selectedItems.delete(i);
        notifyItemChanged(i);
    }

    public void selectAll(int i) {
        selectedItems.put(i, true);
        notifyItemChanged(i);
    }

    public void selectUpdated(int i) {

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

    public interface BackupAdapterListener {
        void onIconClicked(int position);

        void onIconImportantClicked(int position);

        void onMessageRowClicked(int position);

        void onRowLongClicked(int position);
    }

    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {

        TextView appname, backedup, version;
        ImageView icon;
        RelativeLayout relativeLayout;

        private MyViewHolder(View view) {
            super(view);
            appname = (TextView) view.findViewById(R.id.installer_backup_appname);
            backedup = (TextView) view.findViewById(R.id.installed_backedup);
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
