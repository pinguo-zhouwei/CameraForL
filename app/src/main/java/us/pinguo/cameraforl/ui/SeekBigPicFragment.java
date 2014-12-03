package us.pinguo.cameraforl.ui;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import us.pinguo.cameraforl.R;
import us.pinguo.cameraforl.ZoomOutPageTransformer;

/**
 * Created by zhouwei on 14-10-11.
 */
public class SeekBigPicFragment extends Fragment {
    ViewPager viewPager;
    MyPagerAdapter adapter;
    File files[];
    DisplayImageOptions options;
    int currentItem;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"CameraForL");
        files = file.listFiles();
        //获取cache实例
     //   cache = BitmapCache.getCacheInstance();
        //获取显示的第一张照片的位置
        currentItem = getArguments().getInt("index");

        options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.ic_launcher)
                .showImageOnFail(R.drawable.ic_launcher)
                .resetViewBeforeLoading(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.EXACTLY)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .considerExifParams(true)
                .displayer(new FadeInBitmapDisplayer(300))
                .build();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_big_pic,container,false);
        viewPager = (ViewPager) view.findViewById(R.id.viewPager);
        //设置ViewPager的切换动画
        viewPager.setPageTransformer(true,new ZoomOutPageTransformer());
        adapter = new MyPagerAdapter();
        System.out.println("当前index："+currentItem);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentItem);
        adapter.notifyDataSetChanged();
        return view;
    }

    class MyPagerAdapter extends PagerAdapter {

        public  MyPagerAdapter(){

        }
        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view==o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
                ImageView imageView = null;
                imageView = new ImageView(getActivity());
                imageView.setMaxWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
                imageView.setMaxHeight(ViewGroup.LayoutParams.WRAP_CONTENT);

            String url = files[position].toString();
            String imgUrl[] = url.split("/");
            url = "file:///mnt/sdcard/"+imgUrl[4]+"/"+imgUrl[5];
            System.out.println("url:"+url);
            ImageLoader.getInstance().displayImage(url,imageView,options);
            ((ViewPager)container).addView(imageView);
            return imageView;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
           container.removeView((View) object);
        }
    }
}
