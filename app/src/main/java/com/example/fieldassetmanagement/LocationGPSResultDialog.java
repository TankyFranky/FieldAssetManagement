// TODO make alert dialogs look pretty
// TODO add toast message to encourage re-use of gps locater if accuracy is low.
package com.example.fieldassetmanagement;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class LocationGPSResultDialog extends AppCompatDialogFragment {
    private GPSResultListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder gpsOverwrite = new AlertDialog.Builder(getActivity());
        gpsOverwrite.setTitle("GPS: Location Results.");

        // Possible null location logic
        final Bundle grabName = getArguments();
        final String message;
        if (grabName.get("message") != null) {
            message = grabName.getString("message");
            gpsOverwrite.setMessage(message);
            gpsOverwrite.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            })
            .setPositiveButton("Update Location Data", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    double latitude = grabName.getDouble("latitude");
                    double longitude = grabName.getDouble("longitude");
                    listener.onNewGPSResult(Double.toString(SingleAssetPage.round(latitude,6)), Double.toString(SingleAssetPage.round(longitude,6)));
                }
            });
        }
        else{
            gpsOverwrite.setMessage("Location could not be accurately determined at this time.\n" +
                    "Please try again later");
            gpsOverwrite.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
        }


        return gpsOverwrite.create();
    }

    public interface GPSResultListener {
        void onNewGPSResult(String latitudeAD, String longitudeAD);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try{
            listener = (GPSResultListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "Pop-up dialog error, must implement Listener (GPS)");
        }
    }
}
