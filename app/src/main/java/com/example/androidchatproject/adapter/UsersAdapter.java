package com.example.androidchatproject.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.androidchatproject.R;
import com.example.androidchatproject.helper.ProfileImageLoader;
import com.example.androidchatproject.model.user.UserListItem;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter para ListView de usuarios con filtrado
 */
public class UsersAdapter extends BaseAdapter implements Filterable {
    
    private Context context;
    private List<UserListItem> originalUsers;  // Lista completa
    private List<UserListItem> filteredUsers;  // Lista filtrada
    private LayoutInflater inflater;
    private UserFilter userFilter;
    private boolean isOfflineMode;
    private ProfileImageLoader imageLoader;
    
    public UsersAdapter(Context context, List<UserListItem> users) {
        this.context = context;
        this.originalUsers = users;
        this.filteredUsers = new ArrayList<>(users);
        this.inflater = LayoutInflater.from(context);
        this.isOfflineMode = false;
        this.imageLoader = new ProfileImageLoader(context);
    }
    
    /**
     * Actualizar lista de usuarios
     */
    public void updateUsers(List<UserListItem> users) {
        this.originalUsers = users;
        this.filteredUsers = new ArrayList<>(users);
        notifyDataSetChanged();
    }
    
    /**
     * Establecer modo offline
     */
    public void setOfflineMode(boolean offline) {
        this.isOfflineMode = offline;
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return filteredUsers.size();
    }
    
    @Override
    public UserListItem getItem(int position) {
        return filteredUsers.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user, parent, false);
            
            holder = new ViewHolder();
            holder.profileImageView = convertView.findViewById(R.id.profileImageView);
            holder.usernameTextView = convertView.findViewById(R.id.usernameTextView);
            holder.userIdTextView = convertView.findViewById(R.id.userIdTextView);
            holder.offlineIndicator = convertView.findViewById(R.id.offlineIndicator);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        UserListItem user = getItem(position);
        
        // Configurar datos
        holder.usernameTextView.setText(user.getUsername());
        holder.userIdTextView.setText(user.getUserId() != null ? user.getUserId() : "Sin ID");
        
        // Log para depuración
        android.util.Log.d("UsersAdapter", "Position: " + position + ", User: " + user.getUsername() + ", UserID: " + user.getUserId() + ", ImageURL: " + user.getProfileImageUrl() + ", OfflineMode: " + isOfflineMode);
        
        // Cargar imagen de perfil o mostrar por defecto
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && !isOfflineMode) {
            // Cargar imagen desde URL usando ProfileImageLoader
            android.util.Log.d("UsersAdapter", "Loading image for user: " + user.getUsername());
            imageLoader.loadProfileImage(user.getProfileImageUrl(), holder.profileImageView, user.getUserId());
            holder.offlineIndicator.setVisibility(View.GONE);
        } else {
            // Mostrar imagen por defecto
            android.util.Log.d("UsersAdapter", "Loading default image for user: " + user.getUsername());
            loadDefaultImage(holder.profileImageView);
            
            // Mostrar indicador si está offline y normalmente tendría imagen
            if (isOfflineMode && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                holder.offlineIndicator.setVisibility(View.VISIBLE);
            } else {
                holder.offlineIndicator.setVisibility(View.GONE);
            }
        }
        
        return convertView;
    }
    
    /**
     * Cargar imagen por defecto
     */
    private void loadDefaultImage(ShapeableImageView imageView) {
        // Cargar user_default.png desde drawable
        try {
            Bitmap defaultBitmap = BitmapFactory.decodeResource(
                    context.getResources(),
                    R.drawable.user_default
            );
            if (defaultBitmap != null) {
                imageView.setImageBitmap(defaultBitmap);
            } else {
                imageView.setImageResource(R.drawable.user_default);
            }
        } catch (Exception e) {
            imageView.setImageResource(R.drawable.user_default);
        }
    }
    
    @Override
    public Filter getFilter() {
        if (userFilter == null) {
            userFilter = new UserFilter();
        }
        return userFilter;
    }
    
    /**
     * ViewHolder para optimizar rendimiento
     */
    static class ViewHolder {
        ShapeableImageView profileImageView;
        TextView usernameTextView;
        TextView userIdTextView;
        ImageView offlineIndicator;
    }
    
    /**
     * Filtro para buscar usuarios por username
     */
    private class UserFilter extends Filter {
        
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            
            if (constraint == null || constraint.length() == 0) {
                // Sin filtro, mostrar todos
                results.values = new ArrayList<>(originalUsers);
                results.count = originalUsers.size();
            } else {
                // Filtrar por username (case insensitive)
                String filterString = constraint.toString().toLowerCase().trim();
                List<UserListItem> filtered = new ArrayList<>();
                
                for (UserListItem user : originalUsers) {
                    if (user.getUsername().toLowerCase().contains(filterString)) {
                        filtered.add(user);
                    }
                }
                
                results.values = filtered;
                results.count = filtered.size();
            }
            
            return results;
        }
        
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredUsers = (List<UserListItem>) results.values;
            notifyDataSetChanged();
        }
    }
    
    /**
     * Limpiar recursos (llamar cuando el adapter ya no se use)
     */
    public void cleanup() {
        if (imageLoader != null) {
            imageLoader.shutdown();
        }
    }
}
