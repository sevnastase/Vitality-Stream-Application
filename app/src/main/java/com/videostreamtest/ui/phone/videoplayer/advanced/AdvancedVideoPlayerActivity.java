package com.videostreamtest.ui.phone.videoplayer.advanced;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerControlView;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastContext;
import com.videostreamtest.R;

public class AdvancedVideoPlayerActivity extends AppCompatActivity implements View.OnClickListener, PlayerManager.QueuePositionListener {

    private PlayerView localPlayerView;
    private PlayerControlView castControlView;
    private PlayerManager playerManager;
    private RecyclerView mediaQueueList;
    private MediaQueueListAdapter mediaQueueListAdapter;
    private CastContext castContext;

    // Activity lifecycle methods.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Getting the cast context later than onStart can cause device discovery not to take place.
        castContext = CastContext.getSharedInstance(this);

        setContentView(R.layout.activity_advanced_video_player);

        localPlayerView = findViewById(R.id.local_player_view);
        localPlayerView.requestFocus();

        castControlView = findViewById(R.id.cast_control_view);

        mediaQueueList = findViewById(R.id.sample_list);

        ItemTouchHelper helper = new ItemTouchHelper(new RecyclerViewCallback());
        helper.attachToRecyclerView(mediaQueueList);
        mediaQueueList.setLayoutManager(new LinearLayoutManager(this));
        mediaQueueList.setHasFixedSize(true);
        mediaQueueListAdapter = new MediaQueueListAdapter();
        mediaQueueList.setAdapter(mediaQueueListAdapter);


        findViewById(R.id.add_sample_button).setOnClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        playerManager =
                PlayerManager.createPlayerManager(
                        /* queuePositionListener= */ this,
                        localPlayerView,
                        castControlView,
                        /* context= */ this,
                        castContext);
        mediaQueueList.setAdapter(mediaQueueListAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        mediaQueueListAdapter.notifyItemRangeRemoved(0, mediaQueueListAdapter.getItemCount());
        mediaQueueList.setAdapter(null);
        playerManager.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu, menu);
        CastButtonFactory.setUpMediaRouteButton(this, menu, R.id.media_route_menu_item);
        return true;
    }

    // Activity input.
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // If the event was not handled then see if the player view can handle it.
        return super.dispatchKeyEvent(event) || playerManager.dispatchKeyEvent(event);
    }

    @Override
    public void onClick(View v) {
        new AlertDialog.Builder(this).setTitle(R.string.sample_list_dialog_title)
                .setView(buildSampleListView())
                .setPositiveButton(android.R.string.ok, null)
                .create()
                .show();
    }


    // PlayerManager.QueuePositionListener implementation.

    @Override
    public void onQueuePositionChanged(int previousIndex, int newIndex) {
        if (previousIndex != C.INDEX_UNSET) {
            mediaQueueListAdapter.notifyItemChanged(previousIndex);
        }
        if (newIndex != C.INDEX_UNSET) {
            mediaQueueListAdapter.notifyItemChanged(newIndex);
        }
    }

    // Internal methods.

    private View buildSampleListView() {
        View dialogList = getLayoutInflater().inflate(R.layout.sample_list, null);
        ListView sampleList = dialogList.findViewById(R.id.sample_list);
        sampleList.setAdapter(new SampleListAdapter(this));
        sampleList.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        playerManager.addItem(DemoUtil.SAMPLES.get(position));
                        mediaQueueListAdapter.notifyItemInserted(playerManager.getMediaQueueSize() - 1);
                    }
                });
        return dialogList;
    }

    // Internal classes.
    private class QueueItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView textView;

        public QueueItemViewHolder(TextView textView) {
            super(textView);
            this.textView = textView;
            textView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            playerManager.selectQueueItem(getAdapterPosition());
        }

    }

    private class MediaQueueListAdapter extends RecyclerView.Adapter<QueueItemViewHolder> {

        @Override
        public QueueItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TextView v = (TextView) LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new QueueItemViewHolder(v);
        }

        @Override
        public void onBindViewHolder(QueueItemViewHolder holder, int position) {
            TextView view = holder.textView;
            view.setText(playerManager.getItem(position).name);
            // TODO: Solve coloring using the theme's ColorStateList.
            view.setTextColor(ColorUtils.setAlphaComponent(view.getCurrentTextColor(),
                    position == playerManager.getCurrentItemIndex() ? 255 : 100));
        }

        @Override
        public int getItemCount() {
            return playerManager.getMediaQueueSize();
        }

    }

    private class RecyclerViewCallback extends ItemTouchHelper.SimpleCallback {

        private int draggingFromPosition;
        private int draggingToPosition;

        public RecyclerViewCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.START | ItemTouchHelper.END);
            draggingFromPosition = C.INDEX_UNSET;
            draggingToPosition = C.INDEX_UNSET;
        }

        @Override
        public boolean onMove(RecyclerView list, RecyclerView.ViewHolder origin,
                              RecyclerView.ViewHolder target) {
            int fromPosition = origin.getAdapterPosition();
            int toPosition = target.getAdapterPosition();
            if (draggingFromPosition == C.INDEX_UNSET) {
                // A drag has started, but changes to the media queue will be reflected in clearView().
                draggingFromPosition = fromPosition;
            }
            draggingToPosition = toPosition;
            mediaQueueListAdapter.notifyItemMoved(fromPosition, toPosition);
            return true;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            if (playerManager.removeItem(position)) {
                mediaQueueListAdapter.notifyItemRemoved(position);
            }
        }

        @Override
        public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (draggingFromPosition != C.INDEX_UNSET) {
                // A drag has ended. We reflect the media queue change in the player.
                if (!playerManager.moveItem(draggingFromPosition, draggingToPosition)) {
                    // The move failed. The entire sequence of onMove calls since the drag started needs to be
                    // invalidated.
                    mediaQueueListAdapter.notifyDataSetChanged();
                }
            }
            draggingFromPosition = C.INDEX_UNSET;
            draggingToPosition = C.INDEX_UNSET;
        }

    }

    private static final class SampleListAdapter extends ArrayAdapter<DemoUtil.Sample> {

        public SampleListAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, DemoUtil.SAMPLES);
        }

    }

}

