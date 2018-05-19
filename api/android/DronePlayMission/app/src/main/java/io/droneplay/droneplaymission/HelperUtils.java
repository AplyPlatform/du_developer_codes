package io.droneplay.droneplaymission;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HelperUtils {

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
        Random generator = new Random();
        StringBuilder randomStringBuilder = new StringBuilder();
        int randomLength = generator.nextInt(8);
        char tempChar;
        for (int i = 0; i < randomLength; i++){
            tempChar = (char) (generator.nextInt(96) + 32);
            randomStringBuilder.append(tempChar);
        }
        return randomStringBuilder.toString();
    }

    public interface titleInputClickListener {
        void onTitileInputClick(String buttonTitle) ;
    }

    public static void showTitleInputDialog(Context c, final titleInputClickListener listener) {
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
                            }
                        });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

    public interface markerDataInputClickListener {
        void onMarkerDataInputClick(int altitude, LatLng latLng) ;
    }

    public static void showMarkerDataInputDialog(final Context c, final LatLng latLng, final markerDataInputClickListener listener) {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(c);
        View mView = layoutInflaterAndroid.inflate(R.layout.layout_marker_datainput, null);
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(c);
        alertDialogBuilderUserInput.setView(mView);

        final EditText altitudeEdt = mView.findViewById(R.id.altitude);

        final EditText userInputDialogEditText = (EditText) mView.findViewById(R.id.userInputDialog);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogBox, int id) {

                        String altitudeText = altitudeEdt.getText().toString();
                        int altitude = Integer.parseInt(altitudeText);
                        if (altitude == 0 || altitude > 500) {
                            Toast.makeText(c, "Invalid altitude", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        listener.onMarkerDataInputClick(altitude, latLng);
                    }
                })

                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                            }
                        });

        AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.show();
    }

}
