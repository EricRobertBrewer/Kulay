package com.bunga.kulay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Random;

/**
 * A placeholder fragment containing a simple view.
 */
public class ColorFragment extends Fragment {

    private static final int MAX = 255; // See R.integer.color_value_max.

    public ColorFragment() {
    }

    public interface ColorChangedListener {
        /**
         * Will be called three times when {@link #setColor(int)} is called.
         * @param colorFragment fragment
         */
        void onColorChanged(ColorFragment colorFragment);
        void onColorRandomized(ColorFragment colorFragment);
    }

    private ColorChangedListener mListener;
    private boolean mShouldDoColorChangedCallback = true;
    private static final int COLOR_DIMENSIONS = 3;
    private Random mRandom = new Random();

    // Input
    private TextWatcher mInputTextWatchers[] = new TextWatcher[COLOR_DIMENSIONS];
    private EditText[] mInputs = new EditText[COLOR_DIMENSIONS];

    private GradientDrawable[] mGradients = new GradientDrawable[COLOR_DIMENSIONS];
    private int[][] mGradientColors = new int[COLOR_DIMENSIONS][];
    private SeekBar[] mSeekBars = new SeekBar[COLOR_DIMENSIONS];

    // Output
    private TextView mBackgroundLabelWhite;
    private TextView mBackgroundLabelGray;
    private TextView mBackgroundLabelDarkGray;
    private TextView mBackgroundLabelBlack;
    private TextView mTextLabelWhite;
    private TextView mTextLabelGray;
    private TextView mTextLabelDarkGray;
    private TextView mTextLabelBlack;

    public int getColor() {
        return getColorFromSeekBars();
    }

    public void setColor(int color) {
        mInputs[0].setText(String.valueOf(Color.red(color)));
        mInputs[1].setText(String.valueOf(Color.green(color)));
        mInputs[2].setText(String.valueOf(Color.blue(color)));
    }

    public void randomizeColor() {
        mShouldDoColorChangedCallback = false;
        setColor(Color.rgb(mRandom.nextInt(MAX +1), mRandom.nextInt(MAX +1), mRandom.nextInt(MAX +1)));
        mShouldDoColorChangedCallback = true;
        if (mListener != null) {
            mListener.onColorRandomized(this);
        }
    }

    public void disableInputs() {
        for (int i = 0; i < COLOR_DIMENSIONS; i++) {
            mInputs[i].setEnabled(false);
            mSeekBars[i].setEnabled(false);
        }
    }

    public void enableInputs() {
        for (int i = 0; i < COLOR_DIMENSIONS; i++) {
            mInputs[i].setEnabled(true);
            mSeekBars[i].setEnabled(true);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ColorChangedListener) {
            mListener = (ColorChangedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ColorChangedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_color, container, false);

        PercentRelativeLayout inputLayout = (PercentRelativeLayout) view.findViewById(R.id.color_input_layout);
        mInputs[0] = (EditText) inputLayout.findViewById(R.id.color_input_red);
        mInputs[1] = (EditText) inputLayout.findViewById(R.id.color_input_green);
        mInputs[2] = (EditText) inputLayout.findViewById(R.id.color_input_blue);
        mGradients[0] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.RED});
        mGradients[1] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.GREEN});
        mGradients[2] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.BLUE});
        mGradientColors[0] = new int[2];
        mGradientColors[1] = new int[2];
        mGradientColors[2] = new int[2];
        mSeekBars[0] = (SeekBar) view.findViewById(R.id.color_seekbar_red);
        mSeekBars[1] = (SeekBar) view.findViewById(R.id.color_seekbar_green);
        mSeekBars[2] = (SeekBar) view.findViewById(R.id.color_seekbar_blue);

        PercentRelativeLayout outputLayout = (PercentRelativeLayout) view.findViewById(R.id.color_output_layout);
        mBackgroundLabelWhite = (TextView) outputLayout.findViewById(R.id.color_background_label_white);
        mBackgroundLabelGray = (TextView) outputLayout.findViewById(R.id.color_background_label_gray);
        mBackgroundLabelDarkGray = (TextView) outputLayout.findViewById(R.id.color_background_label_dark_gray);
        mBackgroundLabelBlack = (TextView) outputLayout.findViewById(R.id.color_background_label_black);
        mTextLabelWhite = (TextView) outputLayout.findViewById(R.id.color_text_label_white);
        mTextLabelGray = (TextView) outputLayout.findViewById(R.id.color_text_label_gray);
        mTextLabelDarkGray = (TextView) outputLayout.findViewById(R.id.color_text_label_dark_gray);
        mTextLabelBlack = (TextView) outputLayout.findViewById(R.id.color_text_label_black);

        for (int i = 0; i < COLOR_DIMENSIONS; i++) {
            final int position = i;
            mSeekBars[i].setBackground(mGradients[i]);
            mSeekBars[i].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean isUser) {
                    if (isUser) {
                        mInputs[position].setText(String.valueOf(progress));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            mInputTextWatchers[i] = new TextWatcher() {
                @Override
                public void afterTextChanged(Editable s) {
                }

                @Override
                public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int start, int count, int after) {
                    try {
                        int progress = Integer.parseInt(charSequence.toString());
                        if (0 <= progress && progress <= MAX) {
                            mSeekBars[position].setProgress(progress);
                            for (int j = 0; j < COLOR_DIMENSIONS; j++) {
                                if (j != position) {
                                    setGradientColors(j);
                                    mGradients[j].setColors(mGradientColors[j]);
                                }
                            }
                            updateOutputs(getColorFromSeekBars());
                            if (mListener != null && mShouldDoColorChangedCallback) {
                                mListener.onColorChanged(ColorFragment.this);
                            }
                        } else {
                            mInputs[position].setText(String.valueOf(progress < 0 ? 0 : MAX));
                        }
                    } catch (NumberFormatException e) {
                        mInputs[position].setText("0");
                    }
                }
            };
            mInputs[i].addTextChangedListener(mInputTextWatchers[i]);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (int i = 0; i < COLOR_DIMENSIONS; i++) {
            mInputs[i].removeTextChangedListener(mInputTextWatchers[i]);
        }
    }

    private int getColorFromSeekBars() {
        return Color.rgb(mSeekBars[0].getProgress(), mSeekBars[1].getProgress(), mSeekBars[2].getProgress());
    }

    private void updateOutputs(int color) {
        mBackgroundLabelWhite.setBackgroundColor(color);
        mBackgroundLabelGray.setBackgroundColor(color);
        mBackgroundLabelDarkGray.setBackgroundColor(color);
        mBackgroundLabelBlack.setBackgroundColor(color);
        mTextLabelWhite.setTextColor(color);
        mTextLabelGray.setTextColor(color);
        mTextLabelDarkGray.setTextColor(color);
        mTextLabelBlack.setTextColor(color);
    }

    private void setGradientColors(int position) {
        mGradientColors[position][0] =
                Color.rgb(getGradientColorValue(position, 0, true),
                        getGradientColorValue(position, 1, true),
                        getGradientColorValue(position, 2, true));
        mGradientColors[position][1] =
                Color.rgb(getGradientColorValue(position, 0, false),
                        getGradientColorValue(position, 1, false),
                        getGradientColorValue(position, 2, false));
    }

    private int getGradientColorValue(int position, int dimension, boolean isLow) {
        return position == dimension ? (isLow ? 0 : MAX) : mSeekBars[dimension].getProgress();
    }
}
