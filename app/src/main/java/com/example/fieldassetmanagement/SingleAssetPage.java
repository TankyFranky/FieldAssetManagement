package com.example.fieldassetmanagement;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.SharedPreferences;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SingleAssetPage extends AppCompatActivity implements OnItemSelectedListener{

    //TODO make the app non-rotatable
    private String fileName;
    private List<String[]> curCSV;
    private Uri csvURI;
    private int row;

    // Shared Preference Constants
    public static final String ROW_PREFERENCES = "rowPrev";

    // All Spinner declarations
    private Spinner fruitName, fruitColour;

    // All Spinner option declarations
    private String[] fruitNameOptions, fruitColourOptions;

    Button nextSave, prevSave;
    TextView longitude, latitude, fruit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_asset_page);

        Intent openIntent = getIntent();
        String EXTRA_SAP_URI = openIntent.getStringExtra(LandingPage.EXTRA_SAP_URI);
        int EXTRA_SAP_ROW = openIntent.getIntExtra(LandingPage.EXTRA_SAP_ROW, 1);
        String EXTRA_SAP_FILENAME = openIntent.getStringExtra(LandingPage.EXTRA_SAP_FILENAME);
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

        setupGUIEntries();

        pushGUIEntries();
        // Set all TextViews
        fruit.setText(curCSV.get(0)[0]);
        longitude.setText(csvURI.toString());
        latitude.setText(fileName);

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

        if(row >= curCSV.size()-1){
            // EOF reached, do not increase row count
            Toast.makeText(this, "Last Asset Reached "+ ("\ud83d\ude04"), Toast.LENGTH_LONG).show();
        }
        else{
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

        if(row <= 1){
            // EOF reached, do not increase row count
            Toast.makeText(this, "First Asset Reached "+ ("\ud83d\ude04"), Toast.LENGTH_LONG).show();
        }
        else{
            // EOF not yet reached, increase row count, update GUI for next Asset
            row--;
            // Load GUI items based on row
            pushGUIEntries();
        }
    }

    private void pullGUIEntries() {
        //TODO should activate with saveGUIEntries on App shutdown, read below TODO for details
        String[] curRow = curCSV.get(row); // Get the current content found in current row

        // Pull data from Spinners and load it into StringArray at right location
        curRow[2] = fruitColour.getSelectedItem().toString();

        curCSV.set(row, curRow); // Save modified row back to curCSV
    }

    private void saveGUIEntries() {
        String saveCSV = formatForCSV(); // The String that will be saved
        OutputStream overWritter = null;
        ContentResolver saveResolver = this.getContentResolver();
        try {
            overWritter = saveResolver.openOutputStream(csvURI);
            if (overWritter != null) {
                overWritter.write(saveCSV.getBytes());
                overWritter.close();
                Toast.makeText(this, "Save Successful", Toast.LENGTH_LONG).show();

            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Save Failure", Toast.LENGTH_LONG).show();

        }
        //TODO entries must also be saved when the program is shutdown, that way, the next button does not need to be pressed before close (no save necessary)
    }

    private void pushGUIEntries() {
        fruitName.setSelection(Arrays.asList(fruitNameOptions).indexOf(curCSV.get(row)[0]));
        fruitColour.setSelection(Arrays.asList(fruitColourOptions).indexOf(curCSV.get(row)[2]));

    }

    private void setupGUIEntries() {
        // Link variable to XML ID

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
        fruitName = (Spinner) findViewById(R.id.AssetName);
        fruitColour = (Spinner) findViewById(R.id.colourEntry);

        // Spinner Listeners
        fruitName.setOnItemSelectedListener(this);
        fruitColour.setOnItemSelectedListener(this);

        // Spinner Options
        fruitNameOptions = getSpinnerOptions(curCSV, 0);
        fruitColourOptions = getSpinnerOptions(curCSV, 2);

        // Spinner adapters
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fruitNameOptions);
        ArrayAdapter<String> colourAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fruitColourOptions);

        // Dropdown Spinner Styles
        nameAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colourAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach adapter to spinners
        fruitName.setAdapter(nameAdapter);
        fruitColour.setAdapter(colourAdapter);

        fruitName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // save current GUI items to curCSV
                pullGUIEntries();
                // save curCSV over top of old CSV
                saveGUIEntries();
                // save current row to SharedPreferences
                saveRowPreferences();
                // get chosen item from list and find its row number in curCSV
                row = getRowIndex(fruitName.getSelectedItem().toString());
                // update rest of GUI entries
                pushGUIEntries();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });




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
        //TODO This has to be called on app close
        SharedPreferences rowPreference = getSharedPreferences(ROW_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor rowEditor = rowPreference.edit();
        String saveName = fileName;
        rowEditor.putInt(saveName,row);
        rowEditor.apply();
    }
}
