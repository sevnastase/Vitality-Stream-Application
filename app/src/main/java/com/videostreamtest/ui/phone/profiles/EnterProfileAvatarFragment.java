package com.videostreamtest.ui.phone.profiles;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.picasso.Picasso;
import com.videostreamtest.R;
import com.videostreamtest.data.model.response.Result;
import com.videostreamtest.workers.AddProfileServiceWorker;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link EnterProfileAvatarFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class EnterProfileAvatarFragment extends Fragment {

    private final static String TAG = EnterProfileNameFragment.class.getSimpleName();

    private ProfileViewModel profileViewModel;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private Button confirmButton;
    private ImageButton avatarMaleView;
    private ImageButton avatarFemaleView;
    private float alphaDefaultValue = 0.6f;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public EnterProfileAvatarFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment EnterProfileAvatarFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static EnterProfileAvatarFragment newInstance(String param1, String param2) {
        EnterProfileAvatarFragment fragment = new EnterProfileAvatarFragment();
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
        return inflater.inflate(R.layout.fragment_enter_profile_avatar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        hideKeyboard();

        avatarMaleView = view.findViewById(R.id.profile_avatar_holder_male);
        //Load male avatar with picasso
        Picasso.get()
                .load("http://188.166.100.139:8080/api/dist/img/avatar5.png")
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(avatarMaleView);
        //set onfocuschange listener
        avatarMaleView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                drawSelectionBorder(avatarMaleView);
            } else {
                undrawSelectionBorder(avatarMaleView);
            }
        });
        avatarMaleView.setOnClickListener(v -> {
            selectProfileAvatar(avatarMaleView);
        });

        avatarFemaleView = view.findViewById(R.id.profile_avatar_holder_female);
        //Load female avatar with picasso
        Picasso.get()
                .load("http://188.166.100.139:8080/api/dist/img/avatar2.png")
                .placeholder(R.drawable.cast_album_art_placeholder)
                .error(R.drawable.cast_ic_notification_disconnect)
                .into(avatarFemaleView);
        //set onfocuschange listener
        avatarFemaleView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                drawSelectionBorder(avatarFemaleView);
            } else {
                undrawSelectionBorder(avatarFemaleView);
            }
        });
        avatarFemaleView.setOnClickListener(v -> {
            selectProfileAvatar(avatarFemaleView);
        });

        drawSelectionBorder(avatarFemaleView);
        undrawSelectionBorder(avatarFemaleView);
        drawSelectionBorder(avatarMaleView);
        undrawSelectionBorder(avatarMaleView);

        confirmButton = view.findViewById(R.id.profile_add_confirm_button);
        confirmButton.setOnClickListener((v) -> {
            addProfileToAccount();
        });

        confirmButton.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (avatarMaleView.isSelected()) {
                    avatarMaleView.setAlpha(1.0f);
                }
                if (avatarFemaleView.isSelected()) {
                    avatarFemaleView.setAlpha(1.0f);
                }
                confirmButton.setBackgroundTintMode(PorterDuff.Mode.ADD);
            } else {
                confirmButton.setBackgroundTintMode(PorterDuff.Mode.SRC_OVER);
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), 0);
    }

    private void drawSelectionBorder(ImageButton selectedImage) {
        avatarMaleView.setAlpha(alphaDefaultValue);
        avatarFemaleView.setAlpha(alphaDefaultValue);
        avatarMaleView.setSelected(false);
        avatarFemaleView.setSelected(false);
        final Drawable border = ContextCompat.getDrawable(getActivity(), R.drawable.imagebutton_blue_border);
        selectedImage.setBackground(border);
        selectedImage.setAlpha(1.0f);
        selectedImage.setSelected(true);
    }

    private void undrawSelectionBorder(ImageButton selectedImage) {
        selectedImage.setBackground(null);
        selectedImage.setAlpha(alphaDefaultValue);
    }

    private void selectProfileAvatar(ImageButton selectedImage) {
        avatarMaleView.setAlpha(alphaDefaultValue);
        avatarFemaleView.setAlpha(alphaDefaultValue);
        avatarMaleView.setSelected(false);
        avatarFemaleView.setSelected(false);
        selectedImage.setAlpha(1.0f);
        selectedImage.setSelected(true);
        confirmButton.requestFocus();
    }

    private void addProfileToAccount() {
        /*
        Add serviceworker onetime request
        response successful then proceed to ProfileActivity
         */
        SharedPreferences myPreferences = getActivity().getSharedPreferences("app",0);
        String apikey = myPreferences.getString("apikey", "unauthorized");

        String imgpath = "";
        if (avatarMaleView.getAlpha() == 1.0f) {
            imgpath = "http://188.166.100.139:8080/api/dist/img/avatar5.png";
        }
        if (avatarFemaleView.getAlpha() == 1.0f) {
            imgpath = "http://188.166.100.139:8080/api/dist/img/avatar2.png";
        }

        Data.Builder networkData = new Data.Builder();
        networkData.putString("apikey", apikey);
        networkData.putString("profilename", profileViewModel.getProfileName().getValue());
        networkData.putString("imgpath", imgpath);

        OneTimeWorkRequest addProfileRequest = new OneTimeWorkRequest.Builder(AddProfileServiceWorker.class)
                .setInputData(networkData.build())
                .addTag("addprofile")
                .build();

        WorkManager
                .getInstance(this.getActivity())
                .enqueue(addProfileRequest);

        WorkManager.getInstance(this.getActivity())
                .getWorkInfoByIdLiveData(addProfileRequest.getId())
                .observe(this.getActivity(), workInfo -> {

                    if( workInfo.getState() != null &&
                            workInfo.getState() == WorkInfo.State.SUCCEEDED ) {

                        final String result = workInfo.getOutputData().getString("add-profile-result");

                        try {
                            final ObjectMapper objectMapper = new ObjectMapper();
                            Result responseResult = objectMapper.readValue(result, Result.class);
                            //Check if there are profiles else show add profile form
                            if (responseResult.getStatusCode() == 200) {
                                ProfilesActivity.getInstance().notifyDataSet();
                                getActivity().finish();
                            } else {
                                Toast.makeText(this.getActivity(), getString(R.string.profile_add_form_error),Toast.LENGTH_LONG).show();
                            }
                        } catch (JsonMappingException jsonMappingException) {
                            Log.e(TAG, jsonMappingException.getLocalizedMessage());
                        } catch (JsonProcessingException jsonProcessingException) {
                            Log.e(TAG, jsonProcessingException.getLocalizedMessage());
                        }
                    }
                });
    }
}