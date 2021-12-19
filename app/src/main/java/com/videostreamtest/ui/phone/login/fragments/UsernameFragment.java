package com.videostreamtest.ui.phone.login.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.login.LoginViewModel;

import org.jetbrains.annotations.NotNull;

public class UsernameFragment extends Fragment {

    private LoginViewModel loginViewModel;
    private EditText usernameInput;
    private Button nextButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_enter_username, container, false);
        loginViewModel = new ViewModelProvider(requireActivity()).get(LoginViewModel.class);

        usernameInput = view.findViewById(R.id.login_insert_username_input);
        nextButton = view.findViewById(R.id.login_goto_password_button);

        nextButton.setOnClickListener((onClickedView)-> {
            if (usernameInput.getText().length() > 0) {
                loginViewModel.setUsername(usernameInput.getText().toString());
                NavHostFragment.findNavController(UsernameFragment.this)
                        .navigate(R.id.action_usernameFragment_to_passwordFragment);
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loginViewModel.getUsername().observe(getViewLifecycleOwner(), username -> {
            if (username!= null && username.length()>0) {
                usernameInput.setText(username);
            }
        });

        usernameInput.setFocusable(true);
        usernameInput.requestFocus();
    }
}
