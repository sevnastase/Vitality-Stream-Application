package com.videostreamtest.ui.phone.productpicker.fragments.logout;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.config.entity.Configuration;
import com.videostreamtest.helpers.NavHelper;
import com.videostreamtest.ui.phone.productpicker.ProductPickerViewModel;

import org.jetbrains.annotations.NotNull;

public class LogoutFragment extends Fragment {
    private static final String TAG = LogoutFragment.class.getSimpleName();

    private Button logoutButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable runnable = new Runnable() {
        final int SECONDS_UNTIL_LOGOUT_AVAILABLE = 3;
        int counter = SECONDS_UNTIL_LOGOUT_AVAILABLE;

        @Override
        public void run() {
            if (counter <= 0) {
                logoutButton.setEnabled(true);
                logoutButton.setText(getString(R.string.logout_button_label));
                handler.removeCallbacks(this);
            } else {
                logoutButton.setEnabled(false);
                final String logoutButtonText = getString(R.string.logout_button_label) + String.format("(%d)", counter);
                logoutButton.setText(logoutButtonText);
                counter--;
                handler.postDelayed(this, 1000);
            }
        }
    };

    private ProductPickerViewModel productPickerViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logout_block, container, false);

        productPickerViewModel = new ViewModelProvider(requireActivity()).get(ProductPickerViewModel.class);

        logoutButton = view.findViewById(R.id.account_logout_button);

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

        logoutButton.setOnClickListener(v -> {
            productPickerViewModel.getCurrentConfig().observe(this, configuration -> {
                logout(configuration);
            });
        });

        handler.post(runnable);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {

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

        NavHelper.openPraxtourLauncher(getActivity(), true, () -> {
            Toast.makeText(getActivity(), "Please restart your device", Toast.LENGTH_LONG).show();
        });
    }
}
