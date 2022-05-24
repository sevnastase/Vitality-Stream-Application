package com.videostreamtest.ui.phone.productpicker.fragments.privacypolicy;

import android.content.Intent;
import android.net.Uri;
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

import com.videostreamtest.R;
import com.videostreamtest.ui.phone.helpers.AccountHelper;
import com.videostreamtest.ui.phone.helpers.LogHelper;
import com.videostreamtest.ui.phone.productpicker.ProductPickerViewModel;

public class PrivacyPolicyFragment extends Fragment {
    private final static String TAG = PrivacyPolicyFragment.class.getSimpleName();

    private ProductPickerViewModel productPickerViewModel;
    private Button privacyPolicyButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_privacy_policy, container, false);

        productPickerViewModel = new ViewModelProvider(requireActivity()).get(ProductPickerViewModel.class);

        privacyPolicyButton = view.findViewById(R.id.open_privacy_policy_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        privacyPolicyButton.setOnClickListener((clickedView) -> {
            openWebPage("https://www.praxcloud.nl/privacy-policy/".trim());
        });
    }

    private void openWebPage(String url) {

        Uri webpage = Uri.parse(url);

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            webpage = Uri.parse("http://" + url);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Intent not resolvable.");
            LogHelper.WriteLogRule(getActivity(), AccountHelper.getAccountToken(getActivity().getApplicationContext()), "Browser not installed to open privacy policy.", "ERROR", "");
        }
    }

}
