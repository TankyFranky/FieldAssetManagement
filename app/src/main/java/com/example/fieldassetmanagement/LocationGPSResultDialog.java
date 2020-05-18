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
        Bundle grabName = getArguments();
        assert grabName != null;
        final String message = grabName.getString("message");
        AlertDialog.Builder gpsOverwrite = new AlertDialog.Builder(getActivity());
        gpsOverwrite.setTitle("GPS: Location Results.");
        gpsOverwrite.setMessage(message);
        // TODO make the set message dynamic
        gpsOverwrite.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.onNewGPSResult();
                    }
                });

        return gpsOverwrite.create();
    }

    public interface GPSResultListener {
        void onNewGPSResult();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try{
            listener = (GPSResultListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "Pop-up dialog error, must implent Listener (GPS)");
        }
    }
}
