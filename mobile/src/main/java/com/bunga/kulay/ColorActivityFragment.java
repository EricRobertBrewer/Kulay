package com.bunga.kulay;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * A placeholder fragment containing a simple view.
 */
public class ColorActivityFragment extends Fragment {

    public ColorActivityFragment() {
    }

    public interface ColorPickListener {
    }

    private ColorPickListener mListener;
    private static final int COLOR_DIMENSIONS = 3;
    private int mColorValueMax;
    private LinearLayout mInputLayout;
    private TextWatcher mInputTextWatchers[] = new TextWatcher[COLOR_DIMENSIONS];
    private EditText[] mInputs = new EditText[COLOR_DIMENSIONS];
    private GradientDrawable[] mGradients = new GradientDrawable[COLOR_DIMENSIONS];
    private SeekBar[] mSeekBars = new SeekBar[COLOR_DIMENSIONS];
    private LinearLayout mBackgroundLayout;
    private TextView mLabelWhite;
    private TextView mLabelGray;
    private TextView mLabelBlack;
    private int mColor;

    public int getPickedColor() {
        return mColor;
    }

    public void toggleShowInput(boolean isShown) {
        mInputLayout.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    public void setColor(int color) {
        mInputs[0].setText(String.valueOf(Color.red(color)));
        mInputs[1].setText(String.valueOf(Color.green(color)));
        mInputs[2].setText(String.valueOf(Color.blue(color)));
        setColor();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ColorPickListener) {
            mListener = (ColorPickListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ColorListener");
        }
        mColorValueMax = context.getResources().getInteger(R.integer.color_value_max);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_color, container, false);
        mInputLayout = (LinearLayout) view.findViewById(R.id.color_input_layout);
        mInputs[0] = (EditText) mInputLayout.findViewById(R.id.color_input_red);
        mInputs[1] = (EditText) mInputLayout.findViewById(R.id.color_input_green);
        mInputs[2] = (EditText) mInputLayout.findViewById(R.id.color_input_blue);
        mGradients[0] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.RED});
        mGradients[1] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.GREEN});
        mGradients[2] = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {Color.BLACK, Color.BLUE});
        mSeekBars[0] = (SeekBar) view.findViewById(R.id.color_seekbar_red);
        mSeekBars[1] = (SeekBar) view.findViewById(R.id.color_seekbar_green);
        mSeekBars[2] = (SeekBar) view.findViewById(R.id.color_seekbar_blue);
        LinearLayout layout = (LinearLayout) view.findViewById(R.id.color_layout);
        mBackgroundLayout = (LinearLayout) layout.findViewById(R.id.color_background_layout);
        LinearLayout textLayout = (LinearLayout) layout.findViewById(R.id.color_text_layout);
        mLabelWhite = (TextView) textLayout.findViewById(R.id.color_text_label_white);
        mLabelGray = (TextView) textLayout.findViewById(R.id.color_text_label_gray);
        mLabelBlack = (TextView) textLayout.findViewById(R.id.color_text_label_black);
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
                        if (0 <= progress && progress <= mColorValueMax) {
                            mSeekBars[position].setProgress(progress);
                            for (int j = 0; j < COLOR_DIMENSIONS; j++) {
                                if (j != position) {
                                    mGradients[j].setColors(getGradientColors(j));
                                }
                            }
                            setColor();
                        }
                    } catch (NumberFormatException e) {
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

    private void setColor() {
        mColor = Color.rgb(mSeekBars[0].getProgress(), mSeekBars[1].getProgress(), mSeekBars[2].getProgress());
        mBackgroundLayout.setBackgroundColor(mColor);
        mLabelWhite.setTextColor(mColor);
        mLabelGray.setTextColor(mColor);
        mLabelBlack.setTextColor(mColor);
    }

    private int[] getGradientColors(int position) {
        return new int[] {
                Color.rgb(getGradientColorValue(position, 0, true),
                        getGradientColorValue(position, 1, true),
                        getGradientColorValue(position, 2, true)),
                Color.rgb(getGradientColorValue(position, 0, false),
                        getGradientColorValue(position, 1, false),
                        getGradientColorValue(position, 2, false))
        };
    }

    private int getGradientColorValue(int position, int dimension, boolean isLow) {
        return position == dimension ? (isLow ? 0 : 255) : mSeekBars[dimension].getProgress();
    }
}
