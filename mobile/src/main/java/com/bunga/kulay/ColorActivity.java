package com.bunga.kulay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bunga.kulay.model.User;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

import java.util.HashMap;
import java.util.Map;

public class ColorActivity extends AppCompatActivity
        implements ColorFragment.ColorChangedListener {

    private static final long INT_LONG_OFFSET = 0x100000000L;

    private class ConfigKey {
        private static final String RANDOM_TAP_REPETITION_MESSAGE = "random_tap_repetition_message";
        private static final String RANDOM_TAP_REPETITION_COUNT = "random_tap_repetition_count";
    }

    private String mRandomTapRepetitionMessage;
    private int mRandomTapRepetitionCount;
    private int mRandomTaps = 0;

    private MenuItem mRandomButton;
    private MenuItem mSaveButton;

    private ColorFragment mColorFragment;

    private Integer mPreviousFavoriteColor = null;
    private Long mPreviousTimestamp = null;
    private Integer mCurrentFavoriteColor = null;
    private Long mCurrentTimestamp = null;
    private int mFavoriteColorBeforeSignIn;

    private boolean mIsIndicatingBackgroundWork = false;

    public static final int RC_SIGN_IN = 1;
    public static final int RC_SIGN_IN_AFTER_PICKED_COLOR = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        // in debug mode, enable app to fetch more than 5 (default) requests per hour
        remoteConfig.setConfigSettings(new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG).build());
        long cacheExpirationSeconds = 3600L;
        if (remoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            // ensures that fetch will always fetch current data
            cacheExpirationSeconds = 0L;
        }
        remoteConfig.fetch(cacheExpirationSeconds).addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    remoteConfig.activateFetched();

                    mRandomTapRepetitionMessage = remoteConfig.getString(ConfigKey.RANDOM_TAP_REPETITION_MESSAGE);
                    mRandomTapRepetitionCount = (int) remoteConfig.getLong(ConfigKey.RANDOM_TAP_REPETITION_COUNT);
                }
            }
        });

        mColorFragment = (ColorFragment) getSupportFragmentManager().findFragmentById(R.id.fragment);

        if (savedInstanceState == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                loadFavoriteColor(currentUser);
            } else {
                mColorFragment.randomizeColor();
            }
        } else {
            mColorFragment.setColor(savedInstanceState.getInt("current_color"));
            if (savedInstanceState.getBoolean("has_current_favorite_color", false)) {
                mCurrentFavoriteColor = savedInstanceState.getInt("current_favorite_color");
                mCurrentTimestamp = savedInstanceState.getLong("current_timestamp");
            }
        }
    }

    @Override
    protected void onDestroy() {
        mColorFragment = null;
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_color", mColorFragment.getColor());
        if (mCurrentFavoriteColor != null) {
            outState.putBoolean("has_current_favorite_color", true);
            outState.putInt("current_favorite_color", mCurrentFavoriteColor);
            outState.putLong("current_timestamp", mCurrentTimestamp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && currentUser != null) {
                    loadFavoriteColor(currentUser);
                }
                break;
            case RC_SIGN_IN_AFTER_PICKED_COLOR:
                if (resultCode == Activity.RESULT_OK && currentUser != null) {
                    saveFavoriteColor(currentUser, mFavoriteColorBeforeSignIn);
                }
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_color, menu);
        mRandomButton = menu.findItem(R.id.action_random);
        mRandomButton.setEnabled(!mIsIndicatingBackgroundWork);
        mSaveButton = menu.findItem(R.id.action_save);
        mSaveButton.setEnabled(!mIsIndicatingBackgroundWork);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Runs every time the overflow menu is shown - ie. "Log Out"/"Sign In" will be updated properly.
     * @param menu menu
     * @return super
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_log_out).setTitle(
                FirebaseAuth.getInstance().getCurrentUser() == null ?
                R.string.action_sign_in :
                R.string.action_log_out);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_random:
                mColorFragment.randomizeColor();
                break;
            case R.id.action_save:
                final int favoriteColor = mColorFragment.getColor();
                final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser == null) {
                    showSnackbar("Sign in to see more!", Snackbar.LENGTH_LONG, "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mFavoriteColorBeforeSignIn = favoriteColor;
                            launchSignIn(RC_SIGN_IN_AFTER_PICKED_COLOR);
                        }
                    });
                } else if (mCurrentFavoriteColor != null) {
                    if (mCurrentFavoriteColor != favoriteColor) {
                        removeFavoriteColorFromColorList(currentUser, mCurrentFavoriteColor, new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    saveFavoriteColor(currentUser, favoriteColor);
                                } else {
                                    // most likely a developer error - has a color value been saved incorrectly?
                                    stopBackgroundIndicator();
                                }
                            }
                        });
                    } else {
                        showSnackbar("You can't save the same color twice in a row!", Snackbar.LENGTH_SHORT);
                    }
                } else {
                    saveFavoriteColor(currentUser, favoriteColor);
                }
                break;
            case R.id.action_log_out:
                if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                    AuthUI.getInstance().signOut(this).addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()) {
                                showSnackbar("We hope to see you again!", Snackbar.LENGTH_LONG);
                            }
                        }
                    });
                } else {
                    launchSignIn(RC_SIGN_IN);
                }
                break;
            case R.id.action_settings:
                // TODO Add settings, launch activity
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onColorChanged(ColorFragment colorFragment) {
        mRandomTaps = 0;
    }

    @Override
    public void onColorRandomized(ColorFragment colorFragment) {
        mRandomTaps++;
        if (mRandomTaps == mRandomTapRepetitionCount) {
            showSnackbar(mRandomTapRepetitionMessage, Snackbar.LENGTH_LONG);
        }
    }

    private void startBackgroundIndicator() {
        mIsIndicatingBackgroundWork = true;
        mColorFragment.disableInputs();
        if (mRandomButton != null) {
            mRandomButton.setEnabled(false);
        }
        if (mSaveButton != null) {
            mSaveButton.setEnabled(false);
        }
    }

    private void stopBackgroundIndicator() {
        mIsIndicatingBackgroundWork = false;
        mColorFragment.enableInputs();
        if (mRandomButton != null) {
            mRandomButton.setEnabled(true);
        }
        if (mSaveButton != null) {
            mSaveButton.setEnabled(true);
        }
    }

    private void showSnackbar(String message, int length) {
        showSnackbar(message, length, null, null);
    }

    private void showSnackbar(String message, int length, String action, View.OnClickListener listener) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, length);
        if (!TextUtils.isEmpty(action)) {
            snackbar.setAction(action, listener);
        }
        snackbar.show();
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

    private void removeFavoriteColorFromColorList(FirebaseUser currentUser, int favoriteColor, OnCompleteListener<Void> onCompleteListener) {
        startBackgroundIndicator();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        long positiveFavoriteColor = getPositiveColor(favoriteColor);
        databaseReference.child("colors").child("" + positiveFavoriteColor).child(currentUser.getUid())
                .removeValue().addOnCompleteListener(this, onCompleteListener);
    }

    private void saveFavoriteColor(final FirebaseUser currentUser, final int favoriteColor) {
        startBackgroundIndicator();
        showSnackbar("Saving...", Snackbar.LENGTH_LONG);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String uid = currentUser.getUid();
        // save color to database as positive number - we have to convert to long
        // this plays more nicely with iOS color values
        final long timestamp = System.currentTimeMillis();
        long positiveFavoriteColor = getPositiveColor(favoriteColor);

        Map<String, Object> updates = new HashMap<>();

        User user = new User(positiveFavoriteColor, timestamp);
        Map<String, Object> userValues = user.toMap(); // { favoriteColor: $positiveFavoriteColor, timestamp: $timestamp }
        updates.put("/users/" + uid, userValues);

        Map<String, Object> userColorValues = new HashMap<>();
        userColorValues.put("favoriteColor", positiveFavoriteColor);
        updates.put("/user-colors/" + uid + "/" + timestamp, userColorValues);

        Map<String, Object> colorValues = new HashMap<>();
        colorValues.put("timestamp", timestamp);
        updates.put("/colors/" + positiveFavoriteColor + "/" + uid, colorValues);

        databaseReference.updateChildren(updates, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError error, DatabaseReference ref) {
                if (error == null) {
                    mPreviousFavoriteColor = mCurrentFavoriteColor; // enable undo to last favorite color
                    mPreviousTimestamp = mCurrentTimestamp;
                    mCurrentFavoriteColor = favoriteColor;
                    mCurrentTimestamp = timestamp;

                    // enable undo only if this is not the first time user has saved (or loaded) a color
                    String action = mPreviousFavoriteColor != null ? "UNDO" : null;
                    showSnackbar("Saved!", Snackbar.LENGTH_LONG, action, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startBackgroundIndicator();
                            showSnackbar("Undoing your color...", Snackbar.LENGTH_LONG);

                            undoFavoriteColor(currentUser,
                                    mCurrentFavoriteColor, mCurrentTimestamp,
                                    mPreviousFavoriteColor, mPreviousTimestamp);
                        }
                    });
                } else {
                    showSnackbar("Something went wrong...", Snackbar.LENGTH_LONG, "TRY AGAIN", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            saveFavoriteColor(currentUser, favoriteColor);
                        }
                    });
                }

                stopBackgroundIndicator();
            }
        });
    }

    private void loadFavoriteColor(FirebaseUser currentUser) {
        startBackgroundIndicator();
        showSnackbar("Loading your favorite color...", Snackbar.LENGTH_LONG);

        FirebaseDatabase.getInstance().getReference().child("users").child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            mCurrentFavoriteColor = getIntColor(user.favoriteColor);
                            mCurrentTimestamp = user.timestamp;
                            mColorFragment.setColor(mCurrentFavoriteColor);

                            String hex = Integer.toHexString(mCurrentFavoriteColor).toUpperCase();
                            showSnackbar("So, you're a fan of #" + hex + ", too.", Snackbar.LENGTH_LONG);
                            stopBackgroundIndicator();
                        } else {
                            mColorFragment.randomizeColor();

                            showSnackbar("No favorite? Try this!", Snackbar.LENGTH_SHORT);
                            stopBackgroundIndicator();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        stopBackgroundIndicator();
                    }
                });
    }

    private void undoFavoriteColor(final FirebaseUser currentUser,
                                   final int currentFavoriteColor, final long currentTimestamp,
                                   final int previousFavoriteColor, final long previousTimestamp) {
        final DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        final String uid = currentUser.getUid();

        removeFavoriteColorFromColorList(currentUser, currentFavoriteColor, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    databaseReference.child("user-colors").child(uid).child(""+currentTimestamp).removeValue(new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError error, DatabaseReference ref) {
                            if (error == null) {
                                // /colors/ and /users/ will be updated
                                // /user-colors/ will stay the same (because that data already exists)
                                long positivePreviousFavoriteColor = getPositiveColor(previousFavoriteColor);

                                Map<String, Object> updates = new HashMap<>();

                                User user = new User(positivePreviousFavoriteColor, previousTimestamp);
                                // { favoriteColor: $positivePreviousFavoriteColor, timestamp: $timestamp }
                                Map<String, Object> userValues = user.toMap();
                                updates.put("/users/" + uid, userValues);

                                Map<String, Object> colorValues = new HashMap<>();
                                colorValues.put("timestamp", previousTimestamp);
                                updates.put("/colors/" + positivePreviousFavoriteColor + "/" + uid, colorValues);

                                databaseReference.updateChildren(updates, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError error, DatabaseReference ref) {
                                        if (error == null) {
                                            mPreviousFavoriteColor = null;
                                            mPreviousTimestamp = null;
                                            mCurrentFavoriteColor = previousFavoriteColor;
                                            mCurrentTimestamp = previousTimestamp;

                                            mColorFragment.setColor(mCurrentFavoriteColor);
                                            showSnackbar("But it was such a pretty color...", Snackbar.LENGTH_LONG);
                                        }
                                        stopBackgroundIndicator();
                                    }
                                });
                            } else {
                                stopBackgroundIndicator();
                            }
                        }
                    });

                } else {
                    stopBackgroundIndicator();
                }
            }
        });
    }

    /**
     * See {@link #getPositiveColor(int)}.
     * @param modelColor color as it was retrieved from the Firebase database (only positive).
     * @return a color value that can fit into a signed int
     */
    private static int getIntColor(long modelColor) {
        if (modelColor >= INT_LONG_OFFSET) {
            modelColor -= INT_LONG_OFFSET; // ensure that color value can fit into a SIGNED integer
        }
        return (int) modelColor;
    }

    /**
     * When working with the Firebase database, it is nicer to see positive color values.
     * These work especially nicely with the iOS framework's color values.
     * See {@link #getIntColor(long)}.
     *
     * Compare -6073288 ((0x0 - 0x5CABC8) & 0xFFFFFF = 0xA35438 - which DOES fit into a signed int)
     * to 4288894008 (0xFFA35438 - which does NOT fit into a signed int, because Integer.MAX_VALUE = 0x7FFFFFFF).
     *
     * @param color can be negative
     * @return color as a positive integer
     */
    private static long getPositiveColor(int color) {
        return color < 0 ? (long) color + INT_LONG_OFFSET : color;
    }
}
