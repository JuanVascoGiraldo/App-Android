package com.example.androidchatproject.adapter;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.androidchatproject.R;
import com.example.androidchatproject.model.chats.MessageResponse;
import com.example.androidchatproject.helper.DateTimeHelper;
import com.example.androidchatproject.helper.ProfileImageLoader;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends BaseAdapter {
    
    private final Context context;
    private List<MessageResponse> messages;
    private final String currentUserId;
    private final ProfileImageLoader imageLoader;
    private OnAttachmentClickListener attachmentClickListener;
    
    public interface OnAttachmentClickListener {
        void onAttachmentClick(String url, String fileName, String mimeType);
    }
    
    public MessagesAdapter(Context context, String currentUserId) {
        this.context = context;
        this.messages = new ArrayList<>();
        this.currentUserId = currentUserId;
        this.imageLoader = new ProfileImageLoader(context);
    }
    
    public void setOnAttachmentClickListener(OnAttachmentClickListener listener) {
        this.attachmentClickListener = listener;
    }
    
    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addMessage(MessageResponse message) {
        this.messages.add(message);
        notifyDataSetChanged();
    }
    
    @Override
    public int getCount() {
        return messages.size();
    }
    
    @Override
    public MessageResponse getItem(int position) {
        return messages.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public int getViewTypeCount() {
        return 2; // Dos tipos: mensajes enviados y recibidos
    }
    
    @Override
    public int getItemViewType(int position) {
        MessageResponse message = messages.get(position);
        // 0 = recibido, 1 = enviado
        return message.getSenderId().equals(currentUserId) ? 1 : 0;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessageResponse message = messages.get(position);
        boolean isSent = message.getSenderId().equals(currentUserId);
        
        ViewHolder holder;
        
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            
            if (isSent) {
                convertView = inflater.inflate(R.layout.item_message_sent, parent, false);
            } else {
                convertView = inflater.inflate(R.layout.item_message_received, parent, false);
            }
            
            holder = new ViewHolder();
            holder.messageTextView = convertView.findViewById(R.id.messageTextView);
            holder.timeTextView = convertView.findViewById(R.id.timeTextView);
            holder.attachmentContainer = convertView.findViewById(R.id.attachmentContainer);
            holder.attachmentImageView = convertView.findViewById(R.id.attachmentImageView);
            holder.fileAttachmentLayout = convertView.findViewById(R.id.fileAttachmentLayout);
            holder.fileNameTextView = convertView.findViewById(R.id.fileNameTextView);
            holder.audioAttachmentLayout = convertView.findViewById(R.id.audioAttachmentLayout);
            holder.audioNameTextView = convertView.findViewById(R.id.audioNameTextView);
            
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        // Mostrar contenido del mensaje si existe
        if (message.getContent() != null && !message.getContent().trim().isEmpty()) {
            holder.messageTextView.setVisibility(View.VISIBLE);
            holder.messageTextView.setText(message.getContent());
        } else {
            holder.messageTextView.setVisibility(View.GONE);
        }
        
        // Mostrar fecha y hora
        String timeText = DateTimeHelper.formatMessageDateTime(message.getCreatedAt());
        holder.timeTextView.setText(timeText);
        
        // Manejar attachments
        handleAttachment(message, holder);
        
        return convertView;
    }
    
    private void handleAttachment(MessageResponse message, ViewHolder holder) {
        String attachmentUrl = message.getAttachmentUrl();
        String mimeType = message.getMimeType();
        
        // Resetear visibilidad
        holder.attachmentContainer.setVisibility(View.GONE);
        holder.attachmentImageView.setVisibility(View.GONE);
        holder.fileAttachmentLayout.setVisibility(View.GONE);
        holder.audioAttachmentLayout.setVisibility(View.GONE);
        
        if (attachmentUrl == null || attachmentUrl.trim().isEmpty()) {
            return; // No hay attachment
        }
        
        holder.attachmentContainer.setVisibility(View.VISIBLE);
        
        // Verificar si estamos offline
        boolean isOffline = !isNetworkAvailable();
        
        if (isOffline) {
            // Modo offline: mostrar file_not_found.png para cualquier tipo de archivo
            holder.attachmentImageView.setVisibility(View.VISIBLE);
            holder.attachmentImageView.setImageResource(R.drawable.file_not_found);
            return;
        }
        
        // Usar mimeType si está disponible, sino detectar por extensión
        boolean isImage = false;
        boolean isPdf = false;
        boolean isAudio = false;
        
        if (mimeType != null && !mimeType.isEmpty()) {
            // Usar mimeType para determinar el tipo de archivo
            isImage = mimeType.startsWith("image/");
            isPdf = mimeType.equals("application/pdf");
            isAudio = mimeType.startsWith("audio/");
        } else {
            // Fallback: detectar por extensión
            String lowerUrl = attachmentUrl.toLowerCase();
            isImage = isImageFile(lowerUrl);
            isPdf = isPdfFile(lowerUrl);
            isAudio = isAudioFile(lowerUrl);
        }
        
        String fileName = extractFileName(attachmentUrl);
        
        if (isImage) {
            // Es una imagen - mostrar imagen con click para ver en pantalla completa
            holder.attachmentImageView.setVisibility(View.VISIBLE);
            imageLoader.loadProfileImage(attachmentUrl, holder.attachmentImageView, message.getId());
            
            holder.attachmentImageView.setOnClickListener(v -> {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(attachmentUrl, fileName, mimeType);
                }
            });
            
        } else if (isPdf) {
            // Es un PDF - mostrar botón de descarga
            holder.fileAttachmentLayout.setVisibility(View.VISIBLE);
            holder.fileNameTextView.setText(fileName);
            
            holder.fileAttachmentLayout.setOnClickListener(v -> {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(attachmentUrl, fileName, mimeType);
                }
            });
            
        } else if (isAudio) {
            // Es un audio - mostrar reproductor/descarga
            holder.audioAttachmentLayout.setVisibility(View.VISIBLE);
            holder.audioNameTextView.setText(fileName);
            
            holder.audioAttachmentLayout.setOnClickListener(v -> {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(attachmentUrl, fileName, mimeType);
                }
            });
            
        } else {
            // Tipo desconocido - botón de descarga genérico
            holder.fileAttachmentLayout.setVisibility(View.VISIBLE);
            holder.fileNameTextView.setText(fileName);
            
            holder.fileAttachmentLayout.setOnClickListener(v -> {
                if (attachmentClickListener != null) {
                    attachmentClickListener.onAttachmentClick(attachmentUrl, fileName, mimeType);
                }
            });
        }
    }
    
    private boolean isImageFile(String url) {
        return url.endsWith(".jpg") || url.endsWith(".jpeg") || 
               url.endsWith(".png") || url.endsWith(".gif") || 
               url.endsWith(".webp") || url.endsWith(".bmp");
    }
    
    private boolean isPdfFile(String url) {
        return url.endsWith(".pdf");
    }
    
    private boolean isAudioFile(String url) {
        return url.endsWith(".mp3") || url.endsWith(".wav") || 
               url.endsWith(".ogg") || url.endsWith(".m4a") || 
               url.endsWith(".aac") || url.endsWith(".flac");
    }
    
    private String extractFileName(String url) {
        if (url == null || url.isEmpty()) {
            return "archivo";
        }
        
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        
        return "archivo";
    }
    
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        
        return false;
    }
    
    private static class ViewHolder {
        TextView messageTextView;
        TextView timeTextView;
        LinearLayout attachmentContainer;
        ImageView attachmentImageView;
        LinearLayout fileAttachmentLayout;
        TextView fileNameTextView;
        LinearLayout audioAttachmentLayout;
        TextView audioNameTextView;
    }
}
