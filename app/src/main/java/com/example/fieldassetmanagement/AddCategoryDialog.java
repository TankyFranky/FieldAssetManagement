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

public class AddCategoryDialog extends AppCompatDialogFragment {
    private EditText newCategory;
    private AddCategoryDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final AlertDialog.Builder AddCategoryBuilder = new AlertDialog.Builder(getActivity());

        LayoutInflater AddCategoryInflator = getActivity().getLayoutInflater();
        final View AddCategoryView = AddCategoryInflator.inflate(R.layout.new_category_dialog, null);

        AddCategoryBuilder.setView(AddCategoryView)
                .setTitle("Add New Category")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel(); // debug tell me I am reaching this call
                    }
                })

                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String addCategoryString = newCategory.getText().toString();
                        listener.addCategory(addCategoryString);
                    }
                });
         newCategory = AddCategoryView.findViewById(R.id.newCategoryDialog);

        return AddCategoryBuilder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (AddCategoryDialogListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement AddCategoryDialog");
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        listener.cancelCategoryDialog();
    }

    public interface AddCategoryDialogListener{
        void addCategory(String addCategory);

        void cancelCategoryDialog();
    }
}
