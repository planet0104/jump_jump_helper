package site.fangte.app.jumpjumphelper;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.TargetApi;
import android.graphics.Path;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.Random;

import static site.fangte.app.jumpjumphelper.MainActivity.jumping;
import static site.fangte.app.jumpjumphelper.MainActivity.onGestureCompleted;

/**
 *
 * Created by JiaYe on 2018/1/5.
 */

public class HelperService extends AccessibilityService {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void performTouch(int duration, int top){
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        Path path = new Path();

        Log.d("helper", "duration="+duration);

        //随机坐标
        int min = top-100;
        int max = top-50;
        Random random = new Random();
        int v = random.nextInt(max)%(max-min+1) + min;
        path.moveTo(v, v);
        path.lineTo(v+10, v+10);

        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(path, 50, duration));
        GestureDescription gesture = gestureBuilder.build();
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d("helper", "onCompleted");
                onGestureCompleted = true;
                jumping = false;
                super.onCompleted(gestureDescription);
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.d("helper", "onCancelled");
                super.onCancelled(gestureDescription);
            }
        }, new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                return false;
            }
        }));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        MainActivity.helperService = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    }

    @Override
    public void onInterrupt() {

    }
}
