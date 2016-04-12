package mr_immortalz.com.jellyball.custom;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import mr_immortalz.com.jellyball.R;


/**
 * Created by Mr_immortalZ on 2016/4/10.
 * email : mr_immortalz@qq.com
 */
public class JellyBall extends View {

    private int mWidth;//宽度
    private int mHeight;//高度
    private Rect mRect = new Rect();//用来记录jellyball滑动到PULL_MAX时位置
    //圆
    private float circleStartX;
    private float circleStartY;
    private float radius = 24;
    private float blackMagic = 0.551915024494f;
    private float c;
    private float rebounceY;//竖直方向小球最多回弹距离
    private float rebounceX;//水平方向小球最多回弹距离
    //控制点
    private VPoint p1, p3;//垂直点
    private HPoint p2, p4;//水平点
    //线
    private Path mLinePath;
    private float lineStartY = 0f;
    private float lineWidth;//线宽度
    //回弹
    private float rebounceInterpolatedTime;
    private RebounceAnim rebounceAnim;
    //下拉
    private float pullProgress;//下拉进度(0 ~ 1)
    private float PULL_MAX;
    //private static float PULL_NOT_MOVE = 50;
    private float MOVE_DISTANCE;
    private boolean isFirstPull = true;
    private boolean isFirstUp = true;
    private boolean moveEnd = false;//位移属性动画是否结束
    private boolean isPullOver = false;//滑动到PULL_MAX后，继续滑动
    private float pullOverDistance;//滑动过PULL_MAX后，超出距离

    private Path mPath;
    private Paint mCirclePaint;
    private Paint mLinePaint;
    private Type mType = Type.NORMAL;


    public enum Type {
        NORMAL, //最开始
        PULL, //下拉
        REBOUNCE_DOWN, //下拉后小球回弹
        REBOUNCE_UP,//复位后小球回弹
        UP,//复位
        REFRESHING_START, //刷新开始
        REFRESH_END,//刷新结束
    }

    public JellyBall(Context context) {
        this(context, null);
    }

    public JellyBall(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JellyBall(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化基本信息
     */
    private void init() {
        mCirclePaint = new Paint();
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setColor(getResources().getColor(R.color.red));

        mLinePaint = new Paint();
        mLinePaint.setStyle(Paint.Style.FILL);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setColor(getResources().getColor(R.color.alph_red));

        p1 = new VPoint();
        p3 = new VPoint();
        p2 = new HPoint();
        p4 = new HPoint();

        mPath = new Path();
        mLinePath = new Path();

        PULL_MAX = getResources().getDimension(R.dimen.jellyball_pullmax);
        MOVE_DISTANCE = getResources().getDimension(R.dimen.jellyball_move_distance);

    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        rebounceY = radius / 2;
        rebounceX = radius;
        circleStartX = (mWidth - 2 * radius) / 2;
        circleStartY = rebounceY;

        c = radius * blackMagic;

        lineStartY = circleStartY;
    }
    /**
     * 根据下拉y值 更新状态
     */
    public void setPullHeight(final float y) {
        if (y <= PULL_MAX) {
            pullProgress = y / PULL_MAX;
            //下拉时mType是NORMAL，所以这里需要设置mType
            setType(Type.PULL);
        } else if ((y - PULL_MAX) >= 0) {
            //因为下拉时这个方法会一直调用，而我们的位移属性动画只需一次就好，这里用isFirstPull来控制
            if (isFirstPull) {
                isFirstPull = false;
                isFirstUp = true;
                ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(this, "translationY", MOVE_DISTANCE);
                objectAnimator.setDuration(10);
                objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                objectAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setType(Type.REBOUNCE_DOWN);//移动完成后，设置mType = REBOUNCE_DOWN
                        if (mRect.isEmpty()) {
                            mRect.set(getLeft(), getTop(), getRight(), getBottom());//记录刚开始滑动距离大于PULL_MAX时，jellyball位置
                        }
                        moveEnd = true;//移动完成
                    }
                });
                objectAnimator.start();
            } else {
                if (moveEnd) {//移动完成时，如果继续下拉，则jellyball与竖线同时移动，这里用layout方法来移动
                    isPullOver = true;
                    layout(mRect.left, (int) (mRect.top + (y - PULL_MAX) * 2), mRect.right, (int) (mRect.bottom + (y - PULL_MAX) * 2));
                    pullOverDistance = (y - PULL_MAX) * 2;//记录layout移动的距离，在setUpHeight中会使用这个变量来获取复位的距离
                    //LogUtil.m("pullOverDistance " + pullOverDistance);
                }
            }

        }
    }

    /**
     * 根据复位y值更新状态
     * @param y
     */
    public void setUpHeight(float y) {
        //松手后根据mType来判断松手前下拉的状态
        if (mType == Type.PULL) {//说明下拉距离并没有大于PULL_MAX，所以mType 仍然是 Type.PULL
            pullProgress = y / PULL_MAX;
            invalidate();
            if (y == 0) {
                setType(JellyBall.Type.NORMAL);
            }
        } else if (mType == Type.REFRESHING_START) { //说明下拉的距离大于PULL_MAX，复位且开启小球回弹抖动的果冻效果
            setType(Type.UP);
            if (isFirstUp) {//复位时这个方法会一直调用，而我们的位移属性动画只需一次就好，这里用isFirstUp来控制
                isFirstUp = false;
                isFirstPull = true;
                ObjectAnimator objectAnimator;
                if (isPullOver) {//下拉距离超过了PULL_MAX，则位移属性动画移动到的位置应该是pullOverDistance，否则为0
                    objectAnimator = ObjectAnimator.ofFloat(this, "translationY", -pullOverDistance);
                } else {
                    objectAnimator = ObjectAnimator.ofFloat(this, "translationY", 0);
                }
                objectAnimator.setDuration(150);
                objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
                objectAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setType(Type.REBOUNCE_UP);//移动结束，开启小球回弹抖动的果冻效果
                    }
                });
                objectAnimator.start();
            }
        }
    }



    /**
     * 小球最开始状态
     */
    private void circleModel0() {
        p1.setY(radius);
        p3.setY(-radius);
        p1.x = p3.x = 0;
        p1.left.x = p3.left.x = -c;
        p1.right.x = p3.right.x = c;


        p2.setX(radius);
        p4.setX(-radius);
        p2.y = p4.y = 0;
        p2.top.y = p4.top.y = -c;
        p2.bottom.y = p4.bottom.y = c;

    }

    /**
     * 竖线 最开始状态
     */
    private void lineModel0() {
        lineStartY = circleStartY - 1;
        lineWidth = getResources().getDimension(R.dimen.jellyball_line_width);
    }

    /**
     * 竖线 根据progress更新宽度
     * @param progress
     */
    private void lineModel1(float progress) {
        lineModel0();
        lineWidth = lineWidth * (1.0f - progress + 0.3f) * 0.8f;
    }

    /**
     * 小球，竖线 根据progress跟新 p1 点 和竖线Y的高度
     * @param progress
     */
    private void circleModel1(float progress) {
        circleModel0();

        p1.setY(p1.y + radius * 1.5f * progress);

        lineStartY += radius * 1.5f * progress;
    }
    /**
     * 小球，竖线 根据progress跟新 p1 点 、水平方向点宽度变小 和竖线Y的高度
     * @param progress
     */
    private void circleModel2(float progress) {
        circleModel1(0.8f);
        p2.adjustAllX(-radius * (progress - 0.8f) * 0.4f);
        p4.adjustAllX(radius * (progress - 0.8f) * 0.4f);
        p1.setY(p1.y + radius * 3f * (progress - 0.8f));
        //线
        lineStartY += radius * 3f * (progress - 0.8f);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        mPath.reset();
        mLinePath.reset();
        canvas.translate(circleStartX + radius, circleStartY + radius);
        switch (mType){//根据mType类型进行 坐标运算
            case NORMAL:
                circleModel0();
                lineModel0();
                break;
            case PULL:
                if (pullProgress >= 0 && pullProgress <= 0.8f) {
                    circleModel1(pullProgress);
                } else if (pullProgress > 0.8f && pullProgress <= 1.0f) {
                    circleModel2(pullProgress);
                }
                lineModel1(pullProgress);
                break;
            case REBOUNCE_DOWN:
                rebounceAction();
                break;
            case REFRESHING_START:
                circleModel0();
                break;
            case UP:
                circleModel0();
                break;
            case REBOUNCE_UP:
                rebounceAction();
                break;
            case REFRESH_END:
                setType(Type.NORMAL);
                break;
            default:
                break;
        }
        //根据所得坐标 进行绘制
        mPath.moveTo(p1.x, p1.y);
        //三次贝塞尔曲线
        mPath.cubicTo(p1.right.x, p1.right.y, p2.bottom.x, p2.bottom.y, p2.x, p2.y);
        mPath.cubicTo(p2.top.x, p2.top.y, p3.right.x, p3.right.y, p3.x, p3.y);
        mPath.cubicTo(p3.left.x, p3.left.y, p4.top.x, p4.top.y, p4.x, p4.y);
        mPath.cubicTo(p4.bottom.x, p4.bottom.y, p1.left.x, p1.left.y, p1.x, p1.y);
        canvas.drawPath(mPath, mCirclePaint);
        canvas.save();
        //绘制竖线
        mLinePath.moveTo(-lineWidth / 2, lineStartY);
        mLinePath.lineTo(lineWidth / 2, lineStartY);
        mLinePath.lineTo(getResources().getDimension(R.dimen.jellyball_line_width) / 2, mHeight);
        mLinePath.lineTo(-getResources().getDimension(R.dimen.jellyball_line_width) / 2, mHeight);
        mLinePath.close();
        canvas.drawPath(mLinePath, mLinePaint);
        canvas.restore();
    }

    private void rebounceAction() {
        circleModel0();
        lineModel0();
        p2.adjustAllX(getRebounceHorizontalX(rebounceInterpolatedTime));
        p4.adjustAllX(-getRebounceHorizontalX(rebounceInterpolatedTime));
        p2.adjustAllBottomY(getRebounceHorizontalBottomY(rebounceInterpolatedTime));
        p4.adjustAllBottomY(getRebounceHorizontalBottomY(rebounceInterpolatedTime));
        p3.adjustAllY(getRebounceVerticalPointY(rebounceInterpolatedTime));
        p1.adjustBottomX(getRebounceVerticalPointY(rebounceInterpolatedTime));
    }


    private class VPoint {
        private float x;
        private float y;
        private PointF left;
        private PointF right;

        public VPoint() {
            left = new PointF();
            right = new PointF();
        }

        public void setY(float y) {
            this.y = y;
            left.y = right.y = y;
        }

        public void adjustAllY(float offset) {
            this.y += offset;
            this.left.y += offset;
            this.right.y += offset;
        }

        public void adjustBottomX(float offset) {
            this.left.x -= offset;
            this.right.x += offset;
        }
    }

    private class HPoint {
        private float x;
        private float y;
        private PointF top;
        private PointF bottom;

        public HPoint() {
            top = new PointF();
            bottom = new PointF();
        }

        public void setX(float x) {
            this.x = x;
            top.x = bottom.x = x;
        }

        public void adjustAllX(float offset) {
            this.x += offset;
            this.top.x += offset;
            this.bottom.x += offset;
        }

        public void adjustAllBottomY(float offset) {
            this.y += offset;
            this.bottom.y += offset;
            this.top.y += offset;
        }

        public void adjustAllY(float offset) {
            this.y += offset;
            this.top.y += offset;
            this.bottom.y += offset;
        }
    }

    private void setType(Type type) {
        this.mType = type;
        switch (type) {
            case REBOUNCE_DOWN:
                startRebounceAnim(Type.REFRESHING_START);
                break;
            case REBOUNCE_UP:
                startRebounceAnim(Type.REFRESH_END);
                break;
            default:
                break;
        }
        invalidate();
    }

    /**
     * 回弹动画，主要为了根据回弹时间流失比得到回弹大小
     */
    private class RebounceAnim extends Animation {
        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            super.applyTransformation(interpolatedTime, t);
            rebounceInterpolatedTime = interpolatedTime;
            invalidate();
        }
    }

    /**
     * 开始回弹动画
     * @param type 动画结束后需要设置的状态
     */
    private void startRebounceAnim(final Type type) {
        rebounceAnim = new RebounceAnim();
        rebounceAnim.setDuration(500);
        rebounceAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setType(type);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        startAnimation(rebounceAnim);
    }

    public void beginStopRefresh() {
        stopRebounceAnim();
        setType(Type.REFRESHING_START);
    }

    /**
     * 停止回弹动画
     */
    private void stopRebounceAnim() {
        //LogUtil.m("type " + mType);
        if (rebounceAnim != null) {
            clearAnimation();
        }
    }

    /**
     * 得到水平方向点X回弹距离
     *
     * @return
     */
    private float getRebounceHorizontalX(float x) {
        return (float) ((1 - Math.exp(-2 * (x + 0.052)) * Math.cos(20 * (x + 0.052))) - 1) * rebounceX / 3 * 2;
    }

    /**
     * 得到水平方向点所拥有的底部点的Y回弹距离
     * @param y
     * @return
     */
    private float getRebounceHorizontalBottomY(float y) {
        return (float) ((1 - Math.exp(-2 * (y + 0.052)) * Math.cos(20 * (y + 0.052))) - 1) * rebounceY / 2;
    }

    /**
     * 得到竖直方向点Y回弹距离
     * @param y
     * @return
     */
    private float getRebounceVerticalPointY(float y) {
        return (float) ((1 - Math.exp(-2 * (y + 0.052)) * Math.cos(20 * (y + 0.052))) - 1) * rebounceY;
    }


}
