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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_SAP_URI = "com.example.fieldassetmanagement.EXTRA_SAP_URI";
    public static final String EXTRA_SAP_ROW = "com.example.fieldassetmanagement.EXTRA_SAP_ROW";
    public static final String EXTRA_SAP_FILENAME = "com.example.fieldassetmanagement.EXTRA_SAP_FILENAME";

    private Uri csvURI;
    private int row = 0;

    private  static final int REQUEST = 69;

    Button fileSelect;
    Button openCSV;
    TextView csvFileName, uriText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO add display of last asset edited Asset based on entered file
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        openCSV = (Button) findViewById(R.id.openCSV);
        fileSelect = (Button) findViewById(R.id.fileSelect);
        uriText = (TextView) findViewById(R.id.uri);
        csvFileName = (TextView) findViewById(R.id.csvFileDisplay);

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
    }

    private void openSingleAssetPage() {
        //TODO Check if a file has been selected
        //TODO Chrashes when no file is selected
        Uri singleAssetURI = csvURI;
        String stringURI =  singleAssetURI.toString();
        row = loadRowPreference(singleAssetURI);
        Intent singleAssetIntent = new Intent(this, SingleAssetPage.class);
        singleAssetIntent.putExtra(EXTRA_SAP_URI, stringURI);
        singleAssetIntent.putExtra(EXTRA_SAP_ROW, row); //TODO get shared preference
        singleAssetIntent.putExtra(EXTRA_SAP_FILENAME, getFileName(singleAssetURI));
        startActivity(singleAssetIntent);
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

                Toast.makeText(this, "Uri: " + csvURI, Toast.LENGTH_LONG).show();
                uriText.setText(csvURI.toString());
                Toast.makeText(this, "Path: " + csvURI.getPath(), Toast.LENGTH_LONG).show();
                csvFileName.setText(getFileName(csvURI));
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

    public int loadRowPreference(Uri fileURI){
        SharedPreferences rowPreference = getSharedPreferences(SingleAssetPage.ROW_PREFERENCES, MODE_PRIVATE);
        int savedRow = rowPreference.getInt(getFileName(fileURI),1);
        return savedRow;
    }

}
