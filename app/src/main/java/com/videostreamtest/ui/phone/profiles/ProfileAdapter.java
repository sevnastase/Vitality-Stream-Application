package com.videostreamtest.ui.phone.profiles;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.videostreamtest.R;
import com.videostreamtest.data.model.Profile;

public class ProfileAdapter extends RecyclerView.Adapter<ProfileViewHolder>{

    final static String TAG = ProfileAdapter.class.getSimpleName();

    private Profile[] profiles;
    // Start with first item selected
    private int selectedItem = 0;

    public ProfileAdapter(final Profile[] profiles) {
        this.profiles = profiles;
        Log.d(TAG, "Call ProfileAdapter");
    }


    @NonNull
    @Override
    public ProfileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //Haal application context op
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        //Haal de fragment op waar de recylerview  mee gevuld gaat worden
        View view = layoutInflater.inflate(R.layout.fragment_single_profile, parent, false);
        //retourneer de holder die de koppeling maakt aan de hierboven geselecteerde view
        return new ProfileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileViewHolder holder, int position) {
        Log.d(TAG, "Position: "+position);
        if(selectedItem == position) {
            ImageButton profileImageButton = holder.itemView.findViewById(R.id.profile_avatar);
            profileImageButton.setFocusableInTouchMode(true);
            profileImageButton.setFocusable(true);
            profileImageButton.requestFocus();
        }
        holder.itemView.setSelected(selectedItem == position);
        if (profiles.length > 0) {
            holder.bind(profiles[position], position);
        }
    }

    @Override
    public int getItemCount() {
        return profiles.length;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        Log.d(TAG, "ADAPTER ATTACHED TO RECYCLERVIEW");
    }

}
