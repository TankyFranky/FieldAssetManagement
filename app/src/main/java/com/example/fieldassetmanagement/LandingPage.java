/* Copyright (C) Francesco Software - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * You may note use this material for commercial or monetary purposes
 * Proprietary and confidential
 * Written by Francesco R.A. Marrato <unkindthrower@gmail.com>, June 2020
 */
//TODO app takes a while to load, implement multithreading (Stretch)
package com.example.fieldassetmanagement;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class LandingPage extends AppCompatActivity {
    public static final String EXTRA_SAP_ASSET_URI = "com.example.fieldassetmanagement.EXTRA_SAP_ASSET_URI";
    public static final String EXTRA_SAP_IMAGE_URI = "com.example.fieldassetmanagement.EXTRA_SAP_IMAGE_URI";
    public static final String EXTRA_SAP_ROW = "com.example.fieldassetmanagement.EXTRA_SAP_ROW";
    public static final String EXTRA_SAP_FILENAME = "com.example.fieldassetmanagement.EXTRA_SAP_FILENAME";

    public Uri getCsvURI() {
        return csvURI;
    }

    private Uri csvURI, imageURI;
    private int row = 0;
    private int STORAGE_REQUEST = 31;

    private  static final int REQUEST = 69;
    ProgressBar csvProg, openProgress;
    Button fileSelect, openCSV, exportCSV, startNew;
    TextView csvFileName, progText;

    @Override
    protected void onResume() {
        super.onResume();
        openProgress.setVisibility(View.GONE);
        openCSV.setEnabled(true);
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        if(csvURI != null) {
            try {
                activateProgress();
            } catch (IOException e) {
                progText.setVisibility(View.INVISIBLE);
                csvProg.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        openCSV = findViewById(R.id.openCSV);
        fileSelect = findViewById(R.id.fileSelect);
        exportCSV = findViewById(R.id.export);
        startNew = findViewById(R.id.fileCreate);

        csvProg = findViewById(R.id.csvProgress);
        openProgress = findViewById(R.id.openProgress);

        csvFileName = findViewById(R.id.csvFileDisplay);
        progText = findViewById(R.id.numProgress);

        openCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSingleAssetPage();
            }
        });

        checkPermissions();

        fileSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                activateSearch();
            }
        });

        exportCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportSelected();
            }
        });
    }

    private void checkPermissions() {
        // TODO add permissions dialog (Stretch)
        if (
                ContextCompat.checkSelfPermission(LandingPage.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(LandingPage.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(LandingPage.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            //If permissions already are allowed
            fileSelect.setEnabled(true);
            startNew.setEnabled(true);
        }
        else{
            //If permissions are not allowed
            fileSelect.setEnabled(false);
            startNew.setEnabled(false);
        }

    }

    private void activateProgress() throws IOException {
        //Get total amount of entries
        int totalProg = getRow(csvURI)-1; // subtract one to avoid including the headers as progress
        //Get the last entry
        int curProg = loadRowPreference(getCsvURI());

        String progString = "Last Edited " + curProg + "/" + totalProg;

        //Set the progress bar
        progText.setVisibility(View.VISIBLE);
        progText.setText(progString);
        csvProg.setVisibility(View.VISIBLE);
        csvProg.setMax(totalProg);
        csvProg.setProgress(curProg,true);
    }

    private void exportSelected() {
        if(csvURI != null) {
            Intent exportIntent = new Intent(Intent.ACTION_SEND);
            exportIntent.setType("text/csv");
            exportIntent.putExtra(Intent.EXTRA_SUBJECT, "_exported");
            exportIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            exportIntent.putExtra(Intent.EXTRA_STREAM, getCsvURI());
            startActivity(Intent.createChooser(exportIntent,"Export from Field Asset Management."));

        }

        else{
            Toast.makeText(this, "No File Selected" , Toast.LENGTH_SHORT).show();
        }
    }

    private void openSingleAssetPage() {
        if(csvURI != null) {
            openProgress.setVisibility(View.VISIBLE);
            openCSV.setEnabled(false);
            Uri singleAssetURI = csvURI;
            Uri assetImageURI = imageURI;
            String stringImageURI = null;
            String stringAssetURI = singleAssetURI.toString();
            if (assetImageURI != null) {
                stringImageURI = assetImageURI.toString();
            }
            row = loadRowPreference(singleAssetURI);
            Intent singleAssetIntent = new Intent(this, SingleAssetPage.class);
            singleAssetIntent.putExtra(EXTRA_SAP_ASSET_URI, stringAssetURI);
            singleAssetIntent.putExtra(EXTRA_SAP_IMAGE_URI, stringImageURI);
            singleAssetIntent.putExtra(EXTRA_SAP_ROW, row);
            singleAssetIntent.putExtra(EXTRA_SAP_FILENAME, getFileName(singleAssetURI));
            startActivity(singleAssetIntent);
        }

        else{
            Toast.makeText(this, "No File Selected" , Toast.LENGTH_SHORT).show();
        }
    }

    private void activateSearch() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, REQUEST);
    }

    @Override
    protected  void onActivityResult(int request, int result, Intent data){
        super.onActivityResult(request, result, data);

        if(request == REQUEST && result == Activity.RESULT_OK){
            if(data != null){
                csvURI = data.getData();

                Toast.makeText(this, "Path: " + csvURI.getPath(), Toast.LENGTH_SHORT).show();
                csvFileName.setText(getFileName(csvURI));

                try {
                    activateProgress();
                } catch (IOException e) {
                    progText.setVisibility(View.INVISIBLE);
                    csvProg.setVisibility(View.INVISIBLE);
                }

                createImageFolder();

            }
        }
    }

    private void createImageFolder() {
        String folderName = removeExtension(getFileName(csvURI)) + "_img"; //create folder name
        String state = Environment.getExternalStorageState(); //Check if storage is mounted
        if(Environment.MEDIA_MOUNTED.equals(state)){
            File assetPhotos = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName); //create public directory
            if(!assetPhotos.exists()) { //if it does not already exist then create it
                if (!assetPhotos.mkdir()) {
                    Toast.makeText(this, "Could not create folder " + assetPhotos.getPath(), Toast.LENGTH_LONG).show();
                    assetPhotos = null;
                }

            }
            imageURI = Uri.fromFile(assetPhotos);
        }
        else {
            Toast.makeText(this, "No mounted Storage.", Toast.LENGTH_LONG).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }


    private int getRow(Uri file) throws IOException {
        Scanner csvFileScanner = new Scanner(new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(file))));
        int tempRow = 0;
        while(csvFileScanner.hasNextLine()){
            csvFileScanner.nextLine();
            tempRow++;
        }
        csvFileScanner.close();
        return tempRow;
    }


    public int loadRowPreference(Uri fileURI){
        SharedPreferences rowPreference = getSharedPreferences(SingleAssetPage.ROW_PREFERENCES, MODE_PRIVATE);
        int savedRow = rowPreference.getInt(getFileName(fileURI),1);

        try {
            if (getRow(fileURI) - 1 < savedRow) {
                savedRow = 1;
            }
        } catch (IOException e) {
            savedRow = 1;
        }

        return savedRow;
    }

}
