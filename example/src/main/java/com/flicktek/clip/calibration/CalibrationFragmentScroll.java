package com.flicktek.clip.calibration;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.calibration.R;
import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.eventbus.ConnectedEvent;
import com.flicktek.clip.eventbus.ConnectingEvent;
import com.flicktek.clip.eventbus.DeviceToWearEvent;
import com.flicktek.clip.eventbus.DisconnectedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class CalibrationFragmentScroll extends Fragment implements View.OnClickListener {
    private String TAG = "CalibrationScroll";

    private MainActivity mainActivity;
    private Button mClose;
    private Button mStart;

    private boolean mRepeat = false;

    TextView mConnectingText;

    public void repeatCalibration(boolean repeat) {
        mRepeat = repeat;
    }

    //fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();

        View rootView = null;

        rootView = inflater.inflate(R.layout.fragment_calib_welcome, container, false);

        mClose = (Button) rootView.findViewById(R.id.b_close);
        mClose.setOnClickListener(this);
        mClose.setVisibility(View.GONE);

        mStart = (Button) rootView.findViewById(R.id.b_start);
        mStart.setOnClickListener(this);

        mConnectingText = (TextView) rootView.findViewById(R.id.tv_menu_title);
        if (mRepeat) {
            mConnectingText.setText("Would you like to calibrate again?");
            mStart.setText("Repeat");
            mClose.setVisibility(View.VISIBLE);
        }

        return rootView;
    }

    public void close() {
        mainActivity.finish();
    }

    @Subscribe
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        int gesture = gestureEvent.status;
        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
            case (FlicktekManager.GESTURE_DOWN):
            case (FlicktekManager.GESTURE_ENTER):
                showEnterFragment();
                break;
            case (FlicktekManager.GESTURE_HOME):
                close();
                break;
        }
    }

    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void showEnterFragment() {
        final Fragment _fragment = new CalibrationFragmentAnimated();
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                    transaction.replace(R.id.container, _fragment);
                    transaction.addToBackStack("CalibrationFragment");
                    transaction.commit();
                    for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
                        FragmentManager.BackStackEntry backStackEntryAt = fragmentManager.getBackStackEntryAt(entry);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onClick(View _view) {
        if (_view == mClose) close();
        if (_view == mStart) {
            if (FlicktekManager.getInstance().isHandshakeOk()) {
                showEnterFragment();
            } else {
                if (FlicktekManager.getInstance().isConnecting()) {
                    mainActivity.showMessage("Trying to connect");
                } else {
                    mainActivity.onConnectClicked(null);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceReady(FlicktekCommands.onDeviceReady event) {
        showEnterFragment();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceConnected(ConnectedEvent event) {
    }

    @Subscribe
    public void onDeviceToWearEvent(DeviceToWearEvent event) {
        mainActivity.runOnUiThread(new Runnable() {
                                       public void run() {
                                           Toast.makeText(mainActivity, "Cancelled, connecting to wear", Toast.LENGTH_SHORT).show();
                                       }
                                   }
        );
        close();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLinkLost(DisconnectedEvent event) {
        mConnectingText.setText("Try again to scan for devices...");
        mStart.setEnabled(true);
        mStart.setVisibility(View.VISIBLE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnecting(ConnectingEvent event) {
        mConnectingText.setText("Connecting!...");
        mStart.setEnabled(false);
        mStart.setVisibility(View.GONE);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLinkRestablished(ConnectedEvent connectedEvent) {
    }
}
