package com.example.fieldassetmanagement;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

public class NewAssetDialog extends AppCompatDialogFragment {
    private EditText newAsset;
    private NewAssetDialogListener listener;
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder NewAssetBuilder = new AlertDialog.Builder(getActivity());

        LayoutInflater NewAssetInflator = getActivity().getLayoutInflater();
        View NewAssetView = NewAssetInflator.inflate(R.layout.new_asset_dialog, null);

        NewAssetBuilder.setView(NewAssetView)
                .setTitle("Create New Asset:")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })

                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newAssetString = newAsset.getText().toString();
                        listener.newAssetName(newAssetString);

                    }
                });
        newAsset = NewAssetView.findViewById(R.id.assetNameDialog);

        return NewAssetBuilder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (NewAssetDialogListener) context;
        } catch (Exception e) {
            throw  new ClassCastException(context.toString() + " must implement NewAssetDialog");
        }
    }

    public interface NewAssetDialogListener{
        void newAssetName(String newAssetName);
    }
}
