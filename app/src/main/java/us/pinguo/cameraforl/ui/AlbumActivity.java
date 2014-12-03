package us.pinguo.cameraforl.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

import us.pinguo.cameraforl.R;

/**
 * Created by zhouwei on 14-10-10.
 */
public class AlbumActivity extends Activity {
    FragmentManager fm;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);
        if(savedInstanceState!=null){
            return;
        }
      AlbumFragment fragment = AlbumFragment.getInstance();
      fm = getFragmentManager();
      FragmentTransaction ft = fm.beginTransaction();
      ft.add(R.id.album_container,fragment);
      ft.commit();

    }
}
