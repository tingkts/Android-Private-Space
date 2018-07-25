package com.ting.privatephoto.view;

import android.app.Activity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import com.ting.privatephoto.R;
import com.ting.privatephoto.util.Log;

public class BottomBar implements View.OnClickListener{
    public enum Type {ADD, ITEM_ACTION_ADD2PRIVATE, ITEM_ACTIONS_DEL_MOVE_RESTORE}
    public enum ActionCode {ADD_OR_ADD2PRIVATE, DELETE, MOVE, RESTORE }

    public interface OnButtonClickListener {
        void onButtonBarButtonClicked(ActionCode actionCode, int viewId);
    }
    private static final int CONTAINER_ANIM_DURATION_MS = 200;
    private Animation containerAnimIn = new AlphaAnimation(0f, 1f);
    private Animation containerAnimOut = new AlphaAnimation(1f, 0f);
    private View root;
    private OnButtonClickListener onButtonClickListener;

    public BottomBar(Type type, Activity containerActivity, OnButtonClickListener _onButtonClickListener ) {
        if (type == Type.ADD || type == Type.ITEM_ACTION_ADD2PRIVATE) {
            root = containerActivity.findViewById(R.id.id_bottom_bar_add2private_root);
            TextView add2private = (TextView) containerActivity.findViewById(R.id.id_bottom_bar_add2private);
            add2private.setOnClickListener(this);
            if (type == Type.ADD) {
                add2private.setText(R.string.top_bar_title_public_album_list);
                setVisible(true);
            }
        } else if (type == Type.ITEM_ACTIONS_DEL_MOVE_RESTORE) {
            root = containerActivity.findViewById(R.id.id_bottom_bar_item_actions_root);
            TextView delete = (TextView) containerActivity.findViewById(R.id.id_bottom_bar_item_actions_del);
            TextView move = (TextView) containerActivity.findViewById(R.id.id_bottom_bar_item_actions_move);
            TextView restore = (TextView) containerActivity.findViewById(R.id.id_bottom_bar_item_actions_restore);
            delete.setOnClickListener(this);
            move.setOnClickListener(this);
            restore.setOnClickListener(this);
        }
        containerAnimIn.setDuration(CONTAINER_ANIM_DURATION_MS);
        containerAnimOut.setDuration(CONTAINER_ANIM_DURATION_MS);
        onButtonClickListener = _onButtonClickListener;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        Log.d(getClass().getSimpleName(), "[onClick] viewId=" + viewId);
        if (viewId == R.id.id_bottom_bar_add2private) {
            onButtonClickListener.onButtonBarButtonClicked(ActionCode.ADD_OR_ADD2PRIVATE, viewId);}
        else if (viewId == R.id.id_bottom_bar_item_actions_del) {
            onButtonClickListener.onButtonBarButtonClicked(ActionCode.DELETE, viewId);}
        else if (viewId == R.id.id_bottom_bar_item_actions_move) {
            onButtonClickListener.onButtonBarButtonClicked(ActionCode.MOVE, viewId);}
        else if (viewId == R.id.id_bottom_bar_item_actions_restore) {
            onButtonClickListener.onButtonBarButtonClicked(ActionCode.RESTORE, viewId);}
    }

    public void setVisible(boolean visible) {
        if (visible) {
            root.clearAnimation();
            containerAnimIn.reset();
            root.startAnimation(containerAnimIn);
            root.setVisibility(View.VISIBLE);
        } else {
            root.clearAnimation();
            containerAnimOut.reset();
            root.startAnimation(containerAnimOut);
            root.setVisibility(View.INVISIBLE);
        }
    }
}