package com.example.easychat.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.easychat.ChatActivity;
import com.example.easychat.R;
import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.AndroidUtil;
import com.example.easychat.utils.FirebaseUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.firestore.ListenerRegistration;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatroomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatroomModelViewHolder holder, int position, @NonNull ChatroomModel model) {
        holder.removeListeners();

        // --- CORREÇÃO: PASSO 1 ---
        // Aplica um fundo padrão para TODOS os itens (grupos e conversas privadas).
        // Isso garante que nenhum item será invisível.
        holder.itemView.setBackground(ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.edit_text_rounded_corner));

        String lastMessage = model.getLastMessage() != null ? model.getLastMessage() : "";
        boolean lastMessageSentByMe = model.getLastMessageSenderId() != null && model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

        if (lastMessageSentByMe && !model.isGroupChat()) {
            holder.lastMessageText.setText("You: " + lastMessage);
        } else {
            holder.lastMessageText.setText(lastMessage);
        }
        holder.lastMessageTime.setText(FirebaseUtil.timestampToString(model.getLastMessageTimestamp()));

        if (model.isGroupChat()) {
            // Lógica para grupos (agora vai funcionar pois o fundo já foi aplicado)
            holder.usernameText.setText(model.getGroupName());
            holder.profilePic.setImageResource(R.drawable.chat_icon);
            holder.statusIndicator.setVisibility(View.GONE);
            holder.unreadCountText.setVisibility(View.GONE); // Lembre-se que a contagem de mensagens não lidas para grupos não está implementada

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("chatroomId", model.getChatroomId());
                intent.putExtra("isGroupChat", true);
                intent.putExtra("groupName", model.getGroupName());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        } else {
            // Lógica para conversas privadas
            FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                    .get().addOnSuccessListener(documentSnapshot -> {
                        if (!documentSnapshot.exists()) return;

                        UserModel otherUserModel = documentSnapshot.toObject(UserModel.class);
                        if (otherUserModel == null) return;

                        holder.usernameText.setText(otherUserModel.getUsername());

                        holder.statusIndicator.setVisibility(View.VISIBLE);
                        switch (otherUserModel.getUserStatus()) {
                            case "online":
                                holder.statusIndicator.setImageResource(R.drawable.online_indicator);
                                break;
                            case "busy":
                                holder.statusIndicator.setImageResource(R.drawable.busy_indicator);
                                break;
                            default:
                                holder.statusIndicator.setImageResource(R.drawable.offline_indicator);
                                break;
                        }

                        FirebaseUtil.getOtherProfilePicStorageRef(otherUserModel.getUserId()).getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    AndroidUtil.setProfilePic(context, uri, holder.profilePic);
                                }).addOnFailureListener(e -> {
                                    holder.profilePic.setImageResource(R.drawable.person_icon);
                                });

                        // A lógica do listener que antes definia o fundo agora só vai cuidar do DESTAQUE
                        holder.attachChatroomListener(model.getChatroomId(), otherUserModel.getUserId());

                        holder.itemView.setOnClickListener(v -> {
                            Intent intent = new Intent(context, ChatActivity.class);
                            AndroidUtil.passUserModelAsIntent(intent, otherUserModel);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(intent);
                        });
                    });
        }
    }
    @NonNull
    @Override
    public ChatroomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatroomModelViewHolder(view);
    }

    @Override
    public void onViewRecycled(@NonNull ChatroomModelViewHolder holder) {
        super.onViewRecycled(holder);
        holder.removeListeners();
    }

    static class ChatroomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText, lastMessageText, lastMessageTime, unreadCountText;
        ImageView profilePic, statusIndicator;
        ListenerRegistration unreadCountListener;
        ListenerRegistration chatroomListener;

        public ChatroomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
            unreadCountText = itemView.findViewById(R.id.unread_message_count_text);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        void attachChatroomListener(String chatroomId, String otherUserId) {
            chatroomListener = FirebaseUtil.getChatroomReference(chatroomId)
                    .addSnapshotListener((snapshot, e) -> {
                        if (e != null || snapshot == null || !snapshot.exists()) return;

                        ChatroomModel chatroomModel = snapshot.toObject(ChatroomModel.class);
                        if (chatroomModel == null) return;

                        boolean hasCustomNotif = chatroomModel.getCustomNotificationStatus()
                                .getOrDefault(FirebaseUtil.currentUserId(), false);

                        attachUnreadCountListener(chatroomId, otherUserId, hasCustomNotif);
                    });
        }

        void attachUnreadCountListener(String chatroomId, String otherUserId, boolean hasCustomNotif) {
            removeUnreadCountListener();

            unreadCountListener = FirebaseUtil.getChatroomMessageReference(chatroomId)
                    .whereEqualTo("senderId", otherUserId)
                    .whereEqualTo("status", ChatMessageModel.STATUS_SENT)
                    .addSnapshotListener((querySnapshot, e) -> {
                        if (e != null) return;

                        if (querySnapshot != null) {
                            int unreadCount = querySnapshot.size();
                            if (unreadCount > 0) {
                                unreadCountText.setText(String.valueOf(unreadCount));
                                unreadCountText.setVisibility(View.VISIBLE);
                            } else {
                                unreadCountText.setVisibility(View.GONE);
                            }

                            if (hasCustomNotif && unreadCount > 0) {
                                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.list_item_background_highlight));
                            } else {
                                itemView.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.edit_text_rounded_corner));
                            }
                        }
                    });
        }

        void removeListeners() {
            removeUnreadCountListener();
            if (chatroomListener != null) {
                chatroomListener.remove();
                chatroomListener = null;
            }
        }

        void removeUnreadCountListener() {
            if (unreadCountListener != null) {
                unreadCountListener.remove();
                unreadCountListener = null;
            }
        }
    }
}