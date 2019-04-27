package io.droneplay.droneplaymission.utils;

import android.app.Activity;
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
import android.text.TextUtils;
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

    public String fid = "";

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
/*
    public static boolean isExistMission(String buttonid) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + File.separator + "/DronePlay/AppData" + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            return false;
        }

        path += "missions_" + buttonid;
        File data = new File(path);
        return data.exists();
    }

    public static void deleteMissionsFromFile(String buttonid) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + File.separator + "/DronePlay/AppData" + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }
        path += "missions_" + buttonid;
        File data = new File(path);
        data.delete();
    }

    public static String saveMissionsToFile(String buttonid, List<WaypointData> listData) throws IOException {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/DronePlay/AppData" + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        path += "missions_" + buttonid;

        File data = new File(path);
        if(!data.exists()) {
            data.createNewFile();
            if(data.exists() == false) return "";
        }

        OutputStream outputStream = null;
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                .create();
        try {
            outputStream = new FileOutputStream(data);
            BufferedWriter bufferedWriter;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,
                        StandardCharsets.UTF_8));
            } else {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            }

            Type type = new TypeToken<ArrayList<WaypointData>>() { }.getType();
            gson.toJson(listData, type, bufferedWriter);
            bufferedWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {

                }
            }
        }

        return path;
    }

    public static List<WaypointData> loadMissionsFromFile(String buttonid) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + File.separator + "/DronePlay/AppData" + File.separator;
        path += "missions_" + buttonid;

        File data = new File(path);
        if(!data.exists()) {
            return new ArrayList<WaypointData>();
        }

        List<WaypointData> jsonData = new ArrayList<WaypointData>();

        InputStream inputStream;
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                .create();
        try {
            inputStream = new FileInputStream(data);
            InputStreamReader streamReader;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                streamReader = new InputStreamReader(inputStream,
                        StandardCharsets.UTF_8);
            } else {
                streamReader = new InputStreamReader(inputStream, "UTF-8");
            }

            Type type = new TypeToken<ArrayList<WaypointData>>() { }.getType();
            jsonData = gson.fromJson(streamReader, type);
            streamReader.close();

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            return jsonData;
        }
    }

    public static List<WaypointData> loadMissionFromFile(String name) {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + File.separator + "/DronePlay/AppData" + File.separator;
        path += "missions_" + buttonid;

        File data = new File(path);
        if(!data.exists()) {
            return new ArrayList<WaypointData>();
        }

        List<WaypointData> jsonData = new ArrayList<WaypointData>();

        InputStream inputStream;
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                .create();
        try {
            inputStream = new FileInputStream(data);
            InputStreamReader streamReader;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                streamReader = new InputStreamReader(inputStream,
                        StandardCharsets.UTF_8);
            } else {
                streamReader = new InputStreamReader(inputStream, "UTF-8");
            }

            Type type = new TypeToken<ArrayList<WaypointData>>() { }.getType();
            jsonData = gson.fromJson(streamReader, type);
            streamReader.close();

        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            return jsonData;
        }
    }

    public static ArrayList<MainListItem> loadButtonsFromFile() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()  + File.separator + "/DronePlay/AppData" + File.separator;
        path += "buttons";

        File data = new File(path);
        if(!data.exists()) {
            return null;
        }

        ArrayList<MainListItem> jsonData = null;

        InputStream inputStream = null;
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                .create();
        try {
            inputStream = new FileInputStream(data);
            InputStreamReader streamReader;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                streamReader = new InputStreamReader(inputStream,
                        StandardCharsets.UTF_8);
            } else {
                streamReader = new InputStreamReader(inputStream, "UTF-8");
            }

            Type type = new TypeToken<ArrayList<MainListItem>>() { }.getType();
            jsonData = gson.fromJson(streamReader, type);
            streamReader.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {

                }
            }
        }
        return jsonData;
    }

    public static String saveButtonsToFile(ArrayList<MainListItem> listViewItemList) throws IOException {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "/DronePlay/AppData" + File.separator;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        path += "buttons";

        File data = new File(path);
        if(!data.exists()) {
            data.createNewFile();
            if(data.exists() == false) return "";
        }

        OutputStream outputStream = null;
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().setPrettyPrinting()
                .create();
        try {
            outputStream = new FileOutputStream(data);
            BufferedWriter bufferedWriter;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream,
                        StandardCharsets.UTF_8));
            } else {
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
            }

            Type type = new TypeToken<ArrayList<MainListItem>>() { }.getType();
            gson.toJson(listViewItemList, type, bufferedWriter);
            bufferedWriter.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {

                }
            }
        }

        return path;
    }

    public static void saveButtons(ArrayList<MainListItem> listViewItemList) {

        try {
            HelperUtils.saveButtonsToFile(listViewItemList);
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public static String generateButtonID(ArrayList<MainListItem> listViewItemList) {

        boolean bTryAgain;
        String newButtonID;

        do {
            bTryAgain = false;
            newButtonID = newRandomString();
            for (MainListItem item : listViewItemList) {
                if (item.getId() == newButtonID) {
                    bTryAgain = true;
                    break;
                }
            }
        }while(bTryAgain == true);


        return newButtonID;
    }

    private static String newRandomString() {
        SecureRandom rnd = new SecureRandom();

        StringBuilder sb = new StringBuilder( 8 );
        for( int i = 0; i < 8; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

*/


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

    public void processDronePlayRegister(String name, String email, String phonenumber, String captchaToken, Handler _handler) {

        //
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"member\",");

        body += "\"device\":";
        body += ("\"android\",");

        body += "\"daction\":";
        body += ("\"register\",");

        body += "\"socialid\":";
        body += ("\"" + email + "\",");

        body += "\"name\":";
        body += ("\"" + name + "\",");

        body += "\"phone_number\":";
        body += ("\"" + phonenumber + "\",");

        body += "\"captcha_token\":";
        body += ("\"" + captchaToken + "\",");

        body += "\"emailid\":";
        body += ("\"" + this.fid + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }

    public void getDronePlayToken(String accessToken, String serviceKind, Handler _handler) {

        //
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"member\",");

        body += "\"daction\":";
        body += ("\"login\",");

        body += "\"token\":";
        body += ("\"" + accessToken + "\",");

        body += "\"kind\":";
        body += ("\"" + serviceKind + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }

    public final static boolean isValidEmail(CharSequence target) {
        if (TextUtils.isEmpty(target)) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    public boolean validCellPhone(String number)
    {
        return android.util.Patterns.PHONE.matcher(number).matches();
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



    public void dromiListDeleteFromServer(String buttonId, Handler _handler)
    {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"dromi\",");

        body += "\"daction\":";
        body += ("\"delete\",");

        body += "\"name\":";
        body += ("\"" + buttonId + "\",");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }



    public void loadDromiListFromServer(Handler _handler)
    {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"dromi\",");

        body += "\"daction\":";
        body += ("\"list\",");

        body += "\"clientid\":";
        body += ("\"" + this.clientid + "\"");

        body += "}";

        DronePlayAPISupport dapi = new DronePlayAPISupport();
        dapi.setAction(this.dronePlayToken, body, _handler);
        dapi.start();
    }

    public void loadPositionsFromServer(String buttonID, Handler _handler)
    {
        String body = "";

        body += "{";

        body += "\"action\":";
        body += ("\"dromi\",");

        body += "\"daction\":";
        body += ("\"get\",");

        body += "\"name\":";
        body += ("\"" + buttonID + "\",");

        body += ",\"clientid\":";
        body += ("\"" + this.clientid + "\"");

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

        final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton("Make", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {
                        listener.onTitileInputClick(userInputDialogEditText.getText().toString());
                    }
                })

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                                listener.onTitileInputClick("");
                            }
                        });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    public interface markerDataInputClickListener {
        void onMarkerDataInputClick(String markerName, int nHow, int altitude, int act, int actParam, int speed) ;
    }

    public void showMarkerDataInputDialog(final Context c, final String markerName, final int altitude, final markerDataInputClickListener listener) {
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
            }
        });

        final Button btnDelete = mView.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                listener.onMarkerDataInputClick(markerName, 0, -1, -1, -1, -1);
            }
        });


        altitudeEdt.setText(String.valueOf(altitude));
        alertDialogBuilderUserInput
                .setCancelable(true);

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
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
