/*
 * Copyright(c) 2018 Bob Shen <ayst.shen@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ayst.stresstest.test;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.StringRes;

import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.orhanobut.logger.Logger;
import com.ayst.stresstest.R;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BaseTestFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BaseTestFragment extends Fragment {
    protected String TAG = "BaseTestFragment";

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    protected static final int MSG_UPDATE = 1;

    protected ProgressBar mProgressbar;
    protected TextView mTitleTv;
    protected TextView mCountTv;
    protected TextView mFailureCountTv;
    protected FrameLayout mTitleContainer;
    protected RelativeLayout mContentContainer;
    protected Button mStartBtn;
    protected ImageView mLogoIv;
    protected RelativeLayout mFullContainer;

    protected Activity mActivity;

    protected TestType mType = TestType.TYPE_CPU_TEST;

    public static final int STATE_RUNNING = 1;
    public static final int STATE_STOP = 2;
    protected int mState = STATE_STOP;

    protected static final int COUNT_TYPE_COUNT = 1;
    protected static final int COUNT_TYPE_TIME = 2;
    protected static final int COUNT_TYPE_NONE = 3;
    protected int mCountType = COUNT_TYPE_COUNT;

    protected int mMaxTestCount = 0;
    protected int mMaxTestTime = 0;
    protected int mCurrentCount = 0;
    protected int mFailureCount = 0;
    protected int mCurrentTime = 0;
    private Timer mCountTimer;

    protected int mFailThreshold = 1;
    protected int mPoorThreshold = 3;

    private boolean isEnable = true;

    protected OnFragmentInteractionListener mListener;

    protected enum RESULT {
        GOOD,
        FAIL,
        POOR,
        CANCEL
    }
    protected RESULT mResult = RESULT.GOOD;
    protected HashMap<RESULT, ClipDrawable> mResultDrawable = new HashMap<>();
    protected static HashMap<RESULT, String> sResultStringMap = new HashMap<>();

    static {
        sResultStringMap.put(RESULT.GOOD, "GOOD");
        sResultStringMap.put(RESULT.FAIL, "FAIL");
        sResultStringMap.put(RESULT.POOR, "POOR");
        sResultStringMap.put(RESULT.CANCEL, "CANCEL");
    }

    public BaseTestFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment BaseTestFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static BaseTestFragment newInstance(String param1, String param2) {
        BaseTestFragment fragment = new BaseTestFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getSimpleName();

        mActivity = this.getActivity();

        mResultDrawable.put(RESULT.GOOD, new ClipDrawable(new ColorDrawable(Color.GREEN), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(RESULT.FAIL, new ClipDrawable(new ColorDrawable(Color.YELLOW), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(RESULT.POOR, new ClipDrawable(new ColorDrawable(Color.RED), Gravity.LEFT, ClipDrawable.HORIZONTAL));
        mResultDrawable.put(RESULT.CANCEL, new ClipDrawable(new ColorDrawable(Color.GRAY), Gravity.LEFT, ClipDrawable.HORIZONTAL));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_base_test, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mProgressbar = (ProgressBar) view.findViewById(R.id.progressbar);
        mTitleTv = (TextView) view.findViewById(R.id.tv_title);
        mCountTv = (TextView) view.findViewById(R.id.tv_count);
        mFailureCountTv = (TextView) view.findViewById(R.id.tv_failure_count);
        mTitleContainer = (FrameLayout) view.findViewById(R.id.container_title);
        mContentContainer = (RelativeLayout) view.findViewById(R.id.container);
        mStartBtn = (Button) view.findViewById(R.id.btn_start);
        mLogoIv = (ImageView) view.findViewById(R.id.iv_logo);
        mFullContainer = (RelativeLayout) view.findViewById(R.id.container_full);

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartClicked();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        updateImpl();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (isRunning()) {
            stop();
        }
    }

    protected void updateImpl() {
        // Updating test count.
        if (mCountType == COUNT_TYPE_COUNT) {
            if (mMaxTestCount > 0) {
                mCountTv.setText(mCurrentCount + "/" + mMaxTestCount);
                mCountTv.setVisibility(View.VISIBLE);

                mFailureCountTv.setText(mFailureCount + "/" + mCurrentCount);
                mFailureCountTv.setVisibility(View.VISIBLE);
            } else {
                mCountTv.setVisibility(View.GONE);
                mFailureCountTv.setVisibility(View.GONE);
            }
        } else if (mCountType == COUNT_TYPE_TIME) {
            if (mMaxTestTime > 0) {
                int curHour = mCurrentTime / 3600;
                int curMin = (mCurrentTime % 3600) / 60;
                int curSec = (mCurrentTime % 3600) % 60;
                mCountTv.setText(curHour + ":" + curMin + ":" + curSec + "/" + mMaxTestTime + ":0:0");
                mCountTv.setVisibility(View.VISIBLE);
            } else {
                mCountTv.setVisibility(View.GONE);
            }
        } else {
            mCountTv.setVisibility(View.GONE);
            mFailureCountTv.setVisibility(View.GONE);
        }

        // Updating progressbar.
        mProgressbar.setVisibility(isEnable() ? View.VISIBLE : View.INVISIBLE);
        if (isRunning()) {
            if (mProgressbar.getProgressDrawable() != mResultDrawable.get(mResult)) {
                mProgressbar.setProgressDrawable(mResultDrawable.get(mResult));
            }
            mProgressbar.setProgress((mCountType == COUNT_TYPE_COUNT) ? (mCurrentCount * 100) / mMaxTestCount : (mCurrentTime * 100) / (mMaxTestTime * 3600));
        }

        // Updating other.
        mStartBtn.setEnabled(isEnable());
        if (isRunning()) {
            mStartBtn.setText(R.string.stop);
            mStartBtn.setSelected(true);
            mLogoIv.setVisibility(View.VISIBLE);
        } else {
            mStartBtn.setText(R.string.start);
            mStartBtn.setSelected(false);
            mLogoIv.setVisibility(View.INVISIBLE);
        }
    }

    protected void update() {
        mHandler.sendEmptyMessage(MSG_UPDATE);
    }

    protected void onStartClicked() {
        if (isRunning()) {
            showStopDialog();
        } else {
            if (mCountType == COUNT_TYPE_NONE) {
                start();
            } else {
                showSetMaxDialog();
            }
        }
    }

    protected void setTitle(String text) {
        mTitleTv.setText(text);
    }

    protected void setTitle(@StringRes int textId) {
        mTitleTv.setText(textId);
    }

    protected void setContentView(View contentView) {
        mContentContainer.addView(contentView);
    }

    protected void setFullContentView(View contentView) {
        mFullContainer.addView(contentView);
    }

    protected void setCountType(int type) {
        mCountType = type;
    }

    protected void setType(TestType type) {
        mType = type;
    }

    protected void setThreshold(int fail, int poor) {
        if (poor > fail) {
            mFailThreshold = fail;
            mPoorThreshold = poor;
        } else {
            Log.w(TAG, "setThreshold, Poor threshold must be greater than the Fail threshold.");
        }
    }

    public void start() {
        Logger.t(TAG).d("Start %s", mCountType == COUNT_TYPE_COUNT ? mMaxTestCount : mMaxTestTime + "h");

        mState = STATE_RUNNING;
        mResult = RESULT.GOOD;
        mCurrentCount = 0;
        mFailureCount = 0;
        mCurrentTime = 0;

        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, STATE_RUNNING);
                }
            });
        }

        if (mCountType == COUNT_TYPE_TIME) {
            mCountTimer = new Timer();
            mCountTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!isRunning() || (mMaxTestTime != 0 && mCurrentTime >= mMaxTestTime * 3600)) {
                        Log.d(TAG, "CountTimer, " + TAG + " test finish!");
                        mResult = RESULT.GOOD;
                        stop();
                    } else {
                        mCurrentTime++;
                        update();
                    }
                }
            }, 1000, 1000);
        }

        update();
    }

    public void stop() {
        String message;
        if (mCountType == COUNT_TYPE_COUNT) {
            message = mCurrentCount + "/" + mMaxTestCount;
        } else {
            int curHour = mCurrentTime / 3600;
            int curMin = (mCurrentTime % 3600) / 60;
            int curSec = (mCurrentTime % 3600) % 60;
            message = curHour + ":" + curMin + ":" + curSec + "/" + mMaxTestTime + ":0:0";
        }
        Logger.t(TAG).d("Stop %s %s", message, sResultStringMap.get(mResult));

        mState = STATE_STOP;
        if (mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onFragmentInteraction(mType, STATE_STOP);
                }
            });
        }
        if (mCountTimer != null) {
            mCountTimer.cancel();
        }
        int delay = (mType == TestType.TYPE_MEMORY_TEST) ? 2000 : 0;
        mHandler.postAtTime(new Runnable() {
            @Override
            public void run() {
                update();
            }
        }, delay);
    }

    protected void incCurrentCount() {
        mCurrentCount++;
        Logger.t(TAG).d("Testing %d/%d", mCurrentCount, mMaxTestCount);
    }

    protected void incFailureCount() {
        mFailureCount++;
        if (mFailureCount >= mPoorThreshold) {
            mResult = RESULT.POOR;
        } else if (mFailureCount >= mFailThreshold) {
            mResult = RESULT.FAIL;
        } else {
            mResult = RESULT.GOOD;
        }
    }

    public boolean isRunning() {
        return mState == STATE_RUNNING;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
        update();
    }

    public boolean isEnable() {
        return isEnable;
    }

    @SuppressLint("HandlerLeak")
    protected Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE:
                    updateImpl();
                    break;

                default:
                    handleMsg(msg);
                    break;
            }
        }
    };

    protected void handleMsg(Message msg) {

    }

    public void showSetMaxDialog() {
        final String title = mCountType == COUNT_TYPE_COUNT ? getString(R.string.set_time_tips) : getString(R.string.set_duration_tips);
        final EditText editText = new EditText(this.getActivity());
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this.getActivity())
                .setTitle(title)
                .setView(editText)
                .setPositiveButton(R.string.start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!editText.getText().toString().trim().equals("")) {
                            if (mCountType == COUNT_TYPE_COUNT) {
                                mMaxTestCount = Integer.valueOf(editText.getText().toString());
                            } else {
                                mMaxTestTime = Integer.valueOf(editText.getText().toString());
                            }
                            start();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    public void showStopDialog() {
        new AlertDialog.Builder(this.getActivity())
                .setMessage(R.string.stop_test_tips)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mResult = RESULT.CANCEL;
                        stop();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * A {@link Handler} for showing {@link Toast}s on the UI thread.
     */
    private final Handler mMessageHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mActivity, (String) msg.obj, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show.
     */
    protected void showToast(String text) {
        // We show a Toast by sending request message to mMessageHandler. This makes sure that the
        // Toast is shown on the UI thread.
        Message message = Message.obtain();
        message.obj = text;
        mMessageHandler.sendMessage(message);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(TestType testType, int state);
    }
}
