package com.example.sashikiran12.project;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId, senderUserID, Current_State;

    private CircleImageView userProfileImage;
    private TextView userProfileName, userProfileStatus;
    private Button SendMessaageRequestButton, DeclineMessageRequestButton;

    private FirebaseAuth mAuth;

    private DatabaseReference UserRef, ChatRequestRef, ContactsRef, NotificationRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        receiverUserId = getIntent().getExtras().get("visit_user_id").toString();
        senderUserID = mAuth.getCurrentUser().getUid();



        userProfileImage = (CircleImageView) findViewById(R.id.visit_profile_image);
        userProfileName = (TextView) findViewById(R.id.visit_user_name);
        userProfileStatus = (TextView) findViewById(R.id.visit_profile_status);
        SendMessaageRequestButton = (Button) findViewById(R.id.send_message_request_button);
        DeclineMessageRequestButton = (Button) findViewById(R.id.decline_message_request_button);
        Current_State = "new";

        RetrieveUserInfo();
    }

    private void RetrieveUserInfo() {

        UserRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if((dataSnapshot.exists()) && (dataSnapshot.hasChild("image"))){

                    String userImage = dataSnapshot.child("image").getValue().toString();
                    String userNamee = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    Picasso.get().load(userImage).placeholder(R.drawable.profile).into(userProfileImage);
                    userProfileName.setText(userNamee);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequests();
                }
                else {

                    String userNamee = dataSnapshot.child("name").getValue().toString();
                    String userStatus = dataSnapshot.child("status").getValue().toString();

                    userProfileName.setText(userNamee);
                    userProfileStatus.setText(userStatus);

                    ManageChatRequests();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void ManageChatRequests() {

        ChatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(receiverUserId)){

                            String request_type = dataSnapshot.child(receiverUserId).child("request_type").getValue().toString();
                            if (request_type.equals("sent")){

                                Current_State = "request_sent";
                                SendMessaageRequestButton.setText("Cancel Chat Request");

                            }
                            else if (request_type.equals("recieved")) {

                                Current_State = "request_recieved";
                                SendMessaageRequestButton.setText("Accept Chat Request");

                                DeclineMessageRequestButton.setVisibility(View.VISIBLE);
                                DeclineMessageRequestButton.setEnabled(true);
                                DeclineMessageRequestButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        CancelChatRequest();

                                    }
                                });

                            }


                        }
                        else {

                            ContactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(DataSnapshot dataSnapshot) {

                                            if (dataSnapshot.hasChild(receiverUserId)) {

                                                Current_State = "friends";
                                                SendMessaageRequestButton.setText("Remove this Contact");

                                            }

                                        }

                                        @Override
                                        public void onCancelled(DatabaseError databaseError) {

                                        }
                                    });

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        if (!senderUserID.equals(receiverUserId)){

            SendMessaageRequestButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    SendMessaageRequestButton.setEnabled(false);

                    if(Current_State .equals("new")) {

                        SendChatRequest();

                    }

                    if (Current_State.equals("request_sent")){

                        CancelChatRequest();

                    }

                    if (Current_State.equals("request_recieved")){

                        AcceptChatRequest();

                    }

                    if (Current_State.equals("friends")) {

                        RemoveSpecificContact();

                    }

                }
            });

        }
        else {

            SendMessaageRequestButton.setVisibility(View.INVISIBLE);

        }

    }

    private void RemoveSpecificContact() {

        ContactsRef.child(senderUserID).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            ContactsRef.child(receiverUserId).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                SendMessaageRequestButton.setEnabled(true);
                                                Current_State = "new";
                                                SendMessaageRequestButton.setText("Send Message");


                                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                DeclineMessageRequestButton.setEnabled(false);
                                            }
                                        }
                                    });

                        }
                    }
                });

    }

    private void AcceptChatRequest() {

        ContactsRef.child(senderUserID).child(receiverUserId)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            ContactsRef.child(receiverUserId).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {

                                                ChatRequestRef.child(senderUserID).child(receiverUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {

                                                                    ChatRequestRef.child(receiverUserId).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {

                                                                                    SendMessaageRequestButton.setEnabled(true);
                                                                                    Current_State = "friends";
                                                                                    SendMessaageRequestButton.setText("Remove this Contact");

                                                                                    DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                                                    DeclineMessageRequestButton.setEnabled(false);

                                                                                }
                                                                            });

                                                                }
                                                            }
                                                        });

                                            }
                                        }
                                    });

                        }
                    }
                });

    }

    private void CancelChatRequest() {

        ChatRequestRef.child(senderUserID).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {

                            ChatRequestRef.child(receiverUserId).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                SendMessaageRequestButton.setEnabled(true);
                                                Current_State = "new";
                                                SendMessaageRequestButton.setText("Send Message");


                                                DeclineMessageRequestButton.setVisibility(View.INVISIBLE);
                                                DeclineMessageRequestButton.setEnabled(false);
                                            }
                                        }
                                    });

                        }
                    }
                });


    }

    private void SendChatRequest() {

        ChatRequestRef.child(senderUserID).child(receiverUserId)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()){


                            ChatRequestRef.child(receiverUserId).child(senderUserID)
                                    .child("request_type").setValue("recieved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()) {

                                                HashMap<String, String> chatnotificationMap = new HashMap<>();
                                                chatnotificationMap.put("from", senderUserID);
                                                chatnotificationMap.put("type", "request");

                                                NotificationRef.child(receiverUserId).push()
                                                        .setValue(chatnotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if (task.isSuccessful()){

                                                                    SendMessaageRequestButton.setEnabled(true);
                                                                    Current_State = "request_sent";
                                                                    SendMessaageRequestButton.setText("Cancel Chat Request");

                                                                }

                                                            }
                                                        });




                                            }

                                        }
                                    });
                        }

                    }
                });

    }
}
