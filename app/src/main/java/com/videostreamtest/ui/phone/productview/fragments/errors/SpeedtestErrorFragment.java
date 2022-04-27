package com.videostreamtest.ui.phone.productview.fragments.errors;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;

import org.jetbrains.annotations.NotNull;

public class SpeedtestErrorFragment extends Fragment {
    private static final String TAG = SpeedtestErrorFragment.class.getSimpleName();

    private TextView errorMessage;
    private Button backButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_speedtest_failed, container, false);
        errorMessage = view.findViewById(R.id.error_message);
        backButton = view.findViewById(R.id.error_message_back_button);

        if (getArguments()!=null && getArguments().getInt("measured_speed") > 0) {
            final Bundle arguments = getArguments();
            int neededSpeedBits = arguments.getInt("needed_speed");
            int measuredSpeedBits = arguments.getInt("measured_speed");

            errorMessage.setText(String.format(getString(R.string.error_message_speedtest),  measuredSpeedBits/1000/1000, neededSpeedBits/1000/1000));
        }

        backButton.setOnClickListener((onClickedView -> {
            NavHostFragment navHostFragment =
                    (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.product_fragment_view);
            NavController navController = navHostFragment.getNavController();
            navController.navigate(R.id.routeFragment);
        }));

        backButton.setOnFocusChangeListener((onFocusedView, hasFocus)-> {
            if (hasFocus) {
                final Drawable border = getActivity().getDrawable(R.drawable.imagebutton_red_border);
                backButton.setBackground(border);
            } else {
                backButton.setBackground(null);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        backButton.setFocusable(true);
        backButton.requestFocus();
    }
}