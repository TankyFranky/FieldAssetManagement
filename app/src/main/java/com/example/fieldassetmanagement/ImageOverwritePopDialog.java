package com.example.fieldassetmanagement;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class ImageOverwritePopDialog extends AppCompatDialogFragment {
    private imgOverwriteListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle grabName = getArguments();
        assert grabName != null;
        final String name = grabName.getString("name");
        AlertDialog.Builder imgOverwrite = new AlertDialog.Builder(getActivity());
        imgOverwrite.setTitle("Attention: Image Overwrite");
        imgOverwrite.setMessage("Image " + name + " already exists. Proceeding will overwrite it.");
        // TODO make the set message dynamic
        imgOverwrite.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        })

        .setPositiveButton("New Photo", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                listener.onNewPhotoClicked();
            }
        });

        return imgOverwrite.create();
    }

    public interface imgOverwriteListener{
        void onNewPhotoClicked();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try{
            listener = (imgOverwriteListener) context;
        } catch (ClassCastException e){
            throw new ClassCastException(context.toString() + "Pop-up dialog error, must implent Listener (ImageOverwrite)");
        }
    }
}
