package sheyko.aleksey.mapthetrip.models;

import com.orm.SugarRecord;

import sheyko.aleksey.mapthetrip.utils.RegisterDeviceTask;

public class Trip extends SugarRecord<Trip> {

    String tripId;
//    boolean isSaved;
    float distance = 0;
//    int duration;
//    String name;
//    String note;
//    ArrayList<String> states;
//    ArrayList<String> stateDistances;
//    ArrayList<String> stateDurations;

    public Trip(String deviceId, String deviceType, String isCameraAvailable) {
        new RegisterDeviceTask().execute(deviceId, deviceType, isCameraAvailable);
    }

    public Trip(String tripId) {
        this.tripId = tripId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public float getDistance() {
        return distance;
    }

    public void increazeDistance(float increment) {
        distance = distance + increment;
    }
}
