package us.pinguo.cameraforl.ui;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.Switch;

import us.pinguo.cameraforl.R;

/**
 * Created by zhouwei on 14-10-31.
 */
public class SettingActivity extends Activity {
    private SharedPreferences mSharedPreferences;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        initView();
    }

    /**
     * View初始化
     */
    public void initView(){
        mSharedPreferences = getSharedPreferences("config",MODE_PRIVATE);
        final SharedPreferences.Editor editor = mSharedPreferences.edit();;
        Switch aeMode = (Switch)findViewById(R.id.ae_mode);
        Switch dngFprmat = (Switch) findViewById(R.id.dng_format);
        aeMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                 if(isChecked){
                     editor.putBoolean("open_ae_mode", true);
                 }else{
                     editor.putBoolean("open_ae_mode",false);
                 }
                editor.commit();
            }
        });
       dngFprmat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
           @Override
           public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
               if(isChecked){
                   editor.putBoolean("save_dng", true);
               }else{
                   editor.putBoolean("save_dng",false);
               }
               editor.commit();
           }
       });

    }
}
