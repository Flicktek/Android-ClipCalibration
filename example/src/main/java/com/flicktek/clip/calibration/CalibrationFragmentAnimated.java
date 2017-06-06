package com.flicktek.clip.calibration;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.flicktek.calibration.R;
import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.eventbus.DeviceToWearEvent;
import com.flicktek.clip.flickgym.MyGLRenderer;
import com.flicktek.clip.flickgym.MyGLSurfaceView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Random;

import static android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE;

public class CalibrationFragmentAnimated extends Fragment implements View.OnClickListener, MediaPlayer.OnCompletionListener {
    private String TAG = "CalibrationEnter";

    private MainActivity mainActivity;
    private Button b_close;
    private Button b_repeat;

    private int status;

    private boolean bFinishedCalibration = false;
    public boolean bCloseCalibration = false;

    private TextView tv_status;
    private TextView tv_title;
    private ImageView iv_splash;
    private ImageView iv_number;
    private RelativeLayout rl_number_container;

    private int iteration = 1;
    private int gestures_left = 0;
    private int gesture_number = 0;

    public static final int GESTURE_ENTER = 0;
    public static final int GESTURE_HOME = 1;
    public static final int GESTURE_UP = 2;
    public static final int GESTURE_DOWN = 3;
    public static final int GESTURE_EXTRA = 4;

    private static int G_NUMB_MAX = 0;
    private static int G_ITER_MAX = 0;

    /**
     * Resources to use on this gesture calibration
     */
    private int res_layout_calibration;
    private int title;
    private int animation_success;
    private int animation_error;
    private int res_main_icon;

    private MyGLSurfaceView mGLView;

    public static CalibrationFragmentAnimated newInstance(int gesture_number) {
        CalibrationFragmentAnimated myFragment = new CalibrationFragmentAnimated();

        Bundle args = new Bundle();
        args.putInt("gesture", gesture_number);
        myFragment.setArguments(args);
        return myFragment;
    }

    VideoView mVideoView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            gesture_number = getArguments().getInt("gesture");
        } catch (Exception e) {

        }

        FlicktekManager.getInstance().setCalibrationMode(true);

        animation_success = R.anim.calibration_enter;
        animation_error = R.anim.calibration_error;

        mainActivity = (MainActivity) getActivity();
        View rootView = null;

        int video_res = 0;
        switch (gesture_number) {
            case GESTURE_ENTER:
                res_layout_calibration = R.layout.fragment_calib_white;
                title = R.string.calibration_enter;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_enter_circle_black;
                video_res = R.raw.g_enter_snap;
                break;
            case GESTURE_HOME:
                title = R.string.calibration_home;
                res_layout_calibration = R.layout.fragment_calib_black;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_home_circle_black;
                video_res = R.raw.g_home_handspread;
                break;
            case GESTURE_UP:
                title = R.string.calibration_up;
                res_layout_calibration = R.layout.fragment_calib_white;
                animation_success = R.anim.calibration_up;
                res_main_icon = R.drawable.ic_up_circle_black;
                video_res = R.raw.g_up_flickindex;
                break;
            case GESTURE_DOWN:
                title = R.string.calibration_down;
                res_layout_calibration = R.layout.fragment_calib_black;
                animation_success = R.anim.calibration_down;
                res_main_icon = R.drawable.ic_down_circle_black;
                video_res = R.raw.g_down_rubthumb;
                break;

        }
        rootView = inflater.inflate(res_layout_calibration, container, false);

        mGLView = (MyGLSurfaceView) rootView.findViewById(R.id.surfaceviewclass);
        if (mGLView != null) {

            MyGLRenderer renderer = mGLView.mRenderer;

            renderer.mSteps = 1;
            renderer.forceFlatLine();

            if (res_layout_calibration == R.layout.fragment_calib_white) {
                renderer.setBackgroundColor(1.0f, 1.0f, 1.0f);
                renderer.setBlendMode(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                renderer.setBackgroundColor(0.0f, 0.0f, 0.0f);
                renderer.setBlendMode(GLES20.GL_ONE, GLES20.GL_ONE);
            }
        }

        b_close = (Button) rootView.findViewById(R.id.b_close);
        b_close.setOnClickListener(this);

        b_repeat = (Button) rootView.findViewById(R.id.b_repeat);
        b_repeat.setOnClickListener(this);

        iv_splash = (ImageView) rootView.findViewById(R.id.calibration_icon);
        iv_splash.setImageResource(res_main_icon);

        tv_status = (TextView) rootView.findViewById(R.id.tv_status);
        tv_status.setText("");

        tv_title = (TextView) rootView.findViewById(R.id.tv_title);
        tv_title.setText(title);

        if (gesture_number == GESTURE_ENTER) {
            G_NUMB_MAX = FlicktekCommands.getInstance().getGesturesNumber();
            G_ITER_MAX = FlicktekCommands.getInstance().getIterationsNumber();
        }

        iv_number = (ImageView) rootView.findViewById(R.id.counting_icon);

        rl_number_container = (RelativeLayout) rootView.findViewById(R.id.counter_container);

        LinearLayout myll = (LinearLayout) rootView.findViewById(R.id.random_layout);
        if (myll != null) {
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                myll.setOrientation(LinearLayout.HORIZONTAL);
            } else {
                myll.setOrientation(LinearLayout.VERTICAL);
            }
        }

        //Displays a video file.
        try {
            mVideoView = (VideoView) rootView.findViewById(R.id.videoview);
            if (mVideoView != null) {
                if (video_res != 0) {
                    String uriPath = "android.resource://" + mainActivity.getPackageName() + "/" + video_res;
                    Uri uri = Uri.parse(uriPath);
                    mVideoView.setVideoURI(uri);
                    mVideoView.requestFocus();
                    mVideoView.start();
                    mVideoView.setOnCompletionListener(this);
                } else {
                    mVideoView.setVisibility(View.GONE);
                }
            }
        } catch (Exception e) {
        }

        return rootView;
    }

    private boolean anim_running = false;

    public void animate_number() {
        if (anim_running)
            return;

        final Animation anim = AnimationUtils.loadAnimation(
                mainActivity.getApplicationContext(),
                R.anim.calibration_number_rotation_out);

        rl_number_container.setVisibility(View.VISIBLE);
        rl_number_container.startAnimation(anim);

        gestures_left = G_ITER_MAX - iteration + 1;

        int value = 0;
        switch (gesture_number) {
            case GESTURE_ENTER:
                value = FlicktekManager.GESTURE_ENTER;
                break;
            case GESTURE_HOME:
                value = FlicktekManager.GESTURE_HOME;
                break;
            case GESTURE_UP:
                value = FlicktekManager.GESTURE_UP;
                break;
            case GESTURE_DOWN:
                value = FlicktekManager.GESTURE_DOWN;
                break;
        }
        FlicktekCommands.getInstance().onGestureChanged(value);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                anim_running = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                anim.setAnimationListener(null);

                Animation anim = AnimationUtils.loadAnimation(
                        mainActivity.getApplicationContext(),
                        R.anim.calibration_number_rotation_in);

                int res;
                switch (gestures_left) {
                    case 1:
                        res = R.drawable.ic_looks_one_black_48dp;
                        break;
                    case 2:
                        res = R.drawable.ic_looks_two_black_48dp;
                        break;
                    case 3:
                        res = R.drawable.ic_looks_3_black_48dp;
                        break;
                    case 4:
                        res = R.drawable.ic_looks_4_black_48dp;
                        break;
                    case 5:
                        res = R.drawable.ic_looks_5_black_48dp;
                        break;
                    default:
                        res = R.drawable.ic_looks_6_black_48dp;
                        break;
                }

                iv_number.setImageResource(res);
                rl_number_container.setVisibility(View.VISIBLE);
                rl_number_container.startAnimation(anim);
                anim_running = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    public void animate_icon(int animation) {
        Animation anim = AnimationUtils.loadAnimation(
                mainActivity.getApplicationContext(),
                animation);

        iv_splash.setVisibility(View.VISIBLE);
        iv_splash.startAnimation(anim);
    }

    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        if (mainActivity == null) {
            Log.v(TAG, "Something went very wrong!");
            return null;
        }

        if (nextAnim == 0) {
            nextAnim = R.animator.fade_out;
        }

        final Animator anim = AnimatorInflater.loadAnimator(mainActivity, nextAnim);
        if (anim == null) {
            return null;
        }

        FlicktekCommands.getInstance().onGestureChanged(0);

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (enter) {
                    if (gesture_number == GESTURE_ENTER)
                        FlicktekCommands.getInstance().startCalibration();
                    else
                        sendStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "Animation ended. GESTURE " + gesture_number + " Enter " + enter);
            }
        });
        return anim;
    }

    private void updateUi() {
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                switch (status) {
                    case FlicktekCommands.GESTURE_STATUS_RECORDING:
                        if (iv_splash.getVisibility() == View.INVISIBLE) {
                            iv_splash.setVisibility(View.INVISIBLE);
                            animate_icon(animation_success);
                        }
                        return;
                    case FlicktekCommands.GESTURE_STATUS_OK:
                        tv_status.setText("Repeat gesture!");
                        tv_title.setText("OK!");
                        animate_number();
                        animate_icon(animation_success);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_ERROR1:
                        tv_status.setText("Not similar");
                        tv_title.setText("Try again!");
                        animate_icon(animation_error);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_ERROR2:
                        tv_status.setText("Let's start again");
                        tv_title.setText("Try again...");
                        animate_icon(animation_error);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_OKREPETITION:
                        tv_status.setText("Repeat gesture!");
                        animate_number();

                        Random r = new Random();
                        int i = r.nextInt() % 3;
                        String greats;
                        switch (i) {
                            case 0:
                                greats = "Well done!";
                                break;
                            case 1:
                                greats = "Great!";
                                break;
                            default:
                                greats = "Looks good!";
                                break;
                        }

                        tv_title.setText(greats);
                        animate_icon(animation_success);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_OKGESTURE:
                        tv_status.setText("OK!");
                        tv_title.setText("Perfect!");
                        animate_icon(animation_success);
                        break;
                }

                sendStart();
            }
        });
    }

    public void close() {
        if (bCloseCalibration == true) {
            Log.d(TAG, "Calibration closed already");
            return;
        }

        bCloseCalibration = true;
        FlicktekManager.getInstance().setCalibrationMode(false);
        FlicktekCommands.getInstance().stopCalibration();

        // Pop until the start calibration message
        mainActivity.getFragmentManager().popBackStack("CalibrationFragment", POP_BACK_STACK_INCLUSIVE);
        TestAnimatedGestures fragment = new TestAnimatedGestures();
        mainActivity.showFragment(fragment, "TestFragment", true);
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

    private void sendStart() {
        FlicktekCommands.getInstance().writeGestureStatusStart();
    }

    @Subscribe
    public void onCalibrationWritten(FlicktekCommands.onCalibrationWritten _status) {
        Log.d(TAG, "onCalibrationWritten: " + gesture_number);

        status = _status.status;
        switch (_status.status) {
            case FlicktekCommands.STATUS_EXEC:
                break;
            case FlicktekCommands.STATUS_CALIB:
                sendStart();
                break;
        }
    }

    @Subscribe
    public void onGestureStatusNotify(FlicktekCommands.onGestureStatusEvent _status) {
        Log.d(TAG, "onGestureStatusNotify: " + _status.status.toString());
        status = _status.status;
        updateStatus();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGestureQuality(FlicktekCommands.onGestureQuality gestureEvent) {
        Log.d(TAG, "onGestureQuality arrived: ");
        if (mGLView != null)
            mGLView.setQuality(gestureEvent.quality);
    }

    @Subscribe
    public void onDeviceToWearEvent(DeviceToWearEvent event) {
        mainActivity.runOnUiThread(new Runnable() {
                                       public void run() {
                                           Toast.makeText(mainActivity, "Closing Clip connnection", Toast.LENGTH_SHORT).show();
                                       }
                                   }
        );
    }

    public void showNextFragment() {
        if (gesture_number == G_NUMB_MAX - 1) {
            Log.v(TAG, "Finished calibration");
            bFinishedCalibration = true;
            close();
            return;
        }

        final Fragment _fragment = newInstance(gesture_number + 1);
        Log.d(TAG, "showFragment: ");
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_left);
                    transaction.addToBackStack("CalibrationAnimated " + gesture_number);
                    transaction.replace(R.id.container, _fragment);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateStatus() {
        switch (status) {
            case FlicktekCommands.GESTURE_STATUS_OK:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OK");
                if (iteration < G_ITER_MAX) {
                    iteration++;
                } else {
                    showNextFragment();
                }

                break;
            case FlicktekCommands.GESTURE_STATUS_RECORDING:
                // FADEIN
                Log.d(TAG, "updateStatus: GESTURE_STATUS_RECORDING");
                break;
            case FlicktekCommands.GESTURE_STATUS_ERROR1:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_ERROR1");
                iteration = 2;
                break;
            case FlicktekCommands.GESTURE_STATUS_ERROR2:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_ERROR2");
                iteration = 1;
                break;
            case FlicktekCommands.GESTURE_STATUS_OKREPETITION:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKREPETITION");
                iteration++;
                break;
            case FlicktekCommands.GESTURE_STATUS_OKGESTURE:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKGESTURE");
                iteration = 1;
                showNextFragment();
                return;
            case FlicktekCommands.GESTURE_STATUS_OKCALIBRATION:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKCALIBRATION");
                bFinishedCalibration = true;
                close();
                return;
        }

        updateUi();
    }

    public void repeat() {
        FlicktekManager.getInstance().setCalibrationMode(false);
        FlicktekCommands.getInstance().stopCalibration();

        // Pop until the start calibration message
        mainActivity.getFragmentManager().popBackStack("CalibrationFragment", POP_BACK_STACK_INCLUSIVE);

        CalibrationFragmentScroll fragment = new CalibrationFragmentScroll();
        fragment.repeatCalibration(true);
        mainActivity.showFragment(fragment, "CalibrationFragment", true);
    }

    public void onClick(View _view) {
        Log.d(TAG, "onClick: ");

        if (_view == b_close) {
            close();
        }

        if (_view == b_repeat) {
            repeat();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.seekTo(0);
        mp.start();
    }
}
