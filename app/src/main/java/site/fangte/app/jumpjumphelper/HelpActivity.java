package site.fangte.app.jumpjumphelper;

import android.os.Bundle;
import android.app.Activity;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;

public class HelpActivity extends Activity {
    ViewPager vp_help;
    int[] imgs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        vp_help = (ViewPager) findViewById(R.id.vp_help);
        imgs = new int[]{
                R.drawable.step1,
                R.drawable.step2,
                R.drawable.step3,
                R.drawable.step4,
                R.drawable.step5,
                R.drawable.step6,
                R.drawable.step7,
                R.drawable.step8,
                R.drawable.step9,
                R.drawable.step10,
        };
        vp_help.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object)   {
                container.removeView((View) object);
            }


            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                ImageView iv = new ImageView(HelpActivity.this);
                iv.setImageResource(imgs[position]);
                container.addView(iv, 0);
                return iv;
            }

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0==arg1;
            }
        });
    }

}
