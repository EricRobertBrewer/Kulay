package com.uberprinny.kulay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.uberprinny.kulay.model.User;

import java.util.HashMap;
import java.util.Map;

public class ColorActivity extends AppCompatActivity
        implements ColorActivityFragment.ColorPickListener {

    private ColorActivityFragment mColorActivityFragment;
    private boolean mIsInputShown;
    private int mLastSavedColor;
    private int mPickedColorBeforeSignIn;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_SIGN_IN_AFTER_PICKED_COLOR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mColorActivityFragment = (ColorActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);
        mIsInputShown = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("is-input-shown", false);
        invalidateOptionsMenu();
        mColorActivityFragment.toggleShowInput(mIsInputShown);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseDatabase.getInstance().getReference().child("users").child(user.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            User userColor = dataSnapshot.getValue(User.class);
                            mLastSavedColor = userColor.favoriteColor;
                            mColorActivityFragment.setColor(mLastSavedColor);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                        }
                    });
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pickedColor = mColorActivityFragment.getPickedColor();
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    mPickedColorBeforeSignIn = pickedColor;
                    Snackbar.make(view, "Sign in to see more!", Snackbar.LENGTH_LONG)
                            .setAction("OK", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    launchSignIn(RC_SIGN_IN_AFTER_PICKED_COLOR);
                                }
                            })
                            .show();
                } else {
                    savePickedColor(pickedColor);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean("is-input-hidden", mIsInputShown)
                .apply();
    }

    @Override
    protected void onDestroy() {
        mColorActivityFragment = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN:
                break;
            case RC_SIGN_IN_AFTER_PICKED_COLOR:
                if (resultCode == Activity.RESULT_OK && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    savePickedColor(mPickedColorBeforeSignIn);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_color, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_log_out).setTitle(
                FirebaseAuth.getInstance().getCurrentUser() == null ?
                R.string.action_sign_in :
                R.string.action_log_out);
        menu.findItem(R.id.action_toggle_color_value_input).setIcon(
                mIsInputShown ? R.drawable.ic_expand_less_white_24dp : R.drawable.ic_expand_more_white_24dp);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                break;
            case R.id.action_toggle_color_value_input:
                mIsInputShown = !mIsInputShown;
                mColorActivityFragment.toggleShowInput(mIsInputShown);
                invalidateOptionsMenu();
                break;
            case R.id.action_log_out:
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    AuthUI.getInstance().signOut(this);
                } else {
                    launchSignIn(RC_SIGN_IN);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchSignIn(int requestCode) {
        Intent signInIntent = AuthUI.getInstance().createSignInIntentBuilder()
                .setTheme(R.style.AppTheme)
                .setProviders(AuthUI.GOOGLE_PROVIDER, AuthUI.EMAIL_PROVIDER)
                //.setTosUrl()
                .setLogo(R.mipmap.ic_launcher)
                .build();
        startActivityForResult(signInIntent, requestCode);
    }

    private void savePickedColor(final int pickedColor) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
            final String uid = user.getUid();

            databaseReference.child("colors").child("" + mLastSavedColor).child("" + uid)
                    .removeValue().addOnSuccessListener(this, new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    long timestamp = System.currentTimeMillis();

                    String userColorKey = databaseReference.child("user-colors").child(uid).push().getKey();
                    User aUser = new User(pickedColor, timestamp);
                    Map<String, Object> userValues = aUser.toMap();
                    userValues.put("timestamp", timestamp);

                    Map<String, Object> userUpdates = new HashMap<>();
                    userUpdates.put("/users/" + uid, userValues);
                    userUpdates.put("/user-colors/" + uid + "/" + userColorKey, userValues);
                    Map<String, Object> colorValues = new HashMap<>();
                    colorValues.put("timestamp", timestamp);
                    userUpdates.put("/colors/" + pickedColor + "/" + uid, colorValues);

                    databaseReference.updateChildren(userUpdates, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            mLastSavedColor = pickedColor;
                        }
                    });
                }
            });
        }
    }
}
