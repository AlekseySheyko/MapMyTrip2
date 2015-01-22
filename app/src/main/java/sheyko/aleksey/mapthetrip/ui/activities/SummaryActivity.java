package sheyko.aleksey.mapthetrip.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import sheyko.aleksey.mapthetrip.R;
import sheyko.aleksey.mapthetrip.models.Trip;
import sheyko.aleksey.mapthetrip.utils.tasks.GetSummaryInfoTask;
import sheyko.aleksey.mapthetrip.utils.tasks.GetSummaryInfoTask.OnStatesDataRetrieved;
import sheyko.aleksey.mapthetrip.utils.tasks.SaveTripTask;
import sheyko.aleksey.mapthetrip.utils.tasks.SendLocationTask;
import sheyko.aleksey.mapthetrip.utils.tasks.SendLocationTask.OnLocationSent;

public class SummaryActivity extends Activity
        implements OnStatesDataRetrieved, OnLocationSent {

    private String mTripId;
    private int mDuration;
    private String mDistance;
    private String mStartTime;
    private String mStateCodes;
    private String mStateDistances;
    private String mTotalDistance;
    private String mStateDurations;
    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_summary);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        Trip currentTrip = getIntent().getExtras().getParcelable("CurrentTrip");
        // Get trip info
        mDistance = currentTrip.getDistance();
        mDuration = currentTrip.getDuration();
        mStartTime = currentTrip.getStartTime();
        mTripId = currentTrip.getTripId();
        if (mTripId == null) mTripId = sharedPrefs.getString("trip_id", "");

        // Update UI
        ((TextView) findViewById(R.id.TripLabelDistance)).setText(mDistance);
        ((EditText) findViewById(R.id.tripNameField)).setHint("Trip on " + mStartTime);
    }

    public void finishSession(View view) {
        finishSession(true);
    }

    private void finishSession(boolean isSaved) {
        sharedPrefs.edit().putBoolean("is_saved", isSaved);

        if (isOnline()) {
            setProgressBarIndeterminateVisibility(true);
            sendCoordinatesToServer();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(SummaryActivity.this);
            builder.setTitle("Network lost");
            builder.setMessage("Please wait for a network to update trip status");
            builder.setIcon(R.drawable.ic_action_airplane_mode_on);
            // Add the buttons
            builder.setPositiveButton("Wait", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                    dialog.cancel();
                }
            });
            builder.setNegativeButton("Finish", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User confirm exit
                    startActivity(new Intent(SummaryActivity.this, MainActivity.class));
                }
            });
            // Create the AlertDialog
            AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private void sendCoordinatesToServer() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Coordinates");
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> coordinates, ParseException e) {
                for (ParseObject coordinate : coordinates) {
                    String tripId = PreferenceManager.getDefaultSharedPreferences(SummaryActivity.this)
                            .getString("trip_id", "");
                    coordinate.put("trip_id", tripId);
                }
                if (isOnline()) {
                    new SendLocationTask(SummaryActivity.this, SummaryActivity.this).execute(coordinates);
                    for (ParseObject coordinate : coordinates) {
                        coordinate.unpinInBackground();
                    }
                }
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void saveTrip(boolean isSaved) {
        String tripName = ((EditText) findViewById(R.id.tripNameField)).getText().toString();
        if (tripName.equals("")) tripName = "Trip on " + mStartTime;
        String tripNotes = ((EditText) findViewById(R.id.tripNotesField)).getText().toString();

        new SaveTripTask(this).execute(
                mTripId, isSaved + "", mTotalDistance,
                mDuration + "", tripName, tripNotes,
                mStateCodes, mStateDistances, mStateDurations
        );
        setProgressBarIndeterminateVisibility(false);

        startActivity(new Intent(this, StatsActivity.class)
                .putExtra("total_distance", mDistance)
                .putExtra("state_codes", mStateCodes)
                .putExtra("state_distances", mStateDistances));
    }

    public void cancelTrip(View view) {
        cancelTrip();
    }

    private void cancelTrip() {
        AlertDialog.Builder builder = new AlertDialog.Builder(SummaryActivity.this);
        builder.setTitle(R.string.discard_trip_dialog_title);
        builder.setMessage(R.string.discard_trip_dialog_message);
        builder.setIcon(R.drawable.ic_action_discard);
        // Add the buttons
        builder.setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User confirm exit
                finishSession(false);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });
        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onStatesDataRetrieved(String stateCodes, String stateDistances, String totalDistance, String statesDurations) {
        mStateCodes = stateCodes;
        mStateDistances = stateDistances;
        mTotalDistance = totalDistance;
        mStateDurations = statesDurations;

        saveTrip(sharedPrefs.getBoolean("is_saved", true));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // do something useful
                cancelTrip();
                return (true);
        }
        return (super.onOptionsItemSelected(item));
    }

    @Override
    public void onLocationSent() {
        new GetSummaryInfoTask(this).execute(mTripId);
    }
}
