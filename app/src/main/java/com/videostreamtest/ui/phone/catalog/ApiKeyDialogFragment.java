package com.videostreamtest.ui.phone.catalog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Point;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.videostreamtest.R;

public class ApiKeyDialogFragment extends DialogFragment {

    private CatalogViewModel catalogViewModel;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragment_apikey, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        catalogViewModel = new ViewModelProvider(requireActivity()).get(CatalogViewModel.class);
        setupView(view);
        setupFilters(view);
        setupViewListeners(view);
    }

    private void setupView(@NonNull View view) {
        getDialog().setTitle("No API Key found!");
        view.findViewById(R.id.user_apikey).requestFocus();
        setCancelable(false);
    }

    private void setupViewListeners(@NonNull View view) {
        EditText inputText = view.findViewById(R.id.user_apikey);
        Button apikeyButton = view.findViewById(R.id.apikey_enter_button);

        apikeyButton.setOnClickListener(v -> {
            if(inputText.getText().length() > 0 ) {
                catalogViewModel.setApiKey(inputText.getText().toString());
                dismiss();
            }
        });
    }

    private void setupFilters(@NonNull View view) {
        EditText inputText = view.findViewById(R.id.user_apikey);
        InputFilter filter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isWhitespace(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        inputText.setFilters(new InputFilter[]{filter});
    }
}
