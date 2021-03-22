package com.videostreamtest.ui.phone.profiles;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.videostreamtest.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EnterProfileNameFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EnterProfileNameFragment extends Fragment {

    private ProfileViewModel profileViewModel;
    private EditText profileInputView;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EnterProfileNameFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EnterProfileNameFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EnterProfileNameFragment newInstance(String param1, String param2) {
        EnterProfileNameFragment fragment = new EnterProfileNameFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        profileViewModel = new ViewModelProvider(getActivity()).get(ProfileViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_enter_profile_name, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Button nextButton = view.findViewById(R.id.profile_add_next_button);

        profileInputView = getView().findViewById(R.id.profile_add_name);
        profileInputView.setOnEditorActionListener((editTextView, actionid, keyEvent) -> {
            boolean handled = false;
            if (actionid == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard();
                nextButton.requestFocus();
                handled = true;
            }
            return handled;
        });
        profileInputView.setOnFocusChangeListener((editTextView, hasFocus) -> {
            if (hasFocus) {
                showKeyboard();
            } else {
                hideKeyboard();
            }
        });

        nextButton.setOnClickListener((v) -> {
            if (!profileInputView.getText().toString().isEmpty() || !profileInputView.getText().toString().equalsIgnoreCase("")) {
                profileViewModel.getProfileName().setValue(profileInputView.getText().toString());
                NavHostFragment.findNavController(EnterProfileNameFragment.this)
                        .navigate(R.id.action_enterProfileNameFragment_to_enterProfileAvatarFragment);
            } else {
                Toast.makeText(this.getActivity(), getString(R.string.profile_add_form_warning), Toast.LENGTH_LONG).show();
                profileInputView.requestFocus();
            }
        });
        nextButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                nextButton.setBackgroundTintMode(PorterDuff.Mode.ADD);
            } else {
                nextButton.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        if (imm.isActive() && getActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.showSoftInput(getActivity().getCurrentFocus(), 0);
    }
}