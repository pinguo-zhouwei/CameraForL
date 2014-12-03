package us.pinguo.cameraforl.ui;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;

import us.pinguo.cameraforl.R;


public class MainActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState != null) {
            return;
        }
        getFragmentManager().beginTransaction().add(R.id.fragment_container, CameraFragment.getInstance()).commit();

    }


}
