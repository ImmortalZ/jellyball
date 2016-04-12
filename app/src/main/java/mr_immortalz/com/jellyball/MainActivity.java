package mr_immortalz.com.jellyball;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import mr_immortalz.com.jellyball.custom.JellyBall;
import mr_immortalz.com.jellyball.custom.PullScrollView;

public class MainActivity extends Activity implements PullScrollView.OnTurnListener {
    private PullScrollView mScrollView;
    private ImageView mHeadImg;

    private TableLayout mMainLayout;
    private JellyBall jellyBall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_pull_down);
        initView();
        showTable();
    }

    protected void initView() {
        mScrollView = (PullScrollView) findViewById(R.id.scroll_view);
        mHeadImg = (ImageView) findViewById(R.id.background_img);

        mMainLayout = (TableLayout) findViewById(R.id.table_layout);
        jellyBall = (JellyBall) findViewById(R.id.cirle);
        mScrollView.setHeader(mHeadImg);
        mScrollView.setOnTurnListener(this);
    }

    public void showTable() {
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        layoutParams.leftMargin = 30;
        layoutParams.bottomMargin = 10;
        layoutParams.topMargin = 10;

        for (int i = 0; i < 30; i++) {
            TableRow tableRow = new TableRow(this);
            TextView textView = new TextView(this);
            textView.setText("Test pull down scroll view " + i);
            textView.setTextSize(20);
            textView.setPadding(15, 15, 15, 15);

            tableRow.addView(textView, layoutParams);
            if (i % 2 != 0) {
                tableRow.setBackgroundColor(Color.LTGRAY);
            } else {
                tableRow.setBackgroundColor(Color.WHITE);
            }

            final int n = i;
            tableRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this, "Click item " + n, Toast.LENGTH_SHORT).show();
                }
            });

            mMainLayout.addView(tableRow);
        }
    }

    /**
     * 下拉
     *
     * @param y
     */
    @Override
    public void onPull(float y) {
        jellyBall.setPullHeight(y);
    }

    /**
     * 松手
     *
     * @param y
     */
    @Override
    public void onUp(float y) {
        jellyBall.setUpHeight(y);
    }

    @Override
    public void onRefresh() {
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //小球回弹停止一段时候后才开始复位
                jellyBall.stopRefresh();
                mScrollView.stopRefresh();
            }
        }, 500);*/
        jellyBall.beginStopRefresh();
        mScrollView.stopRefresh();
    }


}
