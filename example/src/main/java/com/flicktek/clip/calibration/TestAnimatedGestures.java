package com.flicktek.clip.calibration;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.calibration.R;
import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class TestAnimatedGestures extends Fragment implements View.OnClickListener {
    private String TAG = "TestAnimatedGestures";

    private MainActivity mainActivity;
    private Button b_close;

    private TextView tv_status;
    private TextView tv_title;
    private ImageView iv_splash;

    private int gesture_number = 0;

    /**
     * Resources to use on this gesture calibration
     */
    private int title;
    private int res_layout_calibration;
    private int animation_success;
    private int res_main_icon;

    public static TestAnimatedGestures newInstance(int gesture_number) {
        TestAnimatedGestures myFragment = new TestAnimatedGestures();

        Bundle args = new Bundle();
        args.putInt("gesture", gesture_number);
        myFragment.setArguments(args);
        return myFragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            gesture_number = getArguments().getInt("gesture");
        } catch (Exception e) {

        }

        animation_success = R.anim.calibration_enter;

        switch (gesture_number) {
            case FlicktekManager.GESTURE_ENTER:
                res_layout_calibration = R.layout.fragment_animated_gesture;
                title = R.string.enter;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_enter_circle_black;
                break;
            case FlicktekManager.GESTURE_HOME:
                title = R.string.home;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_home_circle_black;
                break;
            case FlicktekManager.GESTURE_UP:
                title = R.string.up;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_up_circle_black;
                break;
            case FlicktekManager.GESTURE_DOWN:
                title = R.string.down;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_down_circle_black;
                break;
            default:
                title = R.string.perform_gesture;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_circle_black;
                break;
        }

        mainActivity = (MainActivity) getActivity();
        View rootView = null;

        rootView = inflater.inflate(res_layout_calibration, container, false);

        b_close = (Button) rootView.findViewById(R.id.b_close);
        b_close.setOnClickListener(this);

        iv_splash = (ImageView) rootView.findViewById(R.id.calibration_icon);
        iv_splash.setImageResource(res_main_icon);

        tv_status = (TextView) rootView.findViewById(R.id.tv_status);
        tv_status.setText("");

        tv_title = (TextView) rootView.findViewById(R.id.tv_title);
        tv_title.setText(title);

        animate_icon(animation_success);
        return rootView;
    }

    private boolean anim_running = false;

    public void animate_icon(int animation) {
        Animation anim = AnimationUtils.loadAnimation(
                mainActivity.getApplicationContext(),
                animation);

        iv_splash.setVisibility(View.VISIBLE);
        iv_splash.startAnimation(anim);
    }

    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        if (nextAnim == 0) {
            nextAnim = R.animator.fade_in;
        }

        final Animator anim = AnimatorInflater.loadAnimator(mainActivity, nextAnim);
        if (anim == null) {
            return null;
        }

        return anim;
    }

    public void close() {
        getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        CalibrationFragmentScroll fragment = new CalibrationFragmentScroll();
        fragment.repeatCalibration(true);
        mainActivity.showFragment(fragment, "CalibrationFragmentScroll", false);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void showFragment() {
        final Fragment _fragment = newInstance(gesture_number);
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                    transaction.replace(R.id.container, _fragment);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(final FlicktekCommands.onGestureEvent gestureEvent) {
        gesture_number = gestureEvent.status;
        showFragment();
    }

    public void onClick(View _view) {
        if (_view == b_close)
            close();
    }
}
