package site.fangte.app.jumpjumphelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import cds.sdg.sdf.nm.sp.SpotManager;

public class MainActivity extends Activity {
    static final String TAG = MainActivity.class.getSimpleName();
    public static HelperService helperService;
    public static boolean jumping = false;
    Button btn_open;
    Button btn_help;

    View panel;
    FrameLayout ll_panel_root;
    TextView tv_close;
    TextView tv_tool;
    TextView tv_start_flag;
    TextView tv_end_flag;
    LinearLayout ll_error;
    boolean show = false;
    Button btn_close_err;
    Button btn_show_help;
    TextView tv_share;

    PointF startPoint;
    //检测回调是否成功
    public static boolean onGestureCompleted = false;

    //h为高, w为距离
    double h = 0;
    double w = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_open =  findViewById(R.id.btn_open);
        btn_help =  findViewById(R.id.btn_help);

        setTitle(R.string.app_name_full);

        btn_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open();
            }
        });
        btn_help.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(MainActivity.this, HelpActivity.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (Settings.canDrawOverlays(this)) {
                open();
            }
        }
    }

    public void open(){
        if(show){
            return;
        }
        if(!Settings.canDrawOverlays(getApplicationContext())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("使用提示");
            builder.setMessage("请检允许跳一跳助手“显示在其他应用的上层/显示悬浮窗”");
            builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //启动Activity让用户授权
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent,100);
                }
            });
            builder.create().show();
            return;
        }

        try{
            startService(new Intent(this, HelperService.class));
            panel = LayoutInflater.from(this).inflate(R.layout.tool_bar, null);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSPARENT);

            //6.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
            }

            params.gravity = Gravity.RIGHT|Gravity.CENTER_VERTICAL;
            WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.addView(panel, params);
            show = true;

            tv_tool =  panel.findViewById(R.id.tv_tool);
            tv_close =  panel.findViewById(R.id.tv_close);
            ll_panel_root =  panel.findViewById(R.id.ll_panel_root);
            tv_start_flag =  panel.findViewById(R.id.tv_start_flag);
            tv_end_flag =  panel.findViewById(R.id.tv_end_flag);
            ll_error =  panel.findViewById(R.id.ll_error);
            btn_close_err =  panel.findViewById(R.id.btn_close_err);
            btn_show_help =  panel.findViewById(R.id.btn_show_help);
            tv_share = panel.findViewById(R.id.tv_share);

            btn_close_err.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ll_error.setVisibility(View.GONE);
                }
            });
            btn_show_help.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tv_tool.post(new Runnable() {
                        @Override
                        public void run() {
                            minimize();
                            startActivity(new Intent(MainActivity.this, HelpActivity.class));
                        }
                    });
                }
            });
            tv_share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareApp();
                }
            });

            final int panelWidth = getResources().getDisplayMetrics().widthPixels;
            final int panelHeight = (int) (getResources().getDisplayMetrics().heightPixels*0.8);

            ll_panel_root.getLayoutParams().width = panelWidth;
            ll_panel_root.getLayoutParams().height = panelHeight;

            ll_panel_root.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (jumping){
                        return false;
                    }
                    //Log.d(TAG, "x="+event.getX()+" y="+event.getY());
                    //Log.d(TAG, "rawx="+ event.getRawX()+" rawy="+event.getRawY());
                    if(event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_OUTSIDE){
                        Log.d(TAG, "弹起:"+event.getAction()+" startPoint="+startPoint);
                        if(startPoint == null){
                            //记录起点
                            startPoint = new PointF();
                            startPoint.x = event.getX();
                            startPoint.y = event.getY();
                        }else{
                            //放置了第二个旗子
                            PointF end = new PointF();
                            end.x = event.getX();
                            end.y = event.getY();

                            //清除
                            tv_end_flag.setVisibility(View.INVISIBLE);
                            tv_start_flag.setVisibility(View.INVISIBLE);

                            //时间
                            float x1 = startPoint.x;
                            float y1 = startPoint.y;
                            float x2 = end.x;
                            float y2 = end.y;
                            //计算距离
                            double distance = Math.abs(Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2)));
                            System.out.println("距离："+distance);

                            //距离按照屏幕宽度比例
//                            distance = distance/getResources().getDisplayMetrics().widthPixels;

                            if(h==0){
                                h = distance;
//                                Toast.makeText(MainActivity.this, "bobSize="+bobSize, Toast.LENGTH_SHORT).show();
                                //清空
                                Log.d(TAG, "h=="+h);
                                startPoint = null;
                                return true;
                            }
                            if(w==0){
                                w = distance;
                                Log.d(TAG, "w=="+w);
//                                startPoint = null;
//                                return true;
                            }

                            //根据距离模拟按下事件
//                            double second = distance*(2.2*(1.0-bobSize)); //距离*2=按下秒数
                            double t = w/(0.78/(466/h));
                            System.out.println("时间："+t);

                            //距离406 时间 700ms

                            //起跳
                            Log.d(TAG, "开始跳:");
                            onGestureCompleted = false;
                            jumping = true;

                            int[] loc = new int[2];
                            ll_panel_root.getLocationOnScreen(loc);
                            System.out.println("屏幕位置:"+ Arrays.toString(loc));

                            helperService.performTouch((int) (t), loc[1]);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    jumping = false;
                                    if(onGestureCompleted == false){
                                        ll_error.setVisibility(View.VISIBLE);
                                    }
                                }
                            }, (long) (t+500));
                            startPoint = null;
                            w = 0;
                            h = 0;
                        }
                    }
                    if(event.getAction() == MotionEvent.ACTION_MOVE){
                        Log.d(TAG, "move:");
                        //第一次放置旗子
                        if(startPoint == null){
                            tv_start_flag.setVisibility(View.VISIBLE);
                            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) tv_start_flag.getLayoutParams();
                            flp.leftMargin = (int) (event.getX()-tv_start_flag.getMeasuredWidth());
                            flp.topMargin = (int) event.getY();
                            tv_start_flag.setLayoutParams(flp);
                        }else{
                            tv_end_flag.setVisibility(View.VISIBLE);
                            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) tv_end_flag.getLayoutParams();
                            flp.leftMargin = (int) (event.getX()-tv_end_flag.getMeasuredWidth());
                            flp.topMargin = (int) event.getY();
                            tv_end_flag.setLayoutParams(flp);
                        }
                    }
                    return true;
                }
            });

            tv_tool.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(tv_tool.getText().equals("收起")){
                        minimize();
                    }else{
                        tv_tool.setText("收起");
                        tv_share.setVisibility(View.VISIBLE);
                        tv_close.setVisibility(View.VISIBLE);
                        ll_panel_root.getLayoutParams().width = panelWidth;
                        ll_panel_root.getLayoutParams().height = panelHeight;
                        ll_panel_root.setLayoutParams(ll_panel_root.getLayoutParams());
                    }
                }
            });

            tv_close.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }catch (Exception e){
            e.printStackTrace();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("启动失败");
            builder.setMessage("请检查是否允许“显示在其他应用的上层”的权限！");
            builder.setNegativeButton("关闭", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setPositiveButton("查看帮助", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    if(tv_tool == null){
                        tv_tool =  panel.findViewById(R.id.tv_tool);
                    }
                    tv_tool.performLongClick();
                    startActivity(new Intent(MainActivity.this, HelpActivity.class));
                }
            });
            builder.create().show();
        }
    }

    void minimize(){
        tv_tool.setText("展开");
        tv_close.setVisibility(View.GONE);
        //清除flag
        startPoint = null;
        tv_end_flag.setVisibility(View.INVISIBLE);
        tv_start_flag.setVisibility(View.INVISIBLE);
        FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) tv_start_flag.getLayoutParams();
        flp.leftMargin = 0;
        flp.topMargin = 0;
        tv_start_flag.setLayoutParams(flp);
        flp = (FrameLayout.LayoutParams) tv_end_flag.getLayoutParams();
        flp.leftMargin = 0;
        flp.topMargin = 0;
        tv_end_flag.setLayoutParams(flp);

        //最小化
        ll_error.setVisibility(View.GONE);
        ll_panel_root.getLayoutParams().width = FrameLayout.LayoutParams.WRAP_CONTENT;
        ll_panel_root.getLayoutParams().height = FrameLayout.LayoutParams.WRAP_CONTENT;
        ll_panel_root.setLayoutParams(ll_panel_root.getLayoutParams());
        tv_share.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
        //super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        SpotManager.getInstance(this).onAppExit();
        //关闭
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (wm != null && panel!=null) {
            wm.removeView(panel);
        }
        show = false;
        super.onDestroy();
    }

    public File prepare(){
        try{
            boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
            if (sdCardExist) {
                File imgDir = new File(getCacheDir().toString()+"/apk1");
                if (!imgDir.exists()) {
                    imgDir.mkdirs();
                }
                File file = new File(imgDir, "share.png");
                if(!file.exists()){
                    //复制图片
                    InputStream in = getAssets().open("share_720p.png");
                    OutputStream os = new FileOutputStream(file);
                    int bytesRead = 0;
                    byte[] buffer = new byte[8192];
                    while ((bytesRead = in.read(buffer, 0, 8192)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.close();
                    in.close();
                    Log.d(TAG, "文件复制成功:"+file.getAbsolutePath());
                }
                return file;
            }else{
                Toast.makeText(this, "存储卡未启用。 ", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
    }

    public void shareApp(){
        final File f = prepare();
        if(f == null){
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
            return;
        }

        tv_tool.post(new Runnable() {
            @Override
            public void run() {
                minimize();
                try{
                    Uri contentUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".fileProvider", f);
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "微信跳一跳助手");
                    intent.putExtra(Intent.EXTRA_TEXT, "将小红旗的放到起点和终点位置，松手就会自动跳跃。小红旗拖放的越准确，跳的也就越准确。");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(Intent.createChooser(intent, "微信跳一跳助手"));
                }catch (Exception e){
                    Toast.makeText(MainActivity.this, "分享失败", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
    }
}
