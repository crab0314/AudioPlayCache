package com.yaya.exoplayertest;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Administrator on 2018/4/3.
 * 邮箱：jiaqipang@bupt.edu.cn
 * package_name com.yaya.exoplayertest
 * project_name ExoplayerTest
 * 功能：
 */
public class CustomToolBar extends LinearLayout {

    private Boolean isLeftBtnVisible;
    private int leftResId;

    private Boolean isLeftTvVisible;
    private String leftTvText;

    private Boolean isRightBtnVisible;
    private int rightResId;

    private Boolean isRightTvVisible;
    private String rightTvText;

    private Boolean isTitleVisible;
    private String titleText;

    private int backgroundResId;

    Button leftBtn;
    Button rightBtn;


    public CustomToolBar(Context context) {
        this(context, null);
    }

    public CustomToolBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomToolBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(attrs);
    }

    /**
     * 初始化属性
     *
     * @param attrs
     */
    public void initView(AttributeSet attrs) {

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.CustomToolBar);
        /**-------------获取左边按钮属性------------*/
        isLeftBtnVisible = typedArray.getBoolean(R.styleable.CustomToolBar_left_btn_visible, false);
        leftResId = typedArray.getResourceId(R.styleable.CustomToolBar_left_btn_src, -1);
        /**-------------获取左边文本属性------------*/
        isLeftTvVisible = typedArray.getBoolean(R.styleable.CustomToolBar_left_tv_visible, false);
        if (typedArray.hasValue(R.styleable.CustomToolBar_left_tv_text)) {
            leftTvText = typedArray.getString(R.styleable.CustomToolBar_left_tv_text);
        }
        /**-------------获取右边按钮属性------------*/
        isRightBtnVisible = typedArray.getBoolean(R.styleable.CustomToolBar_right_btn_visible, false);
        rightResId = typedArray.getResourceId(R.styleable.CustomToolBar_right_btn_src, -1);
        /**-------------获取右边文本属性------------*/
        isRightTvVisible = typedArray.getBoolean(R.styleable.CustomToolBar_right_tv_visible, false);
        if (typedArray.hasValue(R.styleable.CustomToolBar_right_tv_text)) {
            rightTvText = typedArray.getString(R.styleable.CustomToolBar_right_tv_text);
        }
        /**-------------获取标题属性------------*/
        isTitleVisible = typedArray.getBoolean(R.styleable.CustomToolBar_title_visible, false);
        if (typedArray.hasValue(R.styleable.CustomToolBar_title_text)) {
            titleText = typedArray.getString(R.styleable.CustomToolBar_title_text);
        }
        /**-------------背景颜色------------*/
        backgroundResId = typedArray.getResourceId(R.styleable.CustomToolBar_barBackground, -1);

        typedArray.recycle();

        /**-------------设置内容------------*/
        View barLayoutView = View.inflate(getContext(), R.layout.layout_common_toolbar, null);
        leftBtn = (Button) barLayoutView.findViewById(R.id.toolbar_left_btn);
        TextView leftTv = (TextView) barLayoutView.findViewById(R.id.toolbar_left_tv);
        TextView titleTv = (TextView) barLayoutView.findViewById(R.id.toolbar_title_tv);
        rightBtn = (Button) barLayoutView.findViewById(R.id.toolbar_right_btn);
        TextView rightTv = (TextView) barLayoutView.findViewById(R.id.toolbar_right_tv);
        RelativeLayout barRlyt = (RelativeLayout) barLayoutView.findViewById(R.id.toolbar_content_rlyt);

        if (isLeftBtnVisible) {
            leftBtn.setVisibility(VISIBLE);
        }
        if (isLeftTvVisible) {
            leftTv.setVisibility(VISIBLE);
        }
        if (isRightBtnVisible) {
            rightBtn.setVisibility(VISIBLE);
        }
        if (isRightTvVisible) {
            rightTv.setVisibility(VISIBLE);
        }
        if (isTitleVisible) {
            titleTv.setVisibility(VISIBLE);
        }
        leftTv.setText(leftTvText);
        rightTv.setText(rightTvText);
        titleTv.setText(titleText);

        if (leftResId != -1) {
            leftBtn.setBackgroundResource(leftResId);
        }
        if (rightResId != -1) {
            rightBtn.setBackgroundResource(rightResId);
        }
        if (backgroundResId != -1) {
            barRlyt.setBackgroundColor(getResources().getColor(R.color.main_color));
        }
        //将设置完成之后的View添加到此LinearLayout中
        addView(barLayoutView, 0);
    }

    public void setLeftClickListener(OnClickListener clickListener) {
        if (leftBtn != null) {
            leftBtn.setOnClickListener(clickListener);
        } else {
            Log.v("click", "左键暂未初始化");
        }
    }

    public void setRightClickListener(OnClickListener clickListener){
        if (rightBtn != null) {
            rightBtn.setOnClickListener(clickListener);
        } else {
            Log.v("click", "右键暂未初始化");
        }
    }


}
