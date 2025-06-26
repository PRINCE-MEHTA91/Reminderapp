package com.example.reminderapp;

import android.app.*;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    EditText titleET, descET, dateEditText, timeEditText;
    Button addReminderBtn, pickDateBtn, pickTimeBtn, logoutBtn;
    ListView reminderListView;

    FirebaseAuth mAuth;
    FirebaseDatabase database;
    DatabaseReference remindersRef;

    ArrayList<String> reminderList;
    ArrayAdapter<String> adapter;

    Calendar reminderTime = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        titleET = findViewById(R.id.reminderTitle);
        descET = findViewById(R.id.reminderDescription);
        dateEditText = findViewById(R.id.dateEditText);
        timeEditText = findViewById(R.id.timeEditText);
        pickDateBtn = findViewById(R.id.pickDateBtn);
        pickTimeBtn = findViewById(R.id.pickTimeBtn);
        addReminderBtn = findViewById(R.id.addReminderBtn);
        logoutBtn = findViewById(R.id.logoutBtn);
        reminderListView = findViewById(R.id.reminderListView);

        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        remindersRef = database.getReference("Reminders").child(user.getUid());

        reminderList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reminderList);
        reminderListView.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminderChannel",
                    "Reminder Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        // Load data
        remindersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                reminderList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Reminder rem = snap.getValue(Reminder.class);
                    if (rem != null && rem.getTitle() != null) {
                        Calendar c = Calendar.getInstance();
                        c.setTimeInMillis(rem.getTimeInMillis());
                        String display = "📌 " + rem.getTitle() + " - " + rem.getDescription()
                                + "\n" + android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", c);
                        reminderList.add(display);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load reminders", Toast.LENGTH_SHORT).show();
            }
        });

        pickDateBtn.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    this::onDateSet,
                    reminderTime.get(Calendar.YEAR),
                    reminderTime.get(Calendar.MONTH),
                    reminderTime.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        pickTimeBtn.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        reminderTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        reminderTime.set(Calendar.MINUTE, minute);
                        reminderTime.set(Calendar.SECOND, 0);
                        timeEditText.setText(String.format("%02d:%02d", hourOfDay, minute));
                    },
                    reminderTime.get(Calendar.HOUR_OF_DAY),
                    reminderTime.get(Calendar.MINUTE),
                    true);
            timePickerDialog.show();
        });

        addReminderBtn.setOnClickListener(v -> {
            String title = titleET.getText().toString().trim();
            String desc = descET.getText().toString().trim();

            if (title.isEmpty() || dateEditText.getText().toString().isEmpty() || timeEditText.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            long timeInMillis = reminderTime.getTimeInMillis();
            String id = remindersRef.push().getKey();
            Log.d("ReminderDebug", "Generated ID: " + id);
            Reminder reminder = new Reminder(id, title, desc, timeInMillis);
            Log.d("ReminderSave", "Saving reminder: " + title + ", " + desc);

            assert id != null;
            remindersRef.child(id).setValue(reminder)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Reminder Saved!", Toast.LENGTH_SHORT).show();
                        scheduleNotification(title, desc, timeInMillis);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void scheduleNotification(String title, String desc, long timeInMillis) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleExactAlarm(title, desc, timeInMillis);
            } else {
                Toast.makeText(this, "Enable exact alarm permission in system settings", Toast.LENGTH_LONG).show();
            }
        } else {
            scheduleExactAlarm(title, desc, timeInMillis);
        }
    }

    private void scheduleExactAlarm(String title, String desc, long timeInMillis) {
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("desc", desc);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    private void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        reminderTime.set(Calendar.YEAR, year);
        reminderTime.set(Calendar.MONTH, month);
        reminderTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        dateEditText.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
    }
}
