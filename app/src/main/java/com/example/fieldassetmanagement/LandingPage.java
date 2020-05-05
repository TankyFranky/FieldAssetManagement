package com.example.fieldassetmanagement;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

//TODO build in a button to export the csv to email/different file transfer places

public class LandingPage extends AppCompatActivity {
    public static final String EXTRA_SAP_URI = "com.example.fieldassetmanagement.EXTRA_SAP_URI";
    public static final String EXTRA_SAP_ROW = "com.example.fieldassetmanagement.EXTRA_SAP_ROW";
    public static final String EXTRA_SAP_FILENAME = "com.example.fieldassetmanagement.EXTRA_SAP_FILENAME";

    public Uri getCsvURI() {
        return csvURI;
    }

    private Uri csvURI;
    private int row = 0;

    private  static final int REQUEST = 69;
    ProgressBar csvProg;
    Button fileSelect, openCSV, exportCSV, startNew;
    TextView csvFileName, progText;

    //TODO check if file selected is blank
    @Override
    protected void onRestart(){
        super.onRestart();
        try {
            activateProgress();
        } catch (IOException e) {
            ;
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO add display of last asset edited Asset based on entered file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing_page);

        openCSV = (Button) findViewById(R.id.openCSV);
        fileSelect = (Button) findViewById(R.id.fileSelect);
        exportCSV = (Button) findViewById(R.id.export);
        startNew = (Button) findViewById(R.id.fileCreate);

        csvProg = (ProgressBar) findViewById(R.id.csvProgress);

        csvFileName = (TextView) findViewById(R.id.csvFileDisplay);
        progText = (TextView) findViewById(R.id.numProgress);

        openCSV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSingleAssetPage();
            }
        });

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


    private void activateProgress() throws IOException {
        //Get total amount of entries
        int totalProg = getRow(csvURI)-1;
        //Get the last entry
        int curProg = loadRowPreference(getCsvURI());

        String progString = "Last Edited " + Integer.toString(curProg) + "/" + Integer.toString(totalProg);

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
            Toast.makeText(this, "No File Selected" , Toast.LENGTH_LONG).show();
        }
    }

    private void openSingleAssetPage() {
        if(csvURI != null) {
            Uri singleAssetURI = csvURI;
            String stringURI = singleAssetURI.toString();
            row = loadRowPreference(singleAssetURI);
            Intent singleAssetIntent = new Intent(this, SingleAssetPage.class);
            singleAssetIntent.putExtra(EXTRA_SAP_URI, stringURI);
            singleAssetIntent.putExtra(EXTRA_SAP_ROW, row);
            singleAssetIntent.putExtra(EXTRA_SAP_FILENAME, getFileName(singleAssetURI));
            startActivity(singleAssetIntent);
        }

        else{
            Toast.makeText(this, "No File Selected" , Toast.LENGTH_LONG).show();
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

                Toast.makeText(this, "Path: " + csvURI.getPath(), Toast.LENGTH_LONG).show();
                csvFileName.setText(getFileName(csvURI));

                try {
                    activateProgress();
                } catch (IOException e) {
                    ;
                }

            }
        }
    }

    public String getFileName(Uri uri) {
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

    private int getRow(Uri file) throws FileNotFoundException, IOException {
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
        return savedRow;
    }

}