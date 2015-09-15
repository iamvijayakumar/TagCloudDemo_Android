package com.vj.tagcloud;

/**
 * Created by AmrutB on 25-06-2015.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.OverScroller;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class CopyOfTagCloudView extends RelativeLayout {
    RelativeLayout navigation_bar;
    TextView mTextView1;
    TagCloudType tagCloudType = TagCloudType.SPHERE;
  //  private  Scroller scroller;
    private String TAG = "TagCloudView";

    OverScroller mScroller ;
    private GestureDetector  gestureDetector;
    
    
 // variable keeping track of scroll/fling offset
    private int              offset     = 0;
    
    public CopyOfTagCloudView(Context mContext, int width, int height, List<Tag> tagList) {
        this(mContext, width, height, tagList, TagCloud.TEXT_SIZE_MIN, TagCloud.TEXT_SIZE_MAX, 1); // default for min/max
        // text size
    }

    public CopyOfTagCloudView(Context mContext, int width, int height, List<Tag> tagList, int textSizeMin, int textSizeMax, int scrollSpeed) {

        super(mContext);
        this.mContext = mContext;
        this.textSizeMin = textSizeMin;
        this.textSizeMax = textSizeMax;
        mScroller = new OverScroller(mContext);
        tspeed = scrollSpeed;
        x = getFromPreferences("TagCloudView_x", 0.0f);
        y = getFromPreferences("TagCloudView_y", 0.0f);
        // set the center of the sphere on center of our screen:
        centerX = width / 2;
        centerY = height / 2;
        radius = Math.min(centerX * 0.95f, centerY * 0.95f); // use 95% of
        // screen
        // since we set tag margins from left of screen, we shift the whole tags
        // to left so that
        // it looks more realistic and symmetric relative to center of screen in
        // X direction
        shiftLeft = (int) (Math.min(centerX * 0.15f, centerY * 0.15f));

        // initialize the TagCloud from a list of tags
        // Filter() func. screens tagList and ignores Tags with same text (Case
        // Insensitive)
        mTagCloud = new TagCloud(Filter(tagList), (int) radius, textSizeMin, textSizeMax);
        
        float[] tempColor1 = {1.0f, 1.0f, 1.0f, 1}; // rgb Alpha
        // {1f,0f,0f,1} red {0.3882f,0.21568f,0.0f,1} orange
        // {0.9412f,0.7686f,0.2f,1} light orange
        float[] tempColor2 = {0.6f, 0.6f, 0.6f, 1}; // rgb Alpha
        // {0f,0f,1f,1} blue
        // {0.1294f,0.1294f,0.1294f,1}
        // grey
        // {0.9412f,0.7686f,0.2f,1}
        // light orange
        mTagCloud.setTagColor1(tempColor1);// higher color
        mTagCloud.setTagColor2(tempColor2);// lower color
        mTagCloud.setRadius((int) radius);
        mTagCloud.create(true); // to put each Tag at its correct initial
        // location
        // update the transparency/scale of tags
        mTagCloud.setAngleX(mAngleX);
        mTagCloud.setAngleY(mAngleY);
        mTagCloud.update();
       
        mTextView = new ArrayList<TextView>();
        mParams = new ArrayList<LayoutParams>();
        // Now Draw the 3D objects: for all the tags in the TagCloud
        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        int i = 0;

        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            tempTag.setParamNo(i); // store the parameter No. related to this
            // tag
            mTextView.add(new TextView(this.mContext));
            mTextView.get(i).setText(tempTag.getText());
            mTextView.get(i).setTag(tempTag);
            mTextView.get(i).setShadowLayer(3, 1, 1, Color.BLACK);
            mParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mParams.get(i).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()), (int) (centerY + tempTag.getLoc2DY()), 0, 0);
            mTextView.get(i).setLayoutParams(mParams.get(i));
            mTextView.get(i).setSingleLine(true);
            int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255), (int) (tempTag.getColorG() * 255),
                    (int) (tempTag.getColorB() * 255));
            mTextView.get(i).setTextColor(mergedColor);
            mTextView.get(i).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
            addView(mTextView.get(i));
            mTextView.get(i).setOnClickListener(OnTagClickListener(tempTag.getUrl()));
            i++;
        }
        
        
        GestureListener gestureListener = new GestureListener(getRootView());
        gestureDetector = new GestureDetector(mContext, gestureListener);
    }

    public void setTagCloudType(TagCloudType type) {
        tagCloudType = type;
        handler.removeCallbacks(animatorSpherical);
        handler.removeCallbacks(animatorBarrel);
        if (tagCloudType == TagCloudType.SPHERE) {
            handler.post(animatorSpherical);
        } else {
            postInvalidate();
        }
    }

    public enum TagCloudType {
        SPHERE, BARREL, FLAT
    }

    float x = 0;
    float y = 0;
    // int textColor = Color.WHITE;
    float changerY = 0.1f;
    float changerX = 0.1f;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
      
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (tagCloudType == TagCloudType.BARREL) {
           updateViewBarrel();
        } else if (tagCloudType == TagCloudType.FLAT) {
            updateViewFlat();
        }
        super.dispatchDraw(canvas);
    }

    Handler handler = new Handler();
    boolean isAnimating = true;
    Runnable animatorSpherical = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            if (isAnimating) {
            	Log.e("x",""+x);
            	Log.e("y",""+y);
            	Log.e("changerX",""+changerX);
            	Log.e("changerY",""+changerY);
            	Log.e("tspeed",""+tspeed);
                x = x + changerX;
                y = y + changerY;
                Log.d("TAG", "animatorSpherical x=" + x + ";changerX=" + changerX + " y=" + y + "; changerY=" + changerY);
               // updateViewSpherical(x, y, tspeed);
               // updateViewSpherical(x, y, 0.15f);
            }
        }
    };
    int timeSpherical = 500;
    int timeCircular = 10;

    
    
    public void addTag(Tag newTag) {
        mTagCloud.add(newTag);
        int i = mTextView.size();
        newTag.setParamNo(i);
        mTextView.add(new TextView(this.mContext));
        mTextView.get(i).setText(newTag.getText());
        mTextView.get(i).setTag(newTag);
        mTextView.get(i).setShadowLayer(3, 1, 1, Color.BLACK);
        mParams.add(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        mParams.get(i).addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParams.get(i).setMargins((int) (centerX - shiftLeft + newTag.getLoc2DX()), (int) (centerY + newTag.getLoc2DY()), 0, 0);
        mTextView.get(i).setLayoutParams(mParams.get(i));
        mTextView.get(i).setSingleLine(true);
        int mergedColor = Color.argb((int) (newTag.getAlpha() * 255), (int) (newTag.getColorR() * 255), (int) (newTag.getColorG() * 255),
                (int) (newTag.getColorB() * 255));
        mTextView.get(i).setTextColor(mergedColor);
        mTextView.get(i).setTextSize((int) (newTag.getTextSize() * newTag.getScale()));
        addView(mTextView.get(i));
        mTextView.get(i).setOnClickListener(OnTagClickListener(newTag.getUrl()));
    }

    private float scroll = 0;
    int changer = -2;
    Runnable animatorBarrel = new Runnable() {
        @Override
        public void run() {
            scroll = scroll + changer;
          postInvalidate();
        }
    };
    @SuppressLint("NewApi") 
    protected void updateViewBarrel() {
       float radius = centerY * 0.70f;
       if(scroll<-9000){
    	   scroll = -800;  
       }
       if(scroll>9000){
    	   scroll = 800;  
       }
       
       if (mScroller.computeScrollOffset()) {
       //    offset = mScroller.getCurrX();
         //  postInvalidateDelayed(30);
       }
       // scroll = 1000;  
        float deltaY = (centerY - radius) / 2;
        for (int i = 0; i < getChildCount(); i++) {
            float t = i + scroll / getHeight();
            double theta = Math.PI * 2 * t / getChildCount();
           float y = (float) (radius * Math.cos(theta));    // parametric circle equation
            float z = (float) (radius * Math.sin(theta));
        //    float y = (float) (radius * Math.cos(Math.PI * 2 * t / getChildCount()));    // parametric circle equation
        //    float z = (float) (radius * Math.sin(Math.PI * 2 * t / getChildCount()));
            float textSize = (radius + z) / radius / 2 * 20 + 16;
            float alpha = (radius + z) / radius / 2 * 127 + 128;
            View view = getChildAt(i);
            int left = getWidth() / 2 - view.getWidth() / 2;
            int top = (int) ((getHeight() / 2) + y);
            int right = left + view.getWidth();
            int bottom = top + view.getHeight();
            int color = (int) (t * 10);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
               textView.setTextSize(textSize);
                //textView.setAlpha(alpha);
                setAlphaAnimation(textView, alpha / 255);
                textView.setTextColor(Color.WHITE);
                LayoutParams params = (LayoutParams) textView.getLayoutParams();
                params.setMargins(left, top, 0, 0);
            //   textView.setLayoutParams(params);
            }
        }
        
        /* mTagCloud.setAngleX(90);
        mTagCloud.setAngleY(180);
        mTagCloud.update();
        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        int i=0;
        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            float t = i + scroll / getHeight();
            double theta = Math.PI * 2 * t / getChildCount();
            float y = (float) (radius * Math.cos(theta));    // parametric circle equation
            float z = (float) (radius * Math.sin(theta)); 
            float textSize = (radius + z) / radius / 2 * 32 + 16;
            float alpha = (radius + z) / radius / 2 * 127 + 128;
            TextView textView = mTextView.get(tempTag.getParamNo());
            int left = (int) (centerX - textView.getWidth() / 2);
            int top = (int) ((getHeight() / 2) + y);
            int right = left + textView.getWidth();
            int bottom = top + textView.getHeight();
            int color = (int) (t * 10);
            mParams.get(tempTag.getParamNo()).setMargins(left,top,0,0);
            textView.setTextSize(textSize);
            textView.setTextColor(Color.WHITE);
            textView.bringToFront();
            i++;
        }*/
        
        
     //  handler.postDelayed(animatorBarrel, timeCircular);
    }
	protected void updateViewSpherical(float x, float y, float tspeed) {
        // TODO Auto-generated method stub
        // rotate elements depending on how far the selection point is from
        // center of cloud
        float dx = x - centerX;
        float dy = y - centerY;
        mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
        mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;
        mTagCloud.setAngleX(mAngleX);
        mTagCloud.setAngleY(mAngleY);
        mTagCloud.update();
        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()), (int) (centerY + tempTag.getLoc2DY()),
                    0, 0);
            mTextView.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
            int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255), (int) (tempTag.getColorG() * 255),
                    (int) (tempTag.getColorB() * 255));
            mTextView.get(tempTag.getParamNo()).setTextColor(mergedColor);
            mTextView.get(tempTag.getParamNo()).bringToFront();
        }
        
    }

    public boolean Replace(Tag newTag, String oldTagText) {
        boolean result = false;
        int j = mTagCloud.Replace(newTag, oldTagText);
        if (j >= 0) { // then oldTagText was found and replaced with newTag data
            Iterator it = mTagCloud.iterator();
            Tag tempTag;
            while (it.hasNext()) {
                tempTag = (Tag) it.next();
                mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()),
                        (int) (centerY + tempTag.getLoc2DY()), 0, 0);
                mTextView.get(tempTag.getParamNo()).setText(tempTag.getText());
                mTextView.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
                int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255),
                        (int) (tempTag.getColorG() * 255), (int) (tempTag.getColorB() * 255));
                mTextView.get(tempTag.getParamNo()).setTextColor(mergedColor);
                mTextView.get(tempTag.getParamNo()).bringToFront();
            }
            result = true;
        }
        return result;
    }

    public void reset() {
        mTagCloud.reset();
        Iterator it = mTagCloud.iterator();
        Tag tempTag;
        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            mParams.get(tempTag.getParamNo()).setMargins((int) (centerX - shiftLeft + tempTag.getLoc2DX()), (int) (centerY + tempTag.getLoc2DY()),
                    0, 0);
            mTextView.get(tempTag.getParamNo()).setTextSize((int) (tempTag.getTextSize() * tempTag.getScale()));
            int mergedColor = Color.argb((int) (tempTag.getAlpha() * 255), (int) (tempTag.getColorR() * 255), (int) (tempTag.getColorG() * 255),
                    (int) (tempTag.getColorB() * 255));
            mTextView.get(tempTag.getParamNo()).setTextColor(mergedColor);
            mTextView.get(tempTag.getParamNo()).bringToFront();
        }
    }

    @SuppressLint("NewApi") @Override
    public boolean onTrackballEvent(MotionEvent e) {
        if (tagCloudType == TagCloudType.SPHERE) {
            float x = e.getX();
            float y = e.getY();
            if (e.getY() != prevY) {
                if (e.getY() - prevY > 0) {
                    changerY = 0 - Math.abs(changerY);
                } else {
                    changerY = Math.abs(changerY);
                }
            }
            if (e.getX() != prevX) {
                if (e.getX() - prevX > 0) {
                    changerX = 0 - Math.abs(changerX);
                } else {
                    changerX = Math.abs(changerX);
                }
            }
            updateViewSpherical(-x, y, tspeed);
        } else if (tagCloudType == TagCloudType.BARREL) {
            if (e.getAction() != MotionEvent.ACTION_DOWN) {
                float diff = e.getY() - prevY;
                scroll = scroll - diff * 4;     // only one plane
                if (e.getY() != prevY) {
                    if (e.getY() - prevY > 0) {
                    	 changer = Math.abs(changer);
                    } else {
                    	
                    		changer = 0 + Math.abs(changer);
                    }
                }
                Log.e("onTrackball", "scroll=" + scroll + "; prevY=" + prevY + "; y=" + e.getY() + "; changer=" + changer);
            }
            int initialX = getRootView().getScrollX();
            int initialY = getRootView().getScrollY();
         //   scroll = scroll + changer;
            prevY = e.getY();
            //postInvalidateOnAnimation();
          //  handler.postDelayed(animatorBarrel, timeCircular);
          //  handler.postDelayed(animatorBarrel, timeCircular);
          //  postInvalidate();
        }
        return true;
    }

    float prevX = 0.0f;
    float prevY = 0.0f;

    @SuppressLint("ClickableViewAccessibility") @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (tagCloudType == TagCloudType.SPHERE) {
            float x = e.getX();
            float y = e.getY();
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isAnimating = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    Log.d("TAG", "x=" + x + ";changerX=" + changerX + " y=" + y + "; changerY=" + changerY);
                    if (e.getY() != prevY) {
                        if (e.getY() - prevY > 0) {
                            changerY = 0 - Math.abs(changerY);
                        } else {
                            changerY = Math.abs(changerY);
                        }
                    }
                    if (e.getX() != prevX) {
                        if (e.getX() - prevX > 0) {
                        	  changerX = Math.abs(changerX);
                        } else {
                        	  changerX = 0 - Math.abs(changerX);
                        }
                    }
                    updateViewSpherical(x, y, tspeed * 2);
                    break;
                case MotionEvent.ACTION_UP:
                    isAnimating = true;
                    break;
        /*
         * case MotionEvent.ACTION_UP: //now it is clicked!!!! dx = x - centerX;
		 * dy = y - centerY; break;
		 */
            }
            this.y = prevY = e.getY();
            this.x = prevX = e.getX();
        } else if (tagCloudType == TagCloudType.BARREL) {
            if (e.getAction() != MotionEvent.ACTION_DOWN) {
            	Log.e("prevY",""+prevY);
                float diff = e.getY() - prevY;
                scroll = scroll - diff *4;     // only one plane
                if (e.getY() != prevY) {
                    if (e.getY() - prevY > 0) {
                    	  changer = 0 + Math.abs(changer);
                    	
                    } else {
                    	
                    }  changer = Math.abs(changer);
                }
                Log.e("ontouch", "scroll=" + scroll + "; prevY=" + prevY + "; y=" + e.getY() + "; changer=" + changer);
            }
           // changer = Math.abs(changer);
          //  scroll = scroll + changer;
            prevY = e.getY();
          //updateViewBarrel();
          ///  postInvalidate();
          //   int in = (int)Math.round(prevX);
            
           // for(float i = prevX; i<prevX+3; i++){
            	
            	//prevX = i;
            	//   handler.postDelayed(animatorBarrel, timeCircular);
          //  }
            postInvalidate();
            View view = getRootView();
            AnimationSet animationSet = new AnimationSet(true);
            TranslateAnimation a = new TranslateAnimation(
                    Animation.INFINITE,0, Animation.INFINITE,0,
                    Animation.INFINITE,0, Animation.INFINITE,0);
            a.setDuration(1000);    
            RotateAnimation r = new RotateAnimation(0f, -190f,200,200);
            r.setStartOffset(1000);
            r.setDuration(1000);
            r.setRepeatCount(100);
            r.setRepeatMode(20);
            animationSet.addAnimation(a);
            animationSet.addAnimation(r);
            TranslateAnimation animation = new TranslateAnimation(0.0f, 1.0f,0.0f, 2.0f);          //  new TranslateAnimation(xFrom,xTo, yFrom,yTo)
            animation.setDuration(1000);  // animation duration 
            animation.setRepeatCount(50);  // animation repeat count
            animation.setRepeatMode(10);   // repeat animation (left to right, right to left )
            //animation.setFillAfter(true);      
           // view.startAnimation(animation); 
         //   setScaleAnimation(view);
        }
        return true;
    }

    private void setAlphaAnimation(View view, float alpha) {
        // TODO Auto-generated method stub
        AnimationSet set = new AnimationSet(true);
        Animation animation = new AlphaAnimation(alpha, alpha);
        set.addAnimation(animation);
        set.setDuration(0);
        set.setFillAfter(true);
        view.setAnimation(set);
        set.start();
    }

    private void setScaleAnimation(View view) {
        // TODO Auto-generated method stub
        ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 5.0f, 1.0f, 5.0f, 50.0f, 50.0f);
        scaleAnimation.setDuration(500);
        scaleAnimation.setFillAfter(false);
        view.startAnimation(scaleAnimation);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (tagCloudType == TagCloudType.SPHERE) {
          handler.postDelayed(animatorSpherical, timeSpherical);
        } else if (tagCloudType == TagCloudType.BARREL) {
            updateViewBarrel();
        } else if (tagCloudType == TagCloudType.FLAT) {
            updateViewFlat();
        }
    }

    private void updateViewFlat() {
        float top = 10;
        float left = 0;
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View view = getChildAt(i);
            //top = (int) ((getHeight() / 2) + y);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;
                //textView.setTextColor(Color.WHITE);
                left = left + 20;
                LayoutParams params = (LayoutParams) textView.getLayoutParams();
                params.setMargins((int) left, (int) top, 0, 0);
                textView.setLayoutParams(params);
            }

            left = left + view.getWidth();
            if (left >= getWidth()) {
                left = 0;
                top = top + view.getHeight();
            } else {
                if (i - 1 >= 0) {
                    top = top + (view.getHeight() - getChildAt(i - 1).getHeight());
                }
            }
            if (i - 1 >= 0) {
                if (getWidth() - left < getChildAt(i - 1).getWidth()) {
                    left = 0;
                    top = top + view.getHeight();
                }
            }
            Log.e("updateview", "left=" + left + "; top=" + top);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        onTouchEvent(ev);
        return false;
    }

    String urlMaker(String url) {
        if ((url.substring(0, 7).equalsIgnoreCase("http://")) || (url.substring(0, 8).equalsIgnoreCase("https://")))
            return url;
        else
            return "http://" + url;
    }

    // the filter function makes sure that there all elements are having unique
    // Text field:
    List<Tag> Filter(List<Tag> tagList) {
        // current implementation is O(n^2) but since the number of tags are not
        // that many,
        // it is acceptable.
        List<Tag> tempTagList = new ArrayList();
        Iterator itr = tagList.iterator();
        Iterator itrInternal;
        Tag tempTag1, tempTag2;
        // for all elements of TagList
        while (itr.hasNext()) {
            tempTag1 = (Tag) (itr.next());
            boolean found = false;
            // go over all elements of temoTagList
            itrInternal = tempTagList.iterator();
            while (itrInternal.hasNext()) {
                tempTag2 = (Tag) (itrInternal.next());
                if (tempTag2.getText().equalsIgnoreCase(tempTag1.getText())) {
                    found = true;
                    break;
                }
            }
            if (found == false)
                tempTagList.add(tempTag1);
        }
        return tempTagList;
    }

    // for handling the click on the tags
    // onclick open the tag url in a new window. Back button will bring you back
    // to TagCloud
    OnClickListener OnTagClickListener(final String url) {
        return new OnClickListener() {
            @Override
            public void onClick(final View v) {
                // we now have url from main code
                /*
                 * Uri uri = Uri.parse(urlMaker(url)); // just open a new intent
				 * and set the content to search for the // url
				 * mContext.startActivity(new Intent(Intent.ACTION_VIEW, uri));
				 */
                saveInPreferences("TagCloudView_x", x);
                saveInPreferences("TagCloudView_y", y);
                setScaleAnimation(v);
                final Tag tag = (Tag) v.getTag();
                if (v instanceof TextView) {
                    TextView textView = (TextView) v;
                    textView.setTextColor(Color.RED);
                    textView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            if (onTagClickListener != null) {
                                onTagClickListener.onTagClick(v, tag);
                            }
                        }
                    }, 500);
                }

            }
        };
    }

    @Override
    protected void onDetachedFromWindow() {
        // TODO Auto-generated method stub
        saveInPreferences("TagCloudView_x", x);
        saveInPreferences("TagCloudView_y", y);
        super.onDetachedFromWindow();
    }
   
    public float getFromPreferences(String name, float fDefault) {
        // TODO Auto-generated method stub
        SharedPreferences selectPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        return selectPreferences.getFloat(name, fDefault);
    }

    public void saveInPreferences(String name, float fData) {
        // TODO Auto-generated method stub
        SharedPreferences selectPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor prefsEditor = selectPreferences.edit();
        prefsEditor.putFloat(name, fData);
        prefsEditor.commit();
    }

    OnTagClickListener onTagClickListener;

    public void setOnTagClickListener(OnTagClickListener onTagClickListener) {
        this.onTagClickListener = onTagClickListener;
    }

    public abstract interface OnTagClickListener {
        public void onTagClick(View view, Tag tag);
    }
    private final float TOUCH_SCALE_FACTOR = .1f;
    private final float TRACKBALL_SCALE_FACTOR = 20;
    private float tspeed;
    private TagCloud mTagCloud;
    private TagCloud mTCloud;
    private float mAngleX = 0;
    private float mAngleY = 0;
    private float centerX, centerY;
    private float radius;
    private Context mContext;
    private int textSizeMin, textSizeMax;
    private List<TextView> mTextView;
    private List<LayoutParams> mParams;
    private int shiftLeft;
}
