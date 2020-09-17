package com.mtios.gasdetector;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
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
import com.google.firebase.messaging.FirebaseMessaging;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener{

    private Button buttonSubscribe;
    private Button buttonUnsubscribe;

    private EditText editTextTopic;

    private Switch switchGas, switchFlame, switchList;
    private String JSON_STRING, token, topic_app;
    private static final String TAG = "SettingActivity";
    DatabaseReference ref, checkdatauser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
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
                getData();
            }
        });
        editTextTopic = (EditText) findViewById(R.id.editTextTopic);

        buttonSubscribe = (Button) findViewById(R.id.buttonSubscribe);
        buttonUnsubscribe = (Button) findViewById(R.id.buttonUnsubscribe);

        switchGas = (Switch) findViewById(R.id.gasSwitch);
        switchFlame = (Switch) findViewById(R.id.flameSwitch);
        switchList = (Switch) findViewById(R.id.listSwitch);

        switchGas.setOnClickListener(this);
        switchFlame.setOnClickListener(this);
        switchList.setOnClickListener(this);
        buttonSubscribe.setOnClickListener(this);
        buttonUnsubscribe.setOnClickListener(this);
    }

    private void getData(){
        final String topic = editTextTopic.getText().toString().trim();
        final DatabaseReference data = FirebaseDatabase.getInstance().getReference().child("user").child(token);
        data.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    getJSON();
                }
                else {
                    data.setValue(0);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getSettings(){
        final String topic = editTextTopic.getText().toString().trim();
        checkdatauser = FirebaseDatabase.getInstance().getReference().child("user").child(token);
        checkdatauser.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    topic_app = (String) dataSnapshot.child("Topic").getValue();

                    editTextTopic.setText(topic_app);
                    if(topic_app != null){
                        ref = FirebaseDatabase.getInstance().getReference().child("topic").child(topic_app);
                        ref.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if(dataSnapshot.exists())
                                {
                                    Boolean gasWorking = (Boolean) dataSnapshot.child("gas_sensor").getValue();
                                    Boolean flameWorking = (Boolean) dataSnapshot.child("flame_sensor").getValue();
                                    Boolean sendList = (Boolean) dataSnapshot.child("send_list").getValue();
                                    switchGas.setChecked(gasWorking);
                                    switchFlame.setChecked(flameWorking);
                                    switchList.setChecked(sendList);
                                }
                                else
                                {
                                    unSubscribe();
                                    checkdatauser.setValue(0);
                                    data_notexist();
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                            }
                        });
                        data_exist();
                    }
                    else {
                        data_notexist();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void getJSON(){
        class GetJSON extends AsyncTask<Void,Void,String> {

            ProgressDialog loading;

            @Override
            protected String doInBackground(Void... voids) {
                getSettings();
                return "done";
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(SettingActivity.this,R.string.gettingdata + "",R.string.pleasewait + "",false,false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(s.equals("done"))
                {
                    loading.dismiss();
                }
                JSON_STRING = s;
            }

        }
        GetJSON gj = new GetJSON();
        gj.execute();
    }

    public void data_exist()
    {
        editTextTopic.setEnabled(false);
        buttonSubscribe.setEnabled(false);
        buttonUnsubscribe.setEnabled(true);
        switchGas.setEnabled(true);
        switchFlame.setEnabled(true);
        switchList.setEnabled(true);
    }

    public void data_notexist()
    {
        switchGas.setEnabled(false);
        switchFlame.setEnabled(false);
        switchList.setEnabled(false);
        editTextTopic.setEnabled(true);
        buttonSubscribe.setEnabled(true);
        buttonUnsubscribe.setEnabled(false);
    }

    public void unSubscribe()
    {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic_app);
        Toast.makeText(SettingActivity.this, getApplicationContext().getResources().getString(R.string.unsubscribefrom)+" "+topic_app+" topic",Toast.LENGTH_LONG).show();
        checkdatauser.child("Topic").removeValue();
        checkdatauser.setValue(0);
        getData();
    }

    public void Subscribe(String topic)
    {
        final String topictocheck = topic;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("topic").child(topic);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    FirebaseMessaging.getInstance().subscribeToTopic(topictocheck);
                    Toast.makeText(SettingActivity.this, getApplicationContext().getResources().getString(R.string.subscribeto)+ " " +topictocheck+" topic",Toast.LENGTH_LONG).show();
                    String data_topic = editTextTopic.getText().toString();
                    checkdatauser.child("Topic").setValue(data_topic);
                    getData();
                }
                else
                {
                    Toast.makeText(SettingActivity.this, getApplicationContext().getResources().getString(R.string.errsub1)+" "+topictocheck+getApplicationContext().getResources().getString(R.string.errsub2), Toast.LENGTH_LONG).show();
                    getData();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    @Override
    public void onClick(View v){
        final String topic = editTextTopic.getText().toString().trim();
        if(v == buttonSubscribe){
            if(topic.equals("")){
                Toast.makeText(SettingActivity.this, R.string.topicisempty, Toast.LENGTH_LONG).show();
            }
            else{
                Subscribe(topic);
            }
        }
        if(v == buttonUnsubscribe){
            unSubscribe();
        }
        if(topic_app != null){
            if(switchGas.isChecked()){
                ref.child("gas_sensor").setValue(true);
            }
            else {
                ref.child("gas_sensor").setValue(false);
            }
            if(switchFlame.isChecked()){
                ref.child("flame_sensor").setValue(true);
            }
            else {
                ref.child("flame_sensor").setValue(false);
            }
            if(switchList.isChecked()){
                ref.child("send_list").setValue(true);
            }
            else {
                ref.child("send_list").setValue(false);
            }
        }
    }
}
