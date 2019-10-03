package com.mtios.gasdetector;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FormActivity extends AppCompatActivity {

    private Button mSimpan;
    private EditText mNama, mAlamat, mNohp;
    private static final String TAG = "FormActivity";
    private String token;
    DatabaseReference ref, data, size;
    private String topic;
    private Integer arraysize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        mSimpan = (Button) findViewById(R.id.simpan);
        mNama = (EditText) findViewById(R.id.nama);
        mAlamat = (EditText) findViewById(R.id.alamat);
        mNohp = (EditText) findViewById(R.id.nohp);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getInstanceId failed", task.getException());
                    return;
                }
                token = task.getResult().getToken();
            }
        });

        mSimpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nama = mNama.getText().toString().trim();
                String alamat = mAlamat.getText().toString().trim();
                String nohp = mNohp.getText().toString().trim();
                boolean failflag = false;

                Pattern pattern = Pattern.compile("(\\()?(\\+62|62|0)(\\d{2,3})?\\)?[ .-]?\\d{2,4}[ .-]?\\d{2,4}[ .-]?\\d{2,4}");
                Matcher matcher = pattern.matcher(nohp);
                if(matcher.matches())
                {
                    failflag = false;
                }
                else
                {
                    failflag = true;
                    Toast.makeText(FormActivity.this, R.string.wrongphoneformat, Toast.LENGTH_LONG).show();
                }

                if(nama.isEmpty() || nama.length() == 0 || nama.equals("") || nama == null)
                {
                    failflag = true;
                    Toast.makeText(FormActivity.this, R.string.nameisempty, Toast.LENGTH_LONG).show();
                }
                if(alamat.isEmpty() || alamat.length() == 0 || alamat.equals("") || alamat == null)
                {
                    failflag = true;
                    Toast.makeText(FormActivity.this, R.string.addressisempty, Toast.LENGTH_LONG).show();
                }
                if(nohp.isEmpty() || nohp.length() == 0 || nohp.equals("") || nohp == null)
                {
                    failflag = true;
                    Toast.makeText(FormActivity.this, R.string.phoneisempty, Toast.LENGTH_LONG).show();
                }
                else
                {
                    if(failflag == false)
                    {
                        ref = FirebaseDatabase.getInstance().getReference().child("user").child(token);
                        ref.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    topic = (String) dataSnapshot.child("Topic").getValue();
                                    size = FirebaseDatabase.getInstance().getReference().child("size").child(topic);
                                    size.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if(dataSnapshot.exists())
                                            {
                                                String sizeof = dataSnapshot.getValue().toString();
                                                arraysize = Integer.parseInt(sizeof) + 1;
                                            }
                                            else
                                            {
                                                arraysize = 1;
                                            }
                                            Log.v(TAG, String.valueOf(arraysize));
                                            size.setValue(arraysize);
                                            setData();
                                        }
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                        }
                                    });
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                    }
                }
            }
        });
    }

    private void setData()
    {
        data = FirebaseDatabase.getInstance().getReference().child("penerima").child(topic);
        data.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String alamat = mAlamat.getText().toString();
                String nama = mNama.getText().toString();
                String nohp = mNohp.getText().toString();
                data.child(String.valueOf(arraysize)).child("alamat").setValue(alamat);
                data.child(String.valueOf(arraysize)).child("nama").setValue(nama);
                data.child(String.valueOf(arraysize)).child("nohp").setValue(nohp);
                onBackPressed();
                showList();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }
    private void showList()
    {
        Intent intent = new Intent(FormActivity.this, ListActivity.class);
        startActivity(intent);
    }
}
