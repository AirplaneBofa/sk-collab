package com.iyxan23.sketch.collab.online;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.iyxan23.sketch.collab.R;
import com.iyxan23.sketch.collab.adapters.BrowseItemAdapter;
import com.iyxan23.sketch.collab.models.BrowseItem;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class BrowseActivity extends AppCompatActivity {

    // Used to cache uid -> names
    HashMap<String, String> cached_names = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse);

        RecyclerView rv = findViewById(R.id.browse_rv);
        rv.setLayoutManager(new LinearLayoutManager(this));

        BrowseItemAdapter adapter = new BrowseItemAdapter(this);
        rv.setAdapter(adapter);

        if (savedInstanceState != null) {
            if (!savedInstanceState.isEmpty()) {
                adapter.updateView(savedInstanceState.getParcelableArrayList("items"));
                return;
            }
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        CollectionReference projects = firestore.collection("projects");
        CollectionReference userdata = firestore.collection("userdata");

        // TODO: RECYCLERVIEW PAGINATION
        new Thread(() -> {
            Task<QuerySnapshot> task = projects.get();

            try {
                Tasks.await(task);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(BrowseActivity.this, "An error occured while fetching data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }

            if (!task.isSuccessful()) {
                Toast.makeText(BrowseActivity.this, "An error occured while retrieving data: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            ArrayList<BrowseItem> items = new ArrayList<>();

            for (DocumentSnapshot project: task.getResult().getDocuments()) {

                String username;
                String uid_uploader = project.getString("author");

                if (cached_names.containsKey(uid_uploader)) {
                    username = cached_names.get(uid_uploader);

                } else {
                    // Fetch it's username
                    Task<DocumentSnapshot> userdata_fetch = userdata.document(uid_uploader).get();

                    try {
                        Tasks.await(userdata_fetch);

                        if (!userdata_fetch.isSuccessful()) {
                            Toast.makeText(BrowseActivity.this, "Error while fetching userdata: " + userdata_fetch.getException().getMessage(), Toast.LENGTH_LONG).show();

                            return;
                        }

                        DocumentSnapshot user = userdata_fetch.getResult();

                        username = user.getString("name");

                        cached_names.put(uid_uploader, username);

                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                        Toast.makeText(BrowseActivity.this, "Error while fetching userdata: " + e.getMessage(), Toast.LENGTH_LONG).show();

                        return;
                    }
                }

                items.add(
                        new BrowseItem(
                                project.getId(),
                                username,
                                project.getString("name"),
                                uid_uploader,
                                // TODO: THIS
                                // project.getTimestamp("last_updated_timestamp") == null ? project.child("last_updated_timestamp").getValue(int.class) : 0
                                new Timestamp(0,0)  // Temporarily Hardcoded
                        )
                );
            }

            if (savedInstanceState != null) savedInstanceState.putParcelableArrayList("items", items);

            runOnUiThread(() -> adapter.updateView(items));
        }).start();
    }
}
