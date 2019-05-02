package io.droneplay.droneplaymission.utils;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointAction;
import dji.common.mission.waypoint.WaypointActionType;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionGotoWaypointMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.keysdk.FlightControllerKey;
import dji.keysdk.KeyManager;
import io.droneplay.droneplaymission.model.MainListItem;
import io.droneplay.droneplaymission.model.WaypointData;

import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LATITUDE;
import static dji.keysdk.FlightControllerKey.HOME_LOCATION_LONGITUDE;

/**
 * Created by gunman on 2018. 2. 12..
 */

public class WaypointManager {

    private static WaypointManager uniqueInstance;

    private static final double ONE_METER_OFFSET = 0.00000899322;
    private List<Waypoint> mwaypointList = new ArrayList<>();

    private ArrayList<MainListItem> mainList = null;
    private ArrayList<WaypointData>  tempwaypointList;

    private String buttonID = "";

    public static WaypointManager getInstance() {

        if (uniqueInstance == null) {
            uniqueInstance = new WaypointManager();
        }

        return uniqueInstance;
    }

    public WaypointManager() {

    }

    public void setMainList(ArrayList<MainListItem> mainList) {
        this.mainList = mainList;
    }

    private ArrayList<WaypointData> loadMissions(String buttonID) {

        if (buttonID == null || buttonID.equalsIgnoreCase("")) return null;

        for(MainListItem item : mainList) {
            if (item.name.equalsIgnoreCase(buttonID)) {
                return item.mission;
            }
        }

        return null;
    }

    public String getMissionID() {
        return buttonID;
    }

    public void setMission(String buttonID) {
        this.buttonID = buttonID;
        tempwaypointList = loadMissions(buttonID);
    }

    public void clear() {
        mwaypointList.clear();
        tempwaypointList.clear();
    }

    public void removeAction(String index) {
        for( WaypointData data : tempwaypointList) {
            if (data.id == index) {
                tempwaypointList.remove(data);
                return;
            }
        }
    }

    public void saveMissionToServer(Context context, String buttonID, Handler handler) {

        if (tempwaypointList.size() <= 0) {
            return;
        }

        HelperUtils.getInstance().saveButtonsToServer(buttonID, tempwaypointList, handler);
    }

/*
    public void saveMissionToFile(Context context, String buttonID) {

        if (tempwaypointList.size() <= 0) {
            Toast.makeText(context, "At least, one mission point must be exist !!!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            HelperUtils.saveMissionsToFile(buttonID, tempwaypointList);
            Toast.makeText(context, "Successfully, Mission is saved.", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, "Failed to save mission !!!", Toast.LENGTH_SHORT).show();
        }
    }
*/

    public int addAction(String markerName, double lat, double lng, float alt, int action, int actParam, int speed) {
        WaypointData data = new WaypointData();
        data.act = action;
        data.id = markerName;
        data.alt = alt;
        data.lng = lng;
        data.lat = lat;
        data.speed = speed;
        data.actparam = actParam;
        tempwaypointList.add(data);
        return tempwaypointList.size();
    }

    public void modifyAction(String markerName, float altitude, int act, int actParam, int speed) {
        WaypointData d = getAction(markerName);
        if (d == null) return;

        d.alt = altitude;
        d.act = act;
        d.actparam = actParam;
        d.speed = speed;
    }

    public WaypointData getData(String markerName) {
        WaypointData d = getAction(markerName);
        return d;
    }

    private WaypointData getAction(String markerName) {

        for(WaypointData d : tempwaypointList) {
            if (d.id == null) continue;

            if (d.id.equalsIgnoreCase(markerName)) {
                return d;
            }
        }

        return null;
    }

    public WaypointMission getWaypointMission() {
        if (tempwaypointList == null || tempwaypointList.size() <= 0) return null;

        mwaypointList.clear();

        for( WaypointData data : tempwaypointList ) {

            Waypoint eachWaypoint = new Waypoint(data.lat,data.lng,data.alt);
            eachWaypoint.addAction(new WaypointAction(WaypointActionType.values()[data.act], data.actparam));
            mwaypointList.add(eachWaypoint);
        }

        WaypointMission.Builder mbuilder = new WaypointMission.Builder();

        mbuilder.autoFlightSpeed(5f);
        mbuilder.maxFlightSpeed(10f);
        mbuilder.setExitMissionOnRCSignalLostEnabled(false);
        mbuilder.finishedAction(WaypointMissionFinishedAction.GO_HOME);
        mbuilder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        mbuilder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        mbuilder.headingMode(WaypointMissionHeadingMode.AUTO);
        mbuilder.repeatTimes(1);

        mbuilder.waypointList(mwaypointList).waypointCount(mwaypointList.size());
        return mbuilder.build();
    }

    /**
     * Randomize a WaypointMission
     *
     * @param numberOfWaypoint total number of Waypoint to randomize
     * @param numberOfAction total number of Action to randomize
     */
    public WaypointMission createRandomWaypointMission(int numberOfWaypoint, int numberOfAction) {
        WaypointMission.Builder builder = new WaypointMission.Builder();
        double baseLatitude = 22;
        double baseLongitude = 113;
        Object latitudeValue = KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LATITUDE)));
        Object longitudeValue =
                KeyManager.getInstance().getValue((FlightControllerKey.create(HOME_LOCATION_LONGITUDE)));
        if (latitudeValue != null && latitudeValue instanceof Double) {
            baseLatitude = (double) latitudeValue;
        }
        if (longitudeValue != null && longitudeValue instanceof Double) {
            baseLongitude = (double) longitudeValue;
        }

        final float baseAltitude = 20.0f;
        builder.autoFlightSpeed(5f);
        builder.maxFlightSpeed(10f);
        builder.setExitMissionOnRCSignalLostEnabled(false);
        builder.finishedAction(WaypointMissionFinishedAction.NO_ACTION);
        builder.flightPathMode(WaypointMissionFlightPathMode.NORMAL);
        builder.gotoFirstWaypointMode(WaypointMissionGotoWaypointMode.SAFELY);
        builder.headingMode(WaypointMissionHeadingMode.AUTO);
        builder.repeatTimes(1);
        Random randomGenerator = new Random(System.currentTimeMillis());
        List<Waypoint> waypointList = new ArrayList<>();
        for (int i = 0; i < numberOfWaypoint; i++) {
            final double variation = (Math.floor(i / 4) + 1) * 2 * ONE_METER_OFFSET;
            final float variationFloat = (baseAltitude + (i + 1) * 2);
            final Waypoint eachWaypoint = new Waypoint(baseLatitude + variation * Math.pow(-1, i) * Math.pow(0, i % 2),
                    baseLongitude + variation * Math.pow(-1, (i + 1)) * Math.pow(0, (i + 1) % 2),
                    variationFloat);
            for (int j = 0; j < numberOfAction; j++) {
                final int randomNumber = randomGenerator.nextInt() % 6;
                switch (randomNumber) {
                    case 0:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.STAY, 1));
                        break;
                    case 1:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
                        break;
                    case 2:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_RECORD, 1));
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.STOP_RECORD, 1));
                        break;
                    case 3:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.GIMBAL_PITCH,
                                randomGenerator.nextInt() % 45 - 45));
                        break;
                    case 4:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.ROTATE_AIRCRAFT,
                                randomGenerator.nextInt() % 180));
                        break;
                    default:
                        eachWaypoint.addAction(new WaypointAction(WaypointActionType.START_TAKE_PHOTO, 1));
                        break;
                }
            }
            waypointList.add(eachWaypoint);
        }
        builder.waypointList(waypointList).waypointCount(waypointList.size());
        return builder.build();
    }

}
