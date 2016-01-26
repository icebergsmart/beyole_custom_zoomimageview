package com.beyole.customzoomimageview;

import com.beyole.view.ZoomImageView;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class MainActivity extends Activity {

	private ViewPager mViewPager;
	private int[] imgs = new int[] { R.drawable.img, R.drawable.news };
	private ImageView[] imageViews = new ImageView[imgs.length];

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mViewPager = (ViewPager) findViewById(R.id.id_viewpager);
		mViewPager.setAdapter(new PagerAdapter() {

			@Override
			public Object instantiateItem(ViewGroup container, int position) {
				ZoomImageView imageView = new ZoomImageView(getApplicationContext());
				imageView.setImageResource(imgs[position]);
				container.addView(imageView);
				imageViews[position] = imageView;
				return imageView;
			}

			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0==arg1;
			}

			@Override
			public void destroyItem(ViewGroup container, int position, Object object) {
				container.removeView(imageViews[position]);
			}

			@Override
			public int getCount() {
				return imgs.length;
			}
		});
	}

}
