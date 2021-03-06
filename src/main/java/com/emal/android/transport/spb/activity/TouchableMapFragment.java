package com.emal.android.transport.spb.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * @author alexey.emelyanenko@gmail.com
 * @since: 1.5
 */
public class TouchableMapFragment extends com.google.android.gms.maps.SupportMapFragment {
    public View mOriginalContentView;
    public TouchableWrapper mTouchView;


    private class TouchableWrapper extends FrameLayout {
        private GMapsV2Activity activity;

        public TouchableWrapper(GMapsV2Activity activity) {
            super(activity);
            this.activity = activity;
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    activity.setTouched(true);
                    break;
                case MotionEvent.ACTION_UP:
                    activity.setTouched(false);
                    break;
            }
            return super.dispatchTouchEvent(event);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        mOriginalContentView = super.onCreateView(inflater, parent, savedInstanceState);
        mTouchView = new TouchableWrapper((GMapsV2Activity) getActivity());
        mTouchView.addView(mOriginalContentView);
        return mTouchView;
    }

    @Override
    public View getView() {
        return mOriginalContentView;
    }
}
