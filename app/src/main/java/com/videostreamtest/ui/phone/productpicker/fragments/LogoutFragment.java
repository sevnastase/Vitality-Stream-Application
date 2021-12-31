package com.videostreamtest.ui.phone.productpicker.fragments;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.ui.phone.listeners.PraxFormOnEditorActionListener;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerViewModel;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

public class LogoutFragment extends Fragment {
    private static final String TAG = LogoutFragment.class.getSimpleName();

    private Button logoutButton;
    private EditText password;
    private ProductPickerViewModel productPickerViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logout_block, container, false);

        productPickerViewModel = new ViewModelProvider(requireActivity()).get(ProductPickerViewModel.class);

        logoutButton = view.findViewById(R.id.account_logout_button);
        password = view.findViewById(R.id.password);

        logoutButton.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    final Drawable border = v.getContext().getDrawable(R.drawable.imagebutton_red_border);
                    logoutButton.setBackground(border);
                } else {
                    logoutButton.setBackground(null);
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {

        productPickerViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), config -> {
            password.setOnEditorActionListener(new PraxFormOnEditorActionListener(logoutButton));
            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "Entered password: "+password.getText().toString());
                    if (!password.getText().toString().replace(" ","").contains("")) {
                        Toast.makeText(view.getContext(), "No password entered.", Toast.LENGTH_LONG).show();
                    } else {
                        if (checkPassword(password.getText().toString())) {
                            logout(config);
                        } else {
                            Toast.makeText(view.getContext(), "Wrong password entered.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            });
        });

    }

    private boolean checkPassword(final String password) {
        SharedPreferences myPreferences = getActivity().getApplication().getSharedPreferences("app",0);
        final String appPassword = myPreferences.getString("password", "");
        return password.equals(appPassword) || (myPreferences.getString("apikey", "").length()>0 && appPassword.equals(""));
    }

    private void logout(final Configuration configuration) {
        SharedPreferences sp = getActivity().getApplication().getSharedPreferences("app",0);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.commit();

        configuration.setCurrent(false);
        productPickerViewModel.updateConfiguration(configuration);

        //Cancel all workers (in case of downloading or syncing actions)
        WorkManager
                .getInstance(getActivity().getApplicationContext())
                .cancelAllWork();

        getActivity().finish();
    }
}
