//TODO add a code copyright
//TODO organize functions
package com.example.fieldassetmanagement;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

public class SingleAssetPage extends AppCompatActivity implements
        OnItemSelectedListener,
        LocationGPSResultDialog.GPSResultListener,
        ImageOverwritePopDialog.imgOverwriteListener{
    // GPS variables
    private LocationManager managerGPS;
    private final long minTimeUpdates = 50;
    private final float minDistanceUpdates = 0; // setting to zero means it is non-movement based updates
    private final long checkGPStime = 4000;
    private final double accThres = 3.0;

    // SingleAssetPage local variables
    private String fileName;
    private List<String[]> curCSV;
    private Uri csvURI, imageURI, imgPath;
    private int row;
    private boolean side;

    // Shared Preference Constants
    public static final String ROW_PREFERENCES = "rowPrev";
    private static final int REQUEST_IMAGE_CAPTURE = 5;

    // All Spinner declarations
    private Spinner AssetName, C1LSpin, C1RSpin;

    // All Spinner option declarations
    private String[] AssetNameOptions, C1LOptions, C1ROptions;

    ImageView mastHead;
    ImageButton mapsButton;
    ProgressBar progressGPS;
    Button nextSave, prevSave, photoL, photoR, getGPS;
    TextView longitude, latitude, C1Ltext, C1Rtext;
    // TODO update the button backgrouns on return from camera

    @Override
    protected void onPause() {   // Any time the SingleAssetActivity leaves the foreground, the info is saved.
        super.onPause();
        // save current GUI items to curCSV
        pullGUIEntries();
        // save curCSV over top of old CSV
        saveGUIEntries();
        // save current row to SharedPreferences
        saveRowPreferences();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        pushGUIEntries();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onStart() {
        super.onStart();
        // Start requesting GPS updates
        managerGPS.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeUpdates, minDistanceUpdates, listenerGPS);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop requesting GPS updates
        managerGPS.removeUpdates(listenerGPS);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_asset_page);
        // Create GPS reference
        managerGPS = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        getExtraData();
        // Load data from selected CSV file
        try {
            curCSV = loadCSVfromURI(csvURI);   // Load 2D arrayList from File
            Toast.makeText(this, "" + fileName + " data loaded succesfully!", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) { // Throw FileNotFoundException if no file found
            //TODO set curCSV to a blank arrayList so the program doesn't crash
            Toast.makeText(this, "File Not Found Exception: " + fileName, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "I/O Exception: " + fileName, Toast.LENGTH_LONG).show();

        }
        setupGUIEntries();
        pushGUIEntries();
    }

    private void getExtraData() {
        Intent openIntent = getIntent();
        String EXTRA_SAP_ASSET_URI = openIntent.getStringExtra(LandingPage.EXTRA_SAP_ASSET_URI);
        String EXTRA_SAP_IMAGE_URI = openIntent.getStringExtra(LandingPage.EXTRA_SAP_IMAGE_URI);
        int EXTRA_SAP_ROW = openIntent.getIntExtra(LandingPage.EXTRA_SAP_ROW, 1);
        String EXTRA_SAP_FILENAME = openIntent.getStringExtra(LandingPage.EXTRA_SAP_FILENAME);
        csvURI = Uri.parse(EXTRA_SAP_ASSET_URI); // Convert EXTRA_SAP_ASSET_URI data back to a URI
        if (EXTRA_SAP_IMAGE_URI != null) {
            imageURI = Uri.parse(EXTRA_SAP_IMAGE_URI); // Convert EXTRA_SAP_IMAGE_URI data back to URI
        }
        fileName = EXTRA_SAP_FILENAME; // Get Short-form fileName from MainActivity
        row = EXTRA_SAP_ROW; // Get loaded Shared Preference Row from MainActivity
    }

    // General Spinner Listeners
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item = parent.getItemAtPosition(position).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    //Next Asset View: Save current entries then load next if not at end of file
    private void nextAssetView() {
        // save current GUI items to curCSV
        pullGUIEntries();
        // save curCSV over top of old CSV
        saveGUIEntries();
        // save current row to SharedPreferences
        saveRowPreferences();

        if (row >= curCSV.size() - 1) {
            // EOF reached, do not increase row count
            Toast.makeText(this, "Last Asset Reached " + ("\ud83d\ude04"), Toast.LENGTH_LONG).show();
        } else {
            // EOF not yet reached, increase row count, update GUI for next Asset
            row++;
            // Load GUI items based on row
            pushGUIEntries();
        }
    }

    private void prevAssetView() {
        // save current GUI items to curCSV
        pullGUIEntries();
        // save curCSV over top of old CSV
        saveGUIEntries();
        // save current row to SharedPreferences
        saveRowPreferences();

        if (row <= 1) {
            // EOF reached, do not increase row count
            Toast.makeText(this, "First Asset Reached " + ("\ud83d\ude04"), Toast.LENGTH_LONG).show();
        } else {
            // EOF not yet reached, increase row count, update GUI for next Asset
            row--;
            // Load GUI items based on row
            pushGUIEntries();
        }
    }

    private void pullGUIEntries() {
        String[] curRow = curCSV.get(row); // Get the current content found in current row

        // Pull data from Spinners and load it into StringArray at right location
        curRow[1] = C1LSpin.getSelectedItem().toString();
        curRow[2] = C1RSpin.getSelectedItem().toString();
        curRow[15] = latitude.getText().toString();
        curRow[16] = longitude.getText().toString();

        // Photo names are not saved based on what is displayed

        curCSV.set(row, curRow); // Save modified row back to curCSV
    }

    private void saveGUIEntries() {
        // Has nothing to do with spinner or GUI items
        String saveCSV = formatForCSV(); // The String that will be saved
        OutputStream overWriter = null;
        ContentResolver saveResolver = this.getContentResolver();
        try {
            overWriter = saveResolver.openOutputStream(csvURI);
            if (overWriter != null) {
                overWriter.write(saveCSV.getBytes());
                overWriter.close();
                Toast.makeText(this, "Save Successful", Toast.LENGTH_LONG).show();

            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save Failure", Toast.LENGTH_LONG).show();

        }
    }

    private void pushGUIEntries() {
        AssetName.setSelection(Arrays.asList(AssetNameOptions).indexOf(curCSV.get(row)[0]));
        longitude.setText(curCSV.get(row)[16]);
        latitude.setText(curCSV.get(row)[15]);
        C1LSpin.setSelection(Arrays.asList(C1LOptions).indexOf(curCSV.get(row)[1]));
        C1RSpin.setSelection(Arrays.asList(C1ROptions).indexOf(curCSV.get(row)[2]));

        Uri lPhotoURI = Uri.withAppendedPath(imageURI, curCSV.get(row)[17]); //TODO Name has to be based off of asset name for search
        Uri rPhotoURI = Uri.withAppendedPath(imageURI, curCSV.get(row)[18]);

        if (imageFound(lPhotoURI)) {
            photoL.setBackground(Drawable.createFromPath(lPhotoURI.getPath())); // Display the found image
            photoL.setText(curCSV.get(row)[17]);
        } else {
            photoL.setBackground(getDrawable(R.drawable.harold));
            photoL.setText(R.string.noimagefound); // Display stock photo
        }
        if (imageFound(rPhotoURI)) {
            photoR.setBackground(Drawable.createFromPath(rPhotoURI.getPath()));
            photoR.setText(curCSV.get(row)[18]);
        } else {
            photoR.setBackground(getDrawable(R.drawable.harold));
            photoR.setText(R.string.noimagefound);
        }

    }

    private void setupGUIEntries() {
        // Link variable to XML ID
        // MastHead
        mastHead = (ImageView) findViewById(R.id.CompanyMastHead);
        // Open in Maps Button
        mapsButton = (ImageButton) findViewById(R.id.mapsButton);
        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMaps();
            }
        });
        // Next and Prev Buttons
        nextSave = (Button) findViewById(R.id.nextAsset);
        prevSave = (Button) findViewById(R.id.prevAsset);

        // Get current GPS coordinates
        getGPS = (Button) findViewById(R.id.getGPS);
        progressGPS = (ProgressBar) findViewById(R.id.gpsProgress);
        // Phot Buttons
        photoL = (Button) findViewById(R.id.photoL);
        photoR = (Button) findViewById(R.id.photoR);

        if (imageURI != null) {
            setPhotoSize();
        } else {   // If no folder could be created than do not show the camera buttons
            photoL.setVisibility(View.GONE);
            photoR.setVisibility(View.GONE);
        }

        photoL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                side = true;
                activatePhoto(17);// Take image for left side
            }
        });

        photoR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                side = false;
                activatePhoto(18);// Take image for right side
            }
        });

        // Next and Prev Button listeners
        nextSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextAssetView();
            }
        });
        prevSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                prevAssetView();
            }
        });

        // Current GPS listener
        getGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressGPS.setVisibility(View.VISIBLE);
                getGPS.setEnabled(false);
                gpsThread gps = new gpsThread();
                gps.start();
            }
        });

        // Entry Headers
        // Location Services
        longitude = (TextView) findViewById(R.id.displayLongitude);
        latitude = (TextView) findViewById(R.id.displayLatitude);
        // Characteristics
        C1Ltext = (TextView) findViewById(R.id.C1Ltext);
        C1Rtext = (TextView) findViewById(R.id.C1Rtext);

        // Entry Spinners
        //Header Spinner
        AssetName = (Spinner) findViewById(R.id.AssetName);
        //Option Spinners
        C1LSpin = (Spinner) findViewById(R.id.C1Lspin);
        C1RSpin = (Spinner) findViewById(R.id.C1Rspin);

        // Spinner Listeners
        AssetName.setOnItemSelectedListener(this);
        C1LSpin.setOnItemSelectedListener(this);
        C1RSpin.setOnItemSelectedListener(this);

        // Spinner Options
        AssetNameOptions = getSpinnerOptions(curCSV, 0);
        C1LOptions = getSpinnerOptions(curCSV, 1);
        C1ROptions = getSpinnerOptions(curCSV, 2);

        // Spinner adapters
        ArrayAdapter<String> assetNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, AssetNameOptions);
        ArrayAdapter<String> C1LAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C1LOptions);
        ArrayAdapter<String> C1RAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C1ROptions);

        // Dropdown Spinner Styles
        assetNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C1LAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C1RAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach adapter to spinners
        AssetName.setAdapter(assetNameAdapter);
        C1LSpin.setAdapter(C1LAdapter);
        C1RSpin.setAdapter(C1RAdapter);

        // Set static Entry Headers (non-location)
        C1Ltext.setText(curCSV.get(0)[1]);
        C1Rtext.setText(curCSV.get(0)[2]);

        AssetName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // save current GUI items to curCSV
                pullGUIEntries();
                // save curCSV over top of old CSV
                saveGUIEntries();
                // save current row to SharedPreferences
                saveRowPreferences();
                // get chosen item from list and find its row number in curCSV
                row = getRowIndex(AssetName.getSelectedItem().toString());
                // update rest of GUI entries
                pushGUIEntries();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @SuppressLint("MissingPermission") // This can be suppressed because the title screen checks for permissions and restricts access until granted.
    private Location getLocation() {
        long start = SystemClock.elapsedRealtime();
        long current = SystemClock.elapsedRealtime();
        double accBest = 1000; // Could return null if accuracy is outside 1 km^2
        Location currentLoc;
        Location bestLoc = null;
        while((current-start) < checkGPStime && (accBest > accThres)){
            current = SystemClock.elapsedRealtime();
            currentLoc = managerGPS.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(currentLoc != null) { // Avoids risk of null return if GPS sig is lost
                if ((currentLoc.getElapsedRealtimeNanos()/1000000) > start) {
                    if (currentLoc.getAccuracy() < accBest) {
                        bestLoc = currentLoc;
                        accBest = bestLoc.getAccuracy();
                    }
                }
            }
        }
        return bestLoc;
    }

    private void activatePhoto(int photoColumn) {
        // Left = True, Right = False
        Uri imgExists = Uri.withAppendedPath(imageURI,curCSV.get(row)[photoColumn]);
        String photoName = null;
        if(side){
            photoName = curCSV.get(row)[0] + "_L.jpg";
        }
        else{
            photoName = curCSV.get(row)[0] + "_R.jpg";
        }
        imgPath = Uri.withAppendedPath(imageURI,photoName); // Set save location for takePhoto()
        //check if photo exists, use pop-up dialog, taking new photo may follow
        if(imageFound(imgExists)){
            Bundle passName = new Bundle();
            passName.putString("name", photoName);
            ImageOverwritePopDialog imgOverwrite = new ImageOverwritePopDialog();
            imgOverwrite.setArguments(passName);
            imgOverwrite.show(getSupportFragmentManager(),"Overwrite Image Pop-Up");
        }
        else{
            // File has to be created
            takePhoto();
        }
    }

    private void takePhoto() {
        Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePhoto.resolveActivity(getPackageManager()) != null) {
            File saveImg = new File(imgPath.getPath());
            String authority = getApplicationContext().getPackageName() + ".fileprovider";
            Uri contentURI = FileProvider.getUriForFile(this, authority, saveImg);
            takePhoto.putExtra(MediaStore.EXTRA_OUTPUT,contentURI);
            startActivityForResult(takePhoto, REQUEST_IMAGE_CAPTURE);

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            String[] curRow = curCSV.get(row);
            if(side){
                curRow[17] = imgPath.getLastPathSegment();
            }
            else{
                curRow[18] = imgPath.getLastPathSegment();
            }
            curCSV.set(row, curRow);

            Intent galleryScan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            galleryScan.setData(imgPath);
            this.sendBroadcast(galleryScan);
        }
    }

    private void openMaps() {
        // TODO test if non-numerics cause a crash
        float mapsLat;
        float mapsLon;
        String mapURI;
        try{
            // Get latitude and longitude values
            mapsLat = Float.parseFloat(latitude.getText().toString());
            mapsLon = Float.parseFloat(longitude.getText().toString());
            // Check if they are valid lats and longs
            if((mapsLat >= -90 && mapsLat <= 90)||(mapsLon >= -180 && mapsLon <= 180)){
                //send to maps
                mapURI = "geo:0,0?q="+latitude.getText().toString()+","+longitude.getText().toString()+"("+AssetName.getSelectedItem().toString()+")";
                Uri openMaps = Uri.parse(mapURI);
                Intent openMapsIntent = new Intent(Intent.ACTION_VIEW, openMaps);
                openMapsIntent.setPackage("com.google.android.apps.maps");
                startActivity(openMapsIntent);
            }
            else{
                throw new NumberFormatException("Map Error");
            }
        }

        catch (NumberFormatException e){
            Toast.makeText(this, "Not valid lat/lon combination.",Toast.LENGTH_LONG).show();
        }
        // Open maps, setting a pin at that location
    }

    private int getRowIndex(String stringValue) {
        String[] indexRow = null;
        for(String[] thisRow: curCSV){
            indexRow = thisRow;
            if(Arrays.asList(indexRow).indexOf(stringValue) != -1){
                break;
            }
        }

        return curCSV.indexOf(indexRow);
    }

    //TODO clean all toast messages

    /////////////////////////////////////////////////////////////////////
    // Support functions: No direct association to current state of GUI//
    /////////////////////////////////////////////////////////////////////

    private List<String[]> loadCSVfromURI(Uri file) throws FileNotFoundException, IOException {

        Scanner csvFileScanner = new Scanner(new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(file))));

        List<String[]> csvROW = new ArrayList<>();
        int tempRow = 0;
        while(csvFileScanner.hasNextLine()){
            String line = csvFileScanner.nextLine(); // Get single line from reader
            String[] splitLine = line.split(",");
            csvROW.add(tempRow, splitLine);
            tempRow++;
        }
        csvFileScanner.close();
        return csvROW;
    }

    private boolean imageFound(Uri photoURI){
        File checkFile = new File(photoURI.getPath());
        if(checkFile.exists()){
            return true;
        }
        else return false;
    }

    private String formatForCSV() {
        final String delimeter = "\n";
        StringBuilder formatCSVdata = new StringBuilder();
        String saveCSVrow;
        for(String[] thisRow: curCSV){
            saveCSVrow = String.join(",",thisRow);
            saveCSVrow = saveCSVrow + delimeter;
            formatCSVdata.append(saveCSVrow);
        }

        String saveCSV = formatCSVdata.toString();

        return saveCSV;
    }

    private String[] getSpinnerOptions(List<String[]> master, int column){
        String[] allOptions = new String[master.size()-1]; // -1 to not include first descriptive row
        //TODO add option for when an empty csv (without previous options) is loaded.
        for(int i = 1; i < master.size(); i++){
            allOptions[i-1]= master.get(i)[column];
        }

        List<String> spinnerOptions = Arrays.asList(allOptions).stream().distinct().collect(Collectors.<String>toList());

        allOptions = new String[spinnerOptions.size()];
        allOptions = spinnerOptions.toArray(allOptions);

        return allOptions;
    }

    private void saveRowPreferences(){
        // Has nothing to do with spinner or GUI items
        //TODO This has to be called on app close
        SharedPreferences rowPreference = getSharedPreferences(ROW_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor rowEditor = rowPreference.edit();
        String saveName = fileName;
        rowEditor.putInt(saveName,row);
        rowEditor.apply();
    }

    private void setPhotoSize() {
        int lH = photoL.getWidth();
        int rH = photoL.getWidth();
        photoL.setHeight(lH);
        photoR.setHeight(rH);
    }

    @Override
    public void onNewPhotoClicked() {
        takePhoto();
    }

    LocationListener listenerGPS = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    class gpsThread extends Thread{
        public void run() {
            super.run();
            Location location = getLocation();// Use location in alertDialog
            String locMessage;
            // make message for alert dialog
            if(location != null){
                locMessage = "GPS location found:\n" +
                        "Latitude: " + location.getLatitude() + "\n" +
                        "Longitude: " + location.getLongitude() + "\n" +
                        "Accuracy: " + location.getAccuracy();
            }
            else{
                locMessage = "GPS could not acquire accurate results.";
            }
            Bundle passMessage = new Bundle();
            passMessage.putString("message", locMessage);
            LocationGPSResultDialog alertGPS = new LocationGPSResultDialog();
            alertGPS.setArguments(passMessage);
            alertGPS.show(getSupportFragmentManager(),"GPS result alert dialog");
            // make alert dialog
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressGPS.setVisibility(View.GONE);
                    getGPS.setEnabled(true);
                }
            });
        }
    }

    @Override
    public void onNewGPSResult() {

    }
}
