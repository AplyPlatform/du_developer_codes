package io.droneplay.droneplaymission.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.droneplay.droneplaymission.model.FlightRecordItem;
import io.droneplay.droneplaymission.R;
import io.droneplay.droneplaymission.model.WaypointData;

public class HelperUtils {
    private static HelperUtils uniqueInstance;

    public static HelperUtils getInstance() {

        if (uniqueInstance == null) {
            uniqueInstance = new HelperUtils();
        }

        return uniqueInstance;
    }

    public HelperUtils() {

    }

    private static final String MY_MAP_STYLE = "my_map_style";
    public String dronePlayToken = "";
    public String clientid = "";

    public void saveMapStyle(Context context, int mapStyle) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String registerKey = MY_MAP_STYLE;

        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(registerKey, mapStyle);
        editor.commit();
    }

    public int readMapStyle(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String registerKey = MY_MAP_STYLE;

        return prefs.getInt(registerKey, 0);
    }

    public String getMetadata(Context context, String name) {
        try {
            ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(
                    context.getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo.metaData != null) {
                return appInfo.metaData.getString(name);
            }
        } catch (PackageManager.NameNotFoundException e) {
// if we can’t find it in the manifest, just return null
        }

        return null;
    }


    public String getCurrentLocalDateTimeStamp() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public long getTimeStamp() {
        return System.currentTimeMillis();
    }

    public void uploadFlightRecord(String missionName, ArrayList<FlightRecordItem> items, Handler _handler) {
        if (items.size() == 0) return;

        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"position\",");

        body += "\"daction\":";
        body += ("\"upload\",");

        missionName += ("-" + this.getCurrentLocalDateTimeStamp());

        body += "\"name\":";
        body += ("\"" + missionName + "\",");

        body += "\"data\": ";

        Type listType = new TypeToken<ArrayList<FlightRecordItem>>() {}.getType();
        Gson gson = new Gson();
        String jsonString = gson.toJson(items, listType);
        body += jsonString;

        body += (",");
        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");
        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }


    public void sendMyPosition(FlightRecordItem item) {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"position\",");

        body += "\"daction\":";
        body += ("\"set\",");

        body += "\"lat\":";
        body += ("\"" + item.lat + "\",");

        body += "\"lng\":";
        body += ("\"" + item.lng + "\",");

        body += "\"alt\":";
        body += ("\"" + item.alt + "\",");

        body += "\"act\":";
        body += ("\"" + item.act + "\",");

        body += "\"dsec\":";
        body += ("\"" + item.dsec + "\",");

        body += "\"etc\":{\"battery\":";
        body += ("\"" + item.etc.battery + "\"");

        body += (",\"marked\":\""+ item.etc.marked + "\"");

        body += ("},");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");
        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, null);
        dapi.start();
    }

    public void deleteButtonsFromServer(String buttonid, Handler _handler) {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"mission\",");

        body += "\"daction\":";
        body += ("\"delete\",");

        body += "\"mname\":";
        body += ("\"" + buttonid + "\",");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }

    public void saveButtonsToServer(String buttonid, List<WaypointData> listData, Handler _handler) {

        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"mission\",");

        body += "\"daction\":";
        body += ("\"set\",");

        body += "\"mname\":";
        body += ("\"" + buttonid + "\",");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\",");


        body += "\"missiondata\":";

        Type listType = new TypeToken<List<WaypointData>>() {}.getType();
        Gson gson = new Gson();
        String jsonString = gson.toJson(listData, listType);
        body += jsonString;

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }



    public void loadButtonsFromServer(Handler _handler)
    {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"mission\",");

        body += "\"daction\":";
        body += ("\"get\",");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }


    public interface titleInputClickListener {
        void onTitileInputClick(String buttonTitle) ;
    }

    public void showTitleInputDialog(Context c, final titleInputClickListener listener) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(c);
        View mView = layoutInflaterAndroid.inflate(R.layout.layout_titleinput, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(c);
        alertDialogBuilderUserInput.setView(mView);

        final EditText userInputDialogEditText = mView.findViewById(R.id.userInputDialog);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton("Make", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                        listener.onTitileInputClick(userInputDialogEditText.getText().toString());
                        dialogBox.dismiss();
                    }
                })

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {

                                listener.onTitileInputClick("");
                                dialogBox.dismiss();
                            }
                        });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    public interface markerDataInputClickListener {
        void onMarkerDataInputClick(String markerName, int nHow, int altitude, int act, int actParam, int speed) ;
    }

    public void showMarkerDataInputDialog(final Context c, final String markerName, final int altitude, final int act, final int actParam, final int speed, final markerDataInputClickListener listener) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(c);
        View mView = layoutInflaterAndroid.inflate(R.layout.layout_marker_datainput, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(c);
        alertDialogBuilderUserInput.setView(mView);

        List<String> spinnerItems = new ArrayList<>();
        ArrayAdapter<String> spinnerAdapter=new ArrayAdapter<>(c,
                android.R.layout.simple_spinner_item, spinnerItems);

        spinnerItems.add("STAY");
        spinnerItems.add("START_TAKE_PHOTO");
        spinnerItems.add("START_RECORD");
        spinnerItems.add("STOP_RECORD");
        spinnerItems.add("ROTATE_AIRCRAFT");
        spinnerItems.add("GIMBAL_PITCH");

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        final Spinner spinner = mView.findViewById(R.id.actions);
        spinner.setAdapter(spinnerAdapter);

        final EditText altitudeEdt = mView.findViewById(R.id.altitude);
        final Button btnModify = mView.findViewById(R.id.btnModify);
        final EditText speedEdt = mView.findViewById(R.id.speed);
        final EditText actParamEdt = mView.findViewById(R.id.actparam);

        speedEdt.setText(String.valueOf(speed));
        actParamEdt.setText(String.valueOf(actParam));
        altitudeEdt.setText(String.valueOf(altitude));
        spinner.setSelection(act);
        final AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();

        btnModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int altitude = Integer.parseInt(altitudeEdt.getText().toString());
                if (altitude == 0 || altitude > 500) {
                    Toast.makeText(c, "Invalid altitude", Toast.LENGTH_SHORT).show();
                    return;
                }

                int act = spinner.getSelectedItemPosition();
                int actParam = Integer.parseInt(actParamEdt.getText().toString());
                int speed = Integer.parseInt(speedEdt.getText().toString());

                listener.onMarkerDataInputClick(markerName, 1, altitude, act, actParam, speed);
                alertDialogAndroid.dismiss();
            }
        });

        final Button btnDelete = mView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMarkerDataInputClick(markerName, 0, -1, -1, -1, -1);
                alertDialogAndroid.dismiss();
            }
        });

        alertDialogBuilderUserInput
                .setCancelable(true);


        alertDialogAndroid.show();
    }


    public void displayError(
            final Context context, final String errorMsg, @Nullable final Throwable problem) {
        final String tag = context.getClass().getSimpleName();
        final String toastText;
        if (problem != null && problem.getMessage() != null) {
            Log.e(tag, errorMsg, problem);
            toastText = errorMsg + ": " + problem.getMessage();
        } else if (problem != null) {
            Log.e(tag, errorMsg, problem);
            toastText = errorMsg;
        } else {
            Log.e(tag, errorMsg);
            toastText = errorMsg;
        }

        new Handler(Looper.getMainLooper())
                .post(
                        () -> {
                            Toast toast = Toast.makeText(context, toastText, Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        });
    }

}
