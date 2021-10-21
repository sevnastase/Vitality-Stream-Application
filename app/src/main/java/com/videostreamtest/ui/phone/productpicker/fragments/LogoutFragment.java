package com.videostreamtest.ui.phone.productpicker.fragments;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.productpicker.ProductPickerActivity;
import com.videostreamtest.ui.phone.productpicker.ProductPickerViewModel;
import com.videostreamtest.ui.phone.productview.viewmodel.ProductViewModel;

import org.jetbrains.annotations.NotNull;

public class LogoutFragment extends Fragment {
    private static final String TAG = LogoutFragment.class.getSimpleName();

    private Button logoutButton;
    private ProductPickerViewModel productPickerViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_logout_block, container, false);

        productPickerViewModel = new ViewModelProvider(requireActivity()).get(ProductPickerViewModel.class);;

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

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {

        productPickerViewModel.getCurrentConfig().observe(getViewLifecycleOwner(), config ->{
            logoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    SharedPreferences sp = getActivity().getApplication().getSharedPreferences("app",0);
                    SharedPreferences.Editor editor = sp.edit();
                    editor.clear();
                    editor.commit();

                    config.setCurrent(false);
                    productPickerViewModel.updateConfiguration(config);

                    //Cancel all workers (in case of downloading or syncing actions)
                    WorkManager
                            .getInstance(getActivity().getApplicationContext())
                            .cancelAllWork();

                    getActivity().finish();
                }
            });
        });

    }
}
