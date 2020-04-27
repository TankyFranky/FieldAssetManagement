package com.example.fieldassetmanagement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Path;
import android.widget.AdapterView.OnItemSelectedListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SingleAssetPage extends AppCompatActivity implements OnItemSelectedListener{

    private String fileName;
    private List<String[]> curCSV;
    private Uri csvURI;
    private int row;

    // Shared Preference Constants
    public static final String ROW_PREFERENCES = "rowPres";

    // All Spinner declarations
    private Spinner fruitSpinner;

    // All Spinner option declarations
    private String[] fruitOptions;

    Button nextSave, prevSave;
    TextView longitude, latitude, fruit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_asset_page);

        Intent openIntent = getIntent();
        String EXTRA_SAP_URI = openIntent.getStringExtra(MainActivity.EXTRA_SAP_URI);
        int EXTRA_SAP_ROW = openIntent.getIntExtra(MainActivity.EXTRA_SAP_ROW, 1);
        String EXTRA_SAP_FILENAME = openIntent.getStringExtra(MainActivity.EXTRA_SAP_FILENAME);
        csvURI=Uri.parse(EXTRA_SAP_URI);    // Convert EXTRA_SAP_URI data back to a URI
        fileName = EXTRA_SAP_FILENAME; // Get Short-form fileName from MainActivity
        row = EXTRA_SAP_ROW; // Get loaded Shared Preference Row from MainActivity

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
        // Next and Prev Buttons
        nextSave = (Button) findViewById(R.id.nextAsset);
        prevSave = (Button) findViewById(R.id.prevAsset);

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

        // Entry Headers
        longitude = (TextView) findViewById(R.id.displayLongitude);
        latitude = (TextView) findViewById(R.id.displayLatitude);
        fruit = (TextView) findViewById(R.id.fruitHeader);

        // Entry Spinners
        fruitSpinner = (Spinner) findViewById(R.id.fruitEntry);

        fruitSpinner.setOnItemSelectedListener(this);

        row = 5;

        // Spinner Options
        fruitOptions = getSpinnerOptions(curCSV, 0);

        // Spinner adapter
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fruitOptions);

        // Dropdown Spinner Style
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Attach adapter to spinner
        fruitSpinner.setAdapter(sizeAdapter);
        
        pushGUIEntries();
        // Set all TextViews
        fruit.setText(curCSV.get(row)[0]);
        longitude.setText(csvURI.toString());
        latitude.setText(fileName);

    }

    // Spinner Listeners
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item = parent.getItemAtPosition(position).toString();
        // Save the new data to curCSV so that onSave it is changed
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    //Next Asset View: Save current entries then load next if not at end of file
    private void nextAssetView() {
        // save current GUI items to curCSV
        pullGUIEntries();
        // save curCSV overtop of old CSV
        saveGUIEntries();
        if(row >= curCSV.size()){
            // EOF reached, do not increase row count
            Toast.makeText(this, "Last Asset Reached "+ ("\ud83d\ude04"), Toast.LENGTH_LONG).show();
        }
        else{
            // EOF not yet reached, increse row count, update GUI for next Asset
            row++;
            // Load GUI items based on row
            pushGUIEntries();
        }
    }

    private void pullGUIEntries() {
        String[] curRow = curCSV.get(row); // Get the current content found in current row

        // Pull data from Spinners and load it into StringArray at right location
        curRow[0] = fruitSpinner.getSelectedItem().toString();

        curCSV.set(row, curRow); // Save modified row back to curCSV
    }

    private void saveGUIEntries() {
        
    }

    private void pushGUIEntries() {
        fruitSpinner.setSelection(Arrays.asList(fruitOptions).indexOf(curCSV.get(row)[0]));
    }

    // Support functions: No direct association to current state of GUI
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
        return csvROW;
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
        SharedPreferences rowPreference = getSharedPreferences(ROW_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor rowEditor = rowPreference.edit();
        final String saveName = fileName;
        rowEditor.putInt(saveName,row);
    }
}
