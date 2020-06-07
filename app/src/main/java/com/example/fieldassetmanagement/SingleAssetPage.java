/* Copyright (C) Francesco Software - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * You may note use this material for commercial or monetary purposes
 * Proprietary and confidential
 * Written by Francesco R.A. Marrato <unkindthrower@gmail.com>, June 2020
 */
//TODO organize functions
//TODO known bug, if the previously saved row is larger than the current size it will crash (avoid any "," from being saved, will corrupt csv)
//TODO fold and comment code
package com.example.fieldassetmanagement;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

public class SingleAssetPage extends AppCompatActivity implements
        OnItemSelectedListener,
        DatePickerDialog.OnDateSetListener,
        LocationGPSResultDialog.GPSResultListener,
        ImageOverwritePopDialog.imgOverwriteListener,
        NewAssetDialog.NewAssetDialogListener,
        AddCategoryDialog.AddCategoryDialogListener {
    // GPS variables
    private LocationManager managerGPS;
    private final long minTimeUpdates = 50;
    private final float minDistanceUpdates = 0; // setting to zero means it is non-movement based updates
    private final long checkGPStime = 4000;
    private final double accThres = 5.0;
    private Calendar today;

    // SingleAssetPage local variables
    private String fileName;
    private List<String[]> curCSV;
    private Uri csvURI, imageURI, imgPath;
    private int row;
    private static int check = 0;
    private boolean side;

    // Shared Preference Constants
    public static final String ROW_PREFERENCES = "rowPrev";
    private static final int REQUEST_IMAGE_CAPTURE = 5;
    private String ASYNC_PREVIOUS_SPINNER_OPTION;

    // All Spinner declarations
    private Spinner ASYNC_PROCESS_SPINNER, AssetName, C1LSpin, C1RSpin, C3LSpin, C3RSpin, C4LSpin, C4RSpin,C6LSpin, C6RSpin, C7LSpin, C7RSpin, C8LSpin, C8RSpin, C9LSpin, C9RSpin, C10LSpin, C10RSpin, C11LSpin, C11RSpin, C12LSpin, C13LSpin, C13RSpin;

    // All EditText declarations
    private EditText C2LeditText, C2ReditText, C14LeditText, C15LeditText, C15CeditText, C15ReditText;

    // All Spinner option declarations
    private List<String> AssetNameOptions, C1LOptions, C1ROptions, C3LOptions, C3ROptions, C4LOptions, C4ROptions, C6213Options, C13ROptions; // TODO use one option list for all spinners which would contain similar values
    private ArrayAdapter<String> ASYNC_PROCESS_ADAPTER, assetNameAdapter, C1LAdapter, C1RAdapter, C3LAdapter, C3RAdapter, C4LAdapter, C4RAdapter, C6213Adapter, C13RAdapter;

    ImageView mastHead;
    ImageButton mapsButton;
    ProgressBar progressGPS;
    Button nextSave, prevSave, photoL, photoR, getGPS, addAsset;
    TextView ASYNC_PROCESS_TEXTVIEW, longitude, latitude, C1Ltext, C1Rtext, C2Ltext, C2Rtext, C3Ltext, C3Rtext, C4Ltext, C4Rtext, C5Ltext, C5Rtext, C5Ldate, C5Rdate, C6Ltext, C6Rtext, C7Ltext, C7Rtext, C8Ltext, C8Rtext, C9Ltext, C9Rtext, C10Ltext, C10Rtext, C11Ltext, C11Rtext, C12Ltext, C13Ltext, C13Rtext, C14Ltext, C15Ltext, C15Ctext, C15Rtext;

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
        today = Calendar.getInstance();
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
        if(++check > 1) {
            switch (parent.getId()) {
                case R.id.AssetName:
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

                    break;
                case R.id.C1Lspin:
                    if (C1LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C1LAdapter, C1LSpin, curCSV.get(row)[getResources().getInteger(R.integer.hwyRW)]);
                    }
                    break;
                case R.id.C1Rspin:
                    if (C1RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C1RAdapter, C1RSpin, curCSV.get(row)[getResources().getInteger(R.integer.material)]);
                    }
                    break;
                case R.id.C3Lspin:
                    if (C3LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C3LAdapter, C3LSpin, curCSV.get(row)[getResources().getInteger(R.integer.township)]);
                    }
                    break;
                case R.id.C3Rspin:
                    if (C3RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C3RAdapter, C3RSpin, curCSV.get(row)[getResources().getInteger(R.integer.highway)]);
                    }
                    break;
                case R.id.C4Lspin:
                    if (C4LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C4LAdapter, C4LSpin, curCSV.get(row)[getResources().getInteger(R.integer.shape)]);
                    }
                    break;
                case R.id.C4Rspin:
                    if (C4RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C4RAdapter, C4RSpin, curCSV.get(row)[getResources().getInteger(R.integer.surround)]);
                    }
                    break;
                case R.id.C6Lspin:
                    if (C6LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C6LSpin, curCSV.get(row)[getResources().getInteger(R.integer.materialL)]);
                    }
                    break;
                case R.id.C6Rspin:
                    if (C6RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C6RSpin, curCSV.get(row)[getResources().getInteger(R.integer.materialR)]);
                    }
                    break;
                case R.id.C7Lspin:
                    if (C7LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C7LSpin, curCSV.get(row)[getResources().getInteger(R.integer.shapeL)]);
                    }
                    break;
                case R.id.C7Rspin:
                    if (C7RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C7RSpin, curCSV.get(row)[getResources().getInteger(R.integer.shapeR)]);
                    }
                    break;
                case R.id.C8Lspin:
                    if (C8LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C8LSpin, curCSV.get(row)[getResources().getInteger(R.integer.capacityL)]);
                    }
                    break;
                case R.id.C8Rspin:
                    if (C8RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C8RSpin, curCSV.get(row)[getResources().getInteger(R.integer.capacityR)]);
                    }
                    break;
                case R.id.C9Lspin:
                    if (C9LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C9LSpin, curCSV.get(row)[getResources().getInteger(R.integer.invertL)]);
                    }
                    break;
                case R.id.C9Rspin:
                    if (C9RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C9RSpin, curCSV.get(row)[getResources().getInteger(R.integer.invertR)]);
                    }
                    break;
                case R.id.C10Lspin:
                    if (C10LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C10LSpin, curCSV.get(row)[getResources().getInteger(R.integer.scourL)]);
                    }
                    break;
                case R.id.C10Rspin:
                    if (C10RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C10RSpin, curCSV.get(row)[getResources().getInteger(R.integer.scourR)]);
                    }
                    break;
                case R.id.C11Lspin:
                    if (C11LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C11LSpin, curCSV.get(row)[getResources().getInteger(R.integer.slopeL)]);
                    }
                    break;
                case R.id.C11Rspin:
                    if (C11RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C11RSpin, curCSV.get(row)[getResources().getInteger(R.integer.slopeR)]);
                    }
                    break;
                case R.id.C12Lspin:
                    if (C12LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C12LSpin, curCSV.get(row)[getResources().getInteger(R.integer.settlement)]);
                    }
                    break;
                case R.id.C13Lspin:
                    if (C13LSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C6213Adapter, C13LSpin, curCSV.get(row)[getResources().getInteger(R.integer.remainingLife)]);
                    }
                    break;
                case R.id.C13Rspin:
                    if (C13RSpin.getSelectedItem().toString().equalsIgnoreCase(getString(R.string.add_option))) {
                        addOption(C13RAdapter, C13RSpin, curCSV.get(row)[getResources().getInteger(R.integer.priority)]);
                    }
                    break;
            }
        }
    }

    private void addOption(ArrayAdapter<String> spinnerAdapter, Spinner spinner, String lastOption) {
        ASYNC_PROCESS_ADAPTER = spinnerAdapter; // Set reference to which spinner list had to be edited
        ASYNC_PROCESS_SPINNER = spinner; // Set reference to which spinner list should show the new change
        ASYNC_PREVIOUS_SPINNER_OPTION = lastOption; // Set reference to previous value of spinner on the event of cancellation

        AddCategoryDialog addCategory = new AddCategoryDialog();
        addCategory.show(getSupportFragmentManager(), "Adding new category to Spinner");
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
        curRow[getResources().getInteger(R.integer.hwyRW)] = C1LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.material)] = C1RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.lhrs)] = C2LeditText.getText().toString();
        curRow[getResources().getInteger(R.integer.offset)] = C2ReditText.getText().toString();
        curRow[getResources().getInteger(R.integer.township)] = C3LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.highway)] = C3RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.shape)] = C4LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.surround)] = C4RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.dateInspected)] = C5Ldate.getText().toString();
        curRow[getResources().getInteger(R.integer.warranty)] = C5Rdate.getText().toString();
        curRow[getResources().getInteger(R.integer.materialL)] = C6LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.materialR)] = C6RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.shapeL)] = C7LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.shapeR)] = C7RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.capacityL)] = C8LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.capacityR)] = C8RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.invertL)] = C9LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.invertR)] = C9RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.scourL)] = C10LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.scourR)] = C10RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.slopeL)] = C11LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.slopeR)] = C11RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.settlement)] = C12LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.remainingLife)] = C13LSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.priority)] = C13RSpin.getSelectedItem().toString();
        curRow[getResources().getInteger(R.integer.notes)] = C14LeditText.getText().toString().replace(",",".");
        curRow[getResources().getInteger(R.integer.length)] = C15LeditText.getText().toString();
        curRow[getResources().getInteger(R.integer.size)] = C15CeditText.getText().toString();
        curRow[getResources().getInteger(R.integer.depth)] = C15ReditText.getText().toString();


        curRow[getResources().getInteger(R.integer.latitude)] = latitude.getText().toString();
        curRow[getResources().getInteger(R.integer.longitude)] = longitude.getText().toString();


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
        AssetName.setSelection(AssetNameOptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.culvertID)]));
        latitude.setText(curCSV.get(row)[getResources().getInteger(R.integer.latitude)]);
        longitude.setText(curCSV.get(row)[getResources().getInteger(R.integer.longitude)]);
        C1LSpin.setSelection(C1LOptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.hwyRW)].toUpperCase()));
        C1RSpin.setSelection(C1ROptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.material)].toUpperCase()));
        C2LeditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.lhrs)]);
        C2ReditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.offset)]);
        C3LSpin.setSelection(C3LOptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.township)].toUpperCase()));
        C3RSpin.setSelection(C3ROptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.highway)].toUpperCase()));
        C4LSpin.setSelection(C4LOptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.shape)].toUpperCase()));
        C4RSpin.setSelection(C4ROptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.surround)].toUpperCase()));
        C5Ldate.setText(curCSV.get(row)[getResources().getInteger(R.integer.dateInspected)]);
        C5Rdate.setText(curCSV.get(row)[getResources().getInteger(R.integer.warranty)]);
        C6LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.materialL)].toUpperCase()));
        C6RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.materialR)].toUpperCase()));
        C7LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.shapeL)].toUpperCase()));
        C7RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.shapeR)].toUpperCase()));
        C8LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.capacityL)].toUpperCase()));
        C8RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.capacityR)].toUpperCase()));
        C9LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.invertL)].toUpperCase()));
        C9RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.invertR)].toUpperCase()));
        C10LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.scourL)].toUpperCase()));
        C10RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.scourR)].toUpperCase()));
        C11LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.slopeL)].toUpperCase()));
        C11RSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.slopeR)].toUpperCase()));
        C12LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.settlement)].toUpperCase()));
        C13LSpin.setSelection(C6213Options.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.remainingLife)].toUpperCase()));
        C13RSpin.setSelection(C13ROptions.indexOf(curCSV.get(row)[getResources().getInteger(R.integer.priority)].toUpperCase()));
        C14LeditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.notes)]);
        C15LeditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.length)]);
        C15CeditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.size)]);
        C15ReditText.setText(curCSV.get(row)[getResources().getInteger(R.integer.depth)]);

        Uri lPhotoURI = Uri.withAppendedPath(imageURI, curCSV.get(row)[getResources().getInteger(R.integer.photoL)]); //TODO Name has to be based off of asset name for search
        Uri rPhotoURI = Uri.withAppendedPath(imageURI, curCSV.get(row)[getResources().getInteger(R.integer.photoR)]);

        if (imageFound(lPhotoURI)) {
            photoL.setBackground(Drawable.createFromPath(lPhotoURI.getPath())); // Display the found image
            photoL.setText(curCSV.get(row)[getResources().getInteger(R.integer.photoL)]);
        } else {
            photoL.setBackground(getDrawable(R.drawable.harold));
            photoL.setText(R.string.noimagefound); // Display stock photo
        }
        if (imageFound(rPhotoURI)) {
            photoR.setBackground(Drawable.createFromPath(rPhotoURI.getPath()));
            photoR.setText(curCSV.get(row)[getResources().getInteger(R.integer.photoR)]);
        } else {
            photoR.setBackground(getDrawable(R.drawable.harold));
            photoR.setText(R.string.noimagefound);
        }

    }

    private void setupGUIEntries() {
        // Link variable to XML ID
        // MastHead
        mastHead = findViewById(R.id.CompanyMastHead);
        // Open in Maps Button
        mapsButton = findViewById(R.id.mapsButton);
        mapsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMaps();
            }
        });
        // Next and Prev Buttons
        nextSave = findViewById(R.id.nextAsset);
        prevSave = findViewById(R.id.prevAsset);

        // Get current GPS coordinates
        getGPS = findViewById(R.id.getGPS);
        progressGPS = findViewById(R.id.gpsProgress);
        // Add new Asset Button
        addAsset = findViewById(R.id.addNew);
        // Photo Buttons
        photoL = findViewById(R.id.photoL);
        photoR = findViewById(R.id.photoR);

        C5Ldate = findViewById(R.id.C5Lspin);
        C5Rdate = findViewById(R.id.C5Rspin);

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
                activatePhoto(getResources().getInteger(R.integer.photoL));// Take image for left side
            }
        });

        photoR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                side = false;
                activatePhoto(getResources().getInteger(R.integer.photoR));// Take image for right side
            }
        });

        C5Ldate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int Year = today.get(Calendar.YEAR);
                int Month = today.get(Calendar.MONTH);
                int Day = today.get(Calendar.DAY_OF_MONTH);

                openDate(Year, Month, Day, C5Ldate);
            }
        });

        C5Rdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pattern validDate = Pattern.compile("^\\d{4}\\-(0?[1-9]|1[012])\\-(0?[1-9]|[12][0-9]|3[01])$");
                String current = C5Rdate.getText().toString();
                int Year;
                int Month;
                int Day;
                if(validDate.matcher(current).find()){
                    String[] dateSplit = current.split("-",3);
                    Year = Integer.parseInt(dateSplit[0]);
                    Month = Integer.parseInt(dateSplit[1]);
                    Day = Integer.parseInt(dateSplit[2]);
                }

                else{
                    Year = today.get(Calendar.YEAR);
                    Month = today.get(Calendar.MONTH);
                    Day = today.get(Calendar.DAY_OF_MONTH);
                }

                openDate(Year, Month, Day, C5Rdate);
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

        // Add new asset listener
        addAsset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NewAssetDialog addNew = new NewAssetDialog();
                addNew.show(getSupportFragmentManager(), "Add new asset dialog");
            }
        });

        // Entry Headers
        // Location Services
        longitude = findViewById(R.id.displayLongitude);
        latitude = findViewById(R.id.displayLatitude);
        // Characteristics
        C1Ltext = findViewById(R.id.C1Ltext);
        C1Rtext = findViewById(R.id.C1Rtext);
        C2Ltext = findViewById(R.id.C2Ltext);
        C2Rtext = findViewById(R.id.C2Rtext);
        C3Ltext = findViewById(R.id.C3Ltext);
        C3Rtext = findViewById(R.id.C3Rtext);
        C4Ltext = findViewById(R.id.C4Ltext);
        C4Rtext = findViewById(R.id.C4Rtext);
        C5Ltext = findViewById(R.id.C5Ltext);
        C5Rtext = findViewById(R.id.C5Rtext);
        C6Ltext = findViewById(R.id.C6Ltext);
        C6Rtext = findViewById(R.id.C6Rtext);
        C7Ltext = findViewById(R.id.C7Ltext);
        C7Rtext = findViewById(R.id.C7Rtext);
        C8Ltext = findViewById(R.id.C8Ltext);
        C8Rtext = findViewById(R.id.C8Rtext);
        C9Ltext = findViewById(R.id.C9Ltext);
        C9Rtext = findViewById(R.id.C9Rtext);
        C10Ltext = findViewById(R.id.C10Ltext);
        C10Rtext = findViewById(R.id.C10Rtext);
        C11Ltext = findViewById(R.id.C11Ltext);
        C11Rtext = findViewById(R.id.C11Rtext);
        C12Ltext = findViewById(R.id.C12Ltext);
        C13Ltext = findViewById(R.id.C13Ltext);
        C13Rtext = findViewById(R.id.C13Rtext);
        C14Ltext = findViewById(R.id.C14Ltext);
        C15Ltext = findViewById(R.id.C15Ltext);
        C15Ctext = findViewById(R.id.C15Ctext);
        C15Rtext = findViewById(R.id.C15Rtext);

        // Entry Spinners
        //Header Spinner
        AssetName = findViewById(R.id.AssetName);
        //Option Spinners
        C1LSpin = findViewById(R.id.C1Lspin);
        C1RSpin = findViewById(R.id.C1Rspin);
        C2LeditText = findViewById(R.id.C2Lspin);
        C2ReditText = findViewById(R.id.C2Rspin);
        C3LSpin = findViewById(R.id.C3Lspin);
        C3RSpin = findViewById(R.id.C3Rspin);
        C4LSpin = findViewById(R.id.C4Lspin);
        C4RSpin = findViewById(R.id.C4Rspin);
        C6LSpin = findViewById(R.id.C6Lspin);
        C6RSpin = findViewById(R.id.C6Rspin);
        C7LSpin = findViewById(R.id.C7Lspin);
        C7RSpin = findViewById(R.id.C7Rspin);
        C8LSpin = findViewById(R.id.C8Lspin);
        C8RSpin = findViewById(R.id.C8Rspin);
        C9LSpin = findViewById(R.id.C9Lspin);
        C9RSpin = findViewById(R.id.C9Rspin);
        C10LSpin = findViewById(R.id.C10Lspin);
        C10RSpin = findViewById(R.id.C10Rspin);
        C11LSpin = findViewById(R.id.C11Lspin);
        C11RSpin = findViewById(R.id.C11Rspin);
        C12LSpin = findViewById(R.id.C12Lspin);
        C13LSpin = findViewById(R.id.C13Lspin);
        C13RSpin = findViewById(R.id.C13Rspin);
        C14LeditText = findViewById(R.id.C14Lspin);
        C15LeditText = findViewById(R.id.C15Lspin);
        C15CeditText = findViewById(R.id.C15Cspin);
        C15ReditText = findViewById(R.id.C15Rspin);

        // Spinner Options
        List commonList = Arrays.asList(getResources().getInteger(R.integer.remainingLife),getResources().getInteger(R.integer.materialL),getResources().getInteger(R.integer.materialR),getResources().getInteger(R.integer.shapeL),getResources().getInteger(R.integer.shapeR),
                getResources().getInteger(R.integer.capacityL),getResources().getInteger(R.integer.capacityR),getResources().getInteger(R.integer.invertL),getResources().getInteger(R.integer.invertR),getResources().getInteger(R.integer.scourL),
                getResources().getInteger(R.integer.scourR),getResources().getInteger(R.integer.slopeL),getResources().getInteger(R.integer.slopeR),getResources().getInteger(R.integer.settlement));


        AssetNameOptions = getAssetNameOptions(curCSV, getResources().getInteger(R.integer.culvertID));
        C1LOptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.hwyRW), true);
        C1ROptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.material), true);
        C3LOptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.township), true);
        C3ROptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.highway), true);
        C4LOptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.shape), true);
        C4ROptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.surround), true);
        C6213Options = getSpinnerOptionsM(curCSV, commonList, true);
        C13ROptions = getSpinnerOptions(curCSV, getResources().getInteger(R.integer.priority), true);

        // Spinner adapters
        assetNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, AssetNameOptions);
        C1LAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C1LOptions);
        C1RAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C1ROptions);
        C3LAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C3LOptions);
        C3RAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C3ROptions);
        C4LAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C4LOptions);
        C4RAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C4ROptions);
        C6213Adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C6213Options);
        C13RAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, C13ROptions);

        // Dropdown Spinner Styles
        assetNameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C1LAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C1RAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C3LAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C3RAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C4LAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C4RAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C6213Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        C13RAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach adapter to spinners
        AssetName.setAdapter(assetNameAdapter);
        C1LSpin.setAdapter(C1LAdapter);
        C1RSpin.setAdapter(C1RAdapter);
        C3LSpin.setAdapter(C3LAdapter);
        C3RSpin.setAdapter(C3RAdapter);
        C4LSpin.setAdapter(C4LAdapter);
        C4RSpin.setAdapter(C4RAdapter);
        C6LSpin.setAdapter(C6213Adapter);
        C6RSpin.setAdapter(C6213Adapter);
        C7LSpin.setAdapter(C6213Adapter);
        C7RSpin.setAdapter(C6213Adapter);
        C8LSpin.setAdapter(C6213Adapter);
        C8RSpin.setAdapter(C6213Adapter);
        C9LSpin.setAdapter(C6213Adapter);
        C9RSpin.setAdapter(C6213Adapter);
        C10LSpin.setAdapter(C6213Adapter);
        C10RSpin.setAdapter(C6213Adapter);
        C11LSpin.setAdapter(C6213Adapter);
        C11RSpin.setAdapter(C6213Adapter);
        C12LSpin.setAdapter(C6213Adapter);
        C13LSpin.setAdapter(C6213Adapter);
        C13RSpin.setAdapter(C13RAdapter);
        // Spinner Listeners
        AssetName.setOnItemSelectedListener(this);
        C1LSpin.setOnItemSelectedListener(this);
        C1RSpin.setOnItemSelectedListener(this);
        C3LSpin.setOnItemSelectedListener(this);
        C3RSpin.setOnItemSelectedListener(this);
        C4LSpin.setOnItemSelectedListener(this);
        C4RSpin.setOnItemSelectedListener(this);
        C6LSpin.setOnItemSelectedListener(this);
        C6RSpin.setOnItemSelectedListener(this);
        C7LSpin.setOnItemSelectedListener(this);
        C7RSpin.setOnItemSelectedListener(this);
        C8LSpin.setOnItemSelectedListener(this);
        C8RSpin.setOnItemSelectedListener(this);
        C9LSpin.setOnItemSelectedListener(this);
        C9RSpin.setOnItemSelectedListener(this);
        C10LSpin.setOnItemSelectedListener(this);
        C10RSpin.setOnItemSelectedListener(this);
        C11LSpin.setOnItemSelectedListener(this);
        C11RSpin.setOnItemSelectedListener(this);
        C12LSpin.setOnItemSelectedListener(this);
        C13LSpin.setOnItemSelectedListener(this);
        C13RSpin.setOnItemSelectedListener(this);
        // Set static Entry Headers (non-location)
        C1Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.hwyRW)]);
        C1Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.material)]);
        C2Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.lhrs)]);
        C2Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.offset)]);
        C3Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.township)]);
        C3Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.highway)]);
        C4Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.shape)]);
        C4Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.surround)]);
        C5Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.dateInspected)]);
        C5Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.warranty)]);
        C6Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.materialL)]);
        C6Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.materialR)]);
        C7Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.shapeL)]);
        C7Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.shapeR)]);
        C8Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.capacityL)]);
        C8Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.capacityR)]);
        C9Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.invertL)]);
        C9Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.invertR)]);
        C10Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.scourL)]);
        C10Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.scourR)]);
        C11Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.slopeL)]);
        C11Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.slopeR)]);
        C12Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.settlement)]);
        C13Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.remainingLife)]);
        C13Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.priority)]);
        C14Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.notes)]);
        C15Ltext.setText(curCSV.get(0)[getResources().getInteger(R.integer.length)]);
        C15Ctext.setText(curCSV.get(0)[getResources().getInteger(R.integer.size)]);
        C15Rtext.setText(curCSV.get(0)[getResources().getInteger(R.integer.depth)]);
    }
    private void openDate(int year, int month, int day, TextView entry) {
        ASYNC_PROCESS_TEXTVIEW = entry;
        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                this,
                year, month, day);
        dateDialog.show();
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        String dateString = year + "-" + month + "-" + dayOfMonth;
        ASYNC_PROCESS_TEXTVIEW.setText(dateString);
        ASYNC_PROCESS_TEXTVIEW = null;
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
            photoName = curCSV.get(row)[getResources().getInteger(R.integer.culvertID)] + "_L.jpg";
        }
        else{
            photoName = curCSV.get(row)[getResources().getInteger(R.integer.culvertID)] + "_R.jpg";
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
                curRow[getResources().getInteger(R.integer.photoL)] = imgPath.getLastPathSegment();
            }
            else{
                curRow[getResources().getInteger(R.integer.photoR)] = imgPath.getLastPathSegment();
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

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    private List<String[]> loadCSVfromURI(Uri file) throws IOException {

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
        return checkFile.exists();
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

    private List<String> getSpinnerOptionsM(List<String[]> master, List<Integer> columns, boolean extendable){
        TreeSet<String> allOptions = new TreeSet<>(); // -1 to not include first descriptive row
        for( Integer curColumn: columns) {
            for (int i = 1; i < master.size(); i++) {
                allOptions.add(master.get(i)[curColumn].toUpperCase());
            }
        }
        List<String> toList = new ArrayList<String> (allOptions);

        if(extendable){
            toList.remove(getString(R.string.notAvailable));
            toList.remove(getString(R.string.add_option));
            toList.add(getString(R.string.notAvailable));
            toList.add(getString(R.string.add_option));
        }

        return toList;
    }

    private List<String> getSpinnerOptions(List<String[]> master, int column, boolean extendable){
        TreeSet<String> allOptions = new TreeSet<>(); // -1 to not include first descriptive row
        for(int i = 1; i < master.size(); i++){
            allOptions.add(master.get(i)[column].toUpperCase());
        }

        List<String> toList = new ArrayList<String> (allOptions);

        if(extendable && !toList.contains(getString(R.string.add_option))){
            if(!toList.contains(getString(R.string.notAvailable))){
                toList.add(getString(R.string.notAvailable));
            }
            toList.add(getString(R.string.add_option));
        }

        return toList;
    }

    private List<String> getAssetNameOptions(List<String[]> master, int column){
        String[] allOptions = new String[master.size()-1]; // -1 to not include first descriptive row
        for(int i = 1; i < master.size(); i++){
            allOptions[i-1]= master.get(i)[column];
        }

        List<String> spinnerOptions = Arrays.asList(allOptions).stream().distinct().collect(Collectors.<String>toList());

        allOptions = new String[spinnerOptions.size()];
        allOptions = spinnerOptions.toArray(allOptions);

        List<String> toList = new ArrayList<String>(Arrays.asList(allOptions));

        return toList;
    }

    private void saveRowPreferences(){
        // Has nothing to do with spinner or GUI items
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
    public void newAssetName(String newAssetName) {
        createNewAsset(newAssetName);
    }

    private void createNewAsset(String newAssetName) {
        Pattern validName = Pattern.compile("[$&+,:;=\\\\?@#|/'<>.^*()%!-]");

        if(assetNameAdapter.getPosition(newAssetName.trim()) == -1) {
            if (!validName.matcher(newAssetName).find()) {
                String[] newAssetRow = new String[curCSV.get(row).length];
                Arrays.fill(newAssetRow, "N/A");
                newAssetRow[getResources().getInteger(R.integer.culvertID)] = newAssetName;
                curCSV.add(newAssetRow);

                pullGUIEntries();
                saveGUIEntries();
                saveRowPreferences();
                row = getRowIndex(newAssetName);
                assetNameAdapter.add(newAssetName);
                assetNameAdapter.notifyDataSetChanged();
                pushGUIEntries();
            } else {
                Toast.makeText(this, "Invalid Name: No special characters allowed. " + ("\u274c"), Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(this, "Invalid Name: Duplicate. " + ("\u274c"), Toast.LENGTH_LONG).show();
        }
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

    @Override
    public void addCategory(String category) {
        //check for proper category name
        String addCategory = category.toUpperCase();

        Pattern validName = Pattern.compile("[$&+,:;=\\\\?@#|'<>.^*()%!-]");

        if(ASYNC_PROCESS_ADAPTER !=null && ASYNC_PROCESS_SPINNER !=null) {
            if (!validName.matcher(addCategory).find() && !addCategory.isEmpty() && !addCategory.trim().isEmpty() && ASYNC_PROCESS_ADAPTER.getPosition(addCategory.trim()) == -1) {
                // Alphabetise
                ASYNC_PROCESS_ADAPTER.remove(getString(R.string.notAvailable));
                ASYNC_PROCESS_ADAPTER.remove(getString(R.string.add_option));
                ASYNC_PROCESS_ADAPTER.add(addCategory);
                ASYNC_PROCESS_ADAPTER.sort(String.CASE_INSENSITIVE_ORDER);
                ASYNC_PROCESS_ADAPTER.add(getString(R.string.notAvailable));
                ASYNC_PROCESS_ADAPTER.add(getString(R.string.add_option));
                ASYNC_PROCESS_ADAPTER.notifyDataSetChanged();
                ASYNC_PROCESS_SPINNER.setSelection(ASYNC_PROCESS_ADAPTER.getPosition(addCategory));
                ASYNC_PREVIOUS_SPINNER_OPTION = null;
                pullGUIEntries();
            }

            else{
                Toast.makeText(this, "No special characters or duplicates allowed. " + ("\u274c"), Toast.LENGTH_LONG).show();
                ASYNC_PROCESS_SPINNER.setSelection(ASYNC_PROCESS_ADAPTER.getPosition(ASYNC_PREVIOUS_SPINNER_OPTION));
            }
        }

        //set ASYNC_PROCESS_ADAPTER to null
        ASYNC_PROCESS_ADAPTER = null;
        ASYNC_PROCESS_SPINNER = null;
    }

    @Override
    public void cancelCategoryDialog() {
        if(ASYNC_PROCESS_SPINNER!=null && ASYNC_PROCESS_ADAPTER !=null && ASYNC_PREVIOUS_SPINNER_OPTION != null) {
            ASYNC_PROCESS_SPINNER.setSelection(ASYNC_PROCESS_ADAPTER.getPosition(ASYNC_PREVIOUS_SPINNER_OPTION));
        }
        ASYNC_PROCESS_ADAPTER = null;
        ASYNC_PROCESS_SPINNER = null;
        ASYNC_PREVIOUS_SPINNER_OPTION = null;
    }

    class gpsThread extends Thread{
        public void run() {
            super.run();
            Location location = getLocation();// Use location in alertDialog
            String locMessage;
            Bundle passMessage = new Bundle();
            // make message for alert dialog
            if(location != null){
                locMessage = "GPS location found:\n" +
                        "Latitude: " + location.getLatitude() + "\n" +
                        "Longitude: " + location.getLongitude() + "\n" +
                        "Accuracy: " + location.getAccuracy();
                passMessage.putDouble("latitude", location.getLatitude());
                passMessage.putDouble("longitude", location.getLongitude());
            }
            else{
                locMessage = null;
            }
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
    public void onNewGPSResult(String latitudeAD, String longitudeAD) {
        latitude.setText(latitudeAD);
        longitude.setText(longitudeAD);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }
}
