package edu.purdue.ece477.triangulationtest;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {
    private Handler handler = new Handler();

    private WifiManager mWifiManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        TextView lbl = (TextView)findViewById(R.id.sigtext);
        //get wifi manager
        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);

//        WifiInfo current = mWifiManager.getConnectionInfo();

        handler.postDelayed(runnable, 100);

    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            List<ScanResult> other = mWifiManager.getScanResults();
            runOnUiThread(new Runnable() {
                //Clear result text
                @Override
                public void run() {

                    TextView lbl = (TextView)findViewById(R.id.sigtext);
                    lbl.setText("");
                }
            });
            for(final ScanResult sr : other){

                runOnUiThread(new Runnable() {
                    //Update result text
                    @Override
                    public void run() {

                        TextView lbl = (TextView)findViewById(R.id.sigtext);

                        lbl.append(sr.BSSID + " (" +  sr.SSID + ") = " + sr.level + "\n");

                    }
                });


            }

            //rescan in another 1.5 second
            handler.postDelayed(this, 1500);


        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
