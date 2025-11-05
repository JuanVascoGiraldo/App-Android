package com.example.androidchatproject.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.androidchatproject.R;
import com.example.androidchatproject.helper.DateTimeHelper;
import com.example.androidchatproject.helper.ProfileImageLoader;
import com.example.androidchatproject.model.chats.ChatItem;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Adapter para ListView de chats
 */
public class ChatsAdapter extends BaseAdapter {
    
    private Context context;
    private List<ChatItem> chats;
    private LayoutInflater inflater;
    private ProfileImageLoader imageLoader;
    
    public ChatsAdapter(Context context, List<ChatItem> chats) {
        this.context = context;
        this.chats = chats != null ? chats : new ArrayList<>();
        this.inflater = LayoutInflater.from(context);
        this.imageLoader = new ProfileImageLoader(context);
    }
    
    /**
     * Actualizar lista de chats y ordenar del más reciente al más antiguo
     */
    public void updateChats(List<ChatItem> chats) {
        this.chats = chats != null ? chats : new ArrayList<>();
        sortChatsByMostRecent();
        notifyDataSetChanged();
    }
    
    /**
     * Ordenar chats del más reciente al más antiguo
     */
    private void sortChatsByMostRecent() {
        if (chats == null || chats.isEmpty()) {
            return;
        }
        
        Collections.sort(chats, new Comparator<ChatItem>() {
            @Override
            public int compare(ChatItem chat1, ChatItem chat2) {
                // Obtener la fecha de cada chat (usar lastMessageTime o updatedAt)
                String dateStr1 = chat1.getLastMessageTime() != null && !chat1.getLastMessageTime().isEmpty()
                        ? chat1.getLastMessageTime()
                        : chat1.getUpdatedAt();
                
                String dateStr2 = chat2.getLastMessageTime() != null && !chat2.getLastMessageTime().isEmpty()
                        ? chat2.getLastMessageTime()
                        : chat2.getUpdatedAt();
                
                // Convertir a Date
                Date date1 = DateTimeHelper.utcToLocal(dateStr1);
                Date date2 = DateTimeHelper.utcToLocal(dateStr2);
                
                // Si alguna fecha es null, moverla al final
                if (date1 == null && date2 == null) return 0;
                if (date1 == null) return 1;
                if (date2 == null) return -1;
                
                // Ordenar descendente (más reciente primero)
                return date2.compareTo(date1);
            }
        });
    }
    
    @Override
    public int getCount() {
        return chats.size();
    }
    
    @Override
    public ChatItem getItem(int position) {
        return chats.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_chat, parent, false);
            
            holder = new ViewHolder();
            holder.profileImageView = convertView.findViewById(R.id.chatProfileImageView);
            holder.usernameTextView = convertView.findViewById(R.id.chatUsernameTextView);
            holder.lastMessageTextView = convertView.findViewById(R.id.chatLastMessageTextView);
            holder.timeTextView = convertView.findViewById(R.id.chatTimeTextView);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        ChatItem chat = getItem(position);
        
        // Configurar username
        holder.usernameTextView.setText(chat.getUsername());
        
        // Configurar último mensaje
        if (chat.getLastMessage() != null && !chat.getLastMessage().isEmpty()) {
            holder.lastMessageTextView.setText(chat.getLastMessage());
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
        } else {
            holder.lastMessageTextView.setText("Sin mensajes");
            holder.lastMessageTextView.setVisibility(View.VISIBLE);
        }
        
        // Configurar tiempo (usar lastMessageTime si está disponible, sino updatedAt)
        String timeToDisplay = chat.getLastMessageTime() != null && !chat.getLastMessageTime().isEmpty() 
                ? chat.getLastMessageTime() 
                : chat.getUpdatedAt();
        holder.timeTextView.setText(formatTime(timeToDisplay));
        
        // Cargar imagen de perfil desde cache
        if (chat.getProfileImg() != null && !chat.getProfileImg().isEmpty()) {
            imageLoader.loadProfileImage(chat.getProfileImg(), holder.profileImageView, chat.getUser());
        } else {
            // Cargar imagen por defecto
            holder.profileImageView.setImageResource(R.drawable.user_default);
        }
        
        return convertView;
    }
    
    /**
     * Formatear fecha/hora para mostrar
     * Convierte de UTC a hora local y formatea apropiadamente
     */
    private String formatTime(String utcDateTimeStr) {
        return DateTimeHelper.formatChatTime(utcDateTimeStr);
    }
    
    /**
     * ViewHolder pattern para optimizar el rendimiento
     */
    static class ViewHolder {
        ShapeableImageView profileImageView;
        TextView usernameTextView;
        TextView lastMessageTextView;
        TextView timeTextView;
    }
    
    /**
     * Limpiar recursos
     */
    public void cleanup() {
        if (imageLoader != null) {
            imageLoader.shutdown();
        }
    }
}
