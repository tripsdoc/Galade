package com.mtios.gasdetector;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
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

import java.util.ArrayList;
import java.util.HashMap;

public class ListActivity extends AppCompatActivity implements ListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener{

    private ListView listView;
    private String JSON_STRING;
    private String token, topic;
    private static final String TAG = "ListActivity";
    private SwipeRefreshLayout swipeRefreshLayout;
    private Button mTambah;
    private int jumlahdata = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        listView = findViewById(R.id.listView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mTambah = (Button) findViewById(R.id.tambah);

        mTambah.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
                Intent intent = new Intent(ListActivity.this, FormActivity.class);
                startActivity(intent);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(this);
        listView.setOnItemClickListener(this);
        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "getInstanceId failed", task.getException());
                    return;
                }
                token = task.getResult().getToken();
                loading();
            }
        });
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
        swipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
                loading();
            }
        });
    }

    private void showPenerima()
    {
        final ArrayList<HashMap<String, String >> mPenerima = new ArrayList<HashMap<String, String>>();
        DatabaseReference datatopic = FirebaseDatabase.getInstance().getReference().child("user").child(token);
        datatopic.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists())
                {
                    topic = (String) dataSnapshot.child("Topic").getValue();
                    final DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("penerima").child(topic);
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists())
                            {
                                listView.setAdapter(null);
                                mPenerima.clear();
                                for(DataSnapshot data: dataSnapshot.getChildren())
                                {
                                    String alamat = "Alamat :" + (String) data.child("alamat").getValue();
                                    String nama = "Nama : " + (String) data.child("nama").getValue();
                                    String nohp = "No Handphone : " + (String) data.child("nohp").getValue();

                                    HashMap<String, String> penerima = new HashMap<>();
                                    penerima.put("nomor", String.valueOf(data.getKey()));
                                    penerima.put("nnama", String.valueOf(data.child("nama").getValue()));
                                    penerima.put("aalamat", String.valueOf(data.child("alamat").getValue()));
                                    penerima.put("nnohp", String.valueOf(data.child("nohp").getValue()));
                                    penerima.put("nama", nama);
                                    penerima.put("alamat", alamat);
                                    penerima.put("nohp", nohp);
                                    mPenerima.add(penerima);
                                }
                                jumlahdata = mPenerima.size();
                                Log.v(TAG, String.valueOf(jumlahdata));
                                ListAdapter adapter = new SimpleAdapter(
                                        ListActivity.this, mPenerima, R.layout.list_item,
                                        new String[]{"nama", "alamat", "nohp"},
                                        new int[]{R.id.name, R.id.address, R.id.nohp})
                                {
                                    @Override
                                    public View getView (final int position, View convertView, ViewGroup parent)
                                    {
                                        View v = super.getView(position, convertView, parent);
                                        ImageButton b=(ImageButton)v.findViewById(R.id.delete);
                                        ImageButton c=(ImageButton)v.findViewById(R.id.update);
                                        b.setOnClickListener(new View.OnClickListener() {

                                            @Override
                                            public void onClick(View arg0) {
                                                // TODO Auto-generated method stub
                                                String data = mPenerima.get(position).get("nomor");
                                                String nama = mPenerima.get(position).get("nama");
                                                ref.child(data).setValue(null);
                                                Toast.makeText(ListActivity.this,nama + R.string.deletesuccess ,Toast.LENGTH_SHORT).show();
                                                loading();
                                            }
                                        });


                                        c.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View arg0) {
                                                onBackPressed();
                                                String nama = mPenerima.get(position).get("nnama");
                                                String alamat = mPenerima.get(position).get("aalamat");
                                                String nohp = mPenerima.get(position).get("nnohp");
                                                String id = mPenerima.get(position).get("nomor");
                                                Intent intent = new Intent(getBaseContext(), UpdateActivity.class);
                                                intent.putExtra("nama", nama);
                                                intent.putExtra("alamat", alamat);
                                                intent.putExtra("nohp", nohp);
                                                intent.putExtra("id", id);
                                                startActivity(intent);
                                            }
                                        });

                                        return v;
                                    }
                                };
                                listView.setAdapter(adapter);
                                listView.setEmptyView(findViewById(R.id.emptyElement));
                                ref.setValue(0);
                                for(int i = 0; i < mPenerima.size(); i++)
                                {
                                    int j = i + 1;
                                    String nama = mPenerima.get(i).get("nnama");
                                    String alamat = mPenerima.get(i).get("aalamat");
                                    String nohp = mPenerima.get(i).get("nnohp");
                                    ref.child(String.valueOf(j)).child("nama").setValue(nama);
                                    ref.child(String.valueOf(j)).child("alamat").setValue(alamat);
                                    ref.child(String.valueOf(j)).child("nohp").setValue(nohp);
                                }
                                final DatabaseReference size = FirebaseDatabase.getInstance().getReference().child("size").child(topic);
                                size.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        size.setValue(mPenerima.size());
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
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void loading()
    {
        class GetJSON extends AsyncTask<Void, Void, String > {
            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                swipeRefreshLayout.setRefreshing(true);
                loading = ProgressDialog.show(ListActivity.this,String.valueOf(R.string.gettingdata),String.valueOf(R.string.pleasewait),false,false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if(s.equals("done"))
                {
                    swipeRefreshLayout.setRefreshing(false);
                    loading.dismiss();
                }
                JSON_STRING = s;
            }

            @Override
            protected String doInBackground(Void... params) {
                showPenerima();
                return "done";
            }
        }
        GetJSON gj = new GetJSON();
        gj.execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

    }

    @Override
    public void onRefresh() {
        loading();
    }
}
