package com.example.easychat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.easychat.model.ChatMessageModel;
import com.example.easychat.model.ChatroomModel;
import com.example.easychat.model.UserModel;
import com.example.easychat.utils.FirebaseUtil;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;

public class CreateGroupActivity extends AppCompatActivity {

    private EditText groupNameInput;
    private Button createGroupBtn;
    private ProgressBar progressBar;
    private ArrayList<String> memberIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        groupNameInput = findViewById(R.id.group_name_input);
        createGroupBtn = findViewById(R.id.create_group_btn);
        progressBar = findViewById(R.id.progress_bar);

        memberIds = getIntent().getStringArrayListExtra("userIds");

        createGroupBtn.setOnClickListener(v -> handleCreateGroup());
    }

    private void handleCreateGroup() {
        String groupName = groupNameInput.getText().toString().trim();
        if (groupName.isEmpty() || groupName.length() < 3) {
            groupNameInput.setError("Group name must be at least 3 characters");
            return;
        }
        setInProgress(true);

        FirebaseUtil.currentUserDetails().get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                UserModel currentUser = task.getResult().toObject(UserModel.class);
                if (currentUser != null) {
                    createGroupWithBatch(groupName, currentUser.getUsername());
                } else {
                    setInProgress(false);
                    Toast.makeText(this, "Failed to retrieve user details.", Toast.LENGTH_SHORT).show();
                }
            } else {
                setInProgress(false);
                Toast.makeText(this, "Failed to retrieve user details.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createGroupWithBatch(String groupName, String creatorName) {
        ArrayList<String> finalMemberIds = new ArrayList<>();
        if (memberIds != null) {
            finalMemberIds.addAll(memberIds);
        }
        finalMemberIds.add(FirebaseUtil.currentUserId());

        DocumentReference chatroomRef = FirebaseUtil.allChatroomCollectionReference().document();
        String chatroomId = chatroomRef.getId();
        Timestamp creationTimestamp = Timestamp.now();
        String initialMessage = creatorName + " criou o grupo";

        ChatroomModel chatroomModel = new ChatroomModel(
                chatroomId,
                finalMemberIds,
                creationTimestamp,
                FirebaseUtil.currentUserId(),
                groupName,
                true
        );
        chatroomModel.setLastMessage(initialMessage);

        ChatMessageModel creationMessage = new ChatMessageModel(initialMessage, FirebaseUtil.currentUserId(), creationTimestamp, ChatMessageModel.STATUS_SENT, new ArrayList<>(), "text");
        DocumentReference messageRef = FirebaseUtil.getChatroomMessageReference(chatroomId).document();

        WriteBatch batch = FirebaseUtil.getFirestore().batch();
        batch.set(chatroomRef, chatroomModel);
        batch.set(messageRef, creationMessage);

        batch.commit().addOnCompleteListener(task -> {
            setInProgress(false);
            if (task.isSuccessful()) {
                Toast.makeText(CreateGroupActivity.this, "Group created successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CreateGroupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CreateGroupActivity.this, "Failed to create group", Toast.LENGTH_SHORT).show();
            }
        });
    }

    void setInProgress(boolean inProgress) {
        if (inProgress) {
            progressBar.setVisibility(View.VISIBLE);
            createGroupBtn.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            createGroupBtn.setVisibility(View.VISIBLE);
        }
    }
}