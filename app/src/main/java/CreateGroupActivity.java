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
import java.util.List;

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

        // Verificação para garantir que a lista de membros não seja nula
        if (memberIds == null) {
            memberIds = new ArrayList<>();
        }

        createGroupBtn.setOnClickListener(v -> handleCreateGroup());
    }

    private void handleCreateGroup() {
        String groupName = groupNameInput.getText().toString().trim();
        if (groupName.isEmpty() || groupName.length() < 3) {
            groupNameInput.setError("O nome do grupo deve ter pelo menos 3 caracteres");
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
                    Toast.makeText(this, "Falha ao obter detalhes do usuário.", Toast.LENGTH_SHORT).show();
                }
            } else {
                setInProgress(false);
                Toast.makeText(this, "Falha ao obter detalhes do usuário.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createGroupWithBatch(String groupName, String creatorName) {
        // PASSO 1: Criar a lista final de membros, garantindo que o criador esteja nela.
        List<String> finalMemberIds = new ArrayList<>(memberIds);
        if (!finalMemberIds.contains(FirebaseUtil.currentUserId())) {
            finalMemberIds.add(FirebaseUtil.currentUserId());
        }

        // PASSO 2: Criar uma referência para o novo documento da sala de chat.
        DocumentReference chatroomRef = FirebaseUtil.allChatroomCollectionReference().document();
        String chatroomId = chatroomRef.getId();
        Timestamp creationTimestamp = Timestamp.now();
        String initialMessage = creatorName + " criou o grupo";

        // PASSO 3: Construir o objeto ChatroomModel de forma explícita.
        ChatroomModel chatroomModel = new ChatroomModel();
        chatroomModel.setChatroomId(chatroomId);
        chatroomModel.setUserIds(finalMemberIds); // USA A LISTA COMPLETA
        chatroomModel.setLastMessageTimestamp(creationTimestamp);
        chatroomModel.setLastMessageSenderId(FirebaseUtil.currentUserId());
        chatroomModel.setGroupName(groupName);
        chatroomModel.setGroupChat(true); // DEFINE EXPLÍCITAMENTE COMO GRUPO
        chatroomModel.setLastMessage(initialMessage);

        // PASSO 4: Criar a mensagem inicial do sistema.
        ChatMessageModel creationMessage = new ChatMessageModel(initialMessage, FirebaseUtil.currentUserId(), creationTimestamp, ChatMessageModel.STATUS_SENT, new ArrayList<>(), "text");
        DocumentReference messageRef = FirebaseUtil.getChatroomMessageReference(chatroomId).document();

        // PASSO 5: Usar um WriteBatch para garantir que ambas as operações sejam atômicas.
        WriteBatch batch = FirebaseUtil.getFirestore().batch();
        batch.set(chatroomRef, chatroomModel);      // Operação 1: Criar o grupo
        batch.set(messageRef, creationMessage); // Operação 2: Adicionar a mensagem inicial

        // PASSO 6: Executar o batch.
        batch.commit().addOnCompleteListener(task -> {
            setInProgress(false);
            if (task.isSuccessful()) {
                Toast.makeText(CreateGroupActivity.this, "Grupo criado com sucesso", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CreateGroupActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(CreateGroupActivity.this, "Falha ao criar o grupo", Toast.LENGTH_SHORT).show();
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