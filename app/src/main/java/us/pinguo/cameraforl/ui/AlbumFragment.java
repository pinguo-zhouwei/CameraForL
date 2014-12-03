package us.pinguo.cameraforl.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;

import us.pinguo.cameraforl.R;


/**
 * Created by zhouwei on 14-10-11.
 */
public class AlbumFragment extends Fragment {
    private File filePaths[];
    DisplayImageOptions options;
    /**
     * 获取Fragment实例
     * @return
     */
    static AlbumFragment getInstance(){
        AlbumFragment fragment = new AlbumFragment();
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"CameraForL");
        filePaths = file.listFiles();

        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.img_defaut)
                .showImageForEmptyUri(R.drawable.img_defaut)
                .showImageOnFail(R.drawable.img_defaut)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       View view = inflater.inflate(R.layout.fragment_album,container,false);
        GridView gridView = (GridView) view.findViewById(R.id.album_gridview);
        GridViewAdapter adapter = new GridViewAdapter();
        gridView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        gridView.setOnItemClickListener(new GridOnItemClickListener());
        return view;
    }

    class GridOnItemClickListener implements AdapterView.OnItemClickListener{

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Bundle bundle = new Bundle();
            bundle.putInt("index",position);
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            SeekBigPicFragment fragment = new SeekBigPicFragment();
            fragment.setArguments(bundle);
            ft.replace(R.id.album_container,fragment);
            ft.addToBackStack(null);
            ft.commit();

        }
    }
    class GridViewAdapter extends BaseAdapter {
      //  BitmapCache cache;
     public GridViewAdapter(){
        // cache = BitmapCache.getCacheInstance();
     }
        @Override
        public int getCount() {
            return filePaths.length;
        }

        @Override
        public Object getItem(int position) {
            return filePaths[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if(convertView==null){
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.album_item,null);
                viewHolder.imageView = (ImageView) convertView.findViewById(R.id.album_item_image);
                convertView.setTag(viewHolder);
            }
            viewHolder = (ViewHolder) convertView.getTag();
            String url = filePaths[position].toString();
            String imgUrl[] = url.split("/");
            url = "file:///mnt/sdcard/"+imgUrl[4]+"/"+imgUrl[5];
            System.out.println("url:"+url);
            ImageLoader.getInstance().displayImage(url,viewHolder.imageView,options);
            return convertView;
        }

        class ViewHolder{
            ImageView imageView;
        }

    }
}
