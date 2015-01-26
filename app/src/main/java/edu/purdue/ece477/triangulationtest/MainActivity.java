package edu.purdue.ece477.triangulationtest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;


public class MainActivity extends ActionBarActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private volatile Gson gson;
    private Handler handler = new Handler();

    public volatile String instruction;
    public volatile boolean scanning;

    public volatile int count;
    private WifiManager mWifiManager;
    public volatile String scanmode;
    public volatile ArrayList<Hashtable<String,String>> packet, accpacket;

    public volatile int tilecount;

    public void nextTile(View v){
        instruction = "===== NEXT TILE \n";
        scanning = true;
        scanmode = "TILE-BASED";

        tilecount++;
        TextView tilect = (TextView)findViewById(R.id.textView3);
        tilect.setText("Tile " + tilecount);
    }

    public volatile float [] acceleration;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tilecount = 0;

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        count = 0;

        mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        scanning = false;

        gson = new Gson();

        packet = new ArrayList<Hashtable<String, String>>();
        accpacket = new ArrayList<Hashtable<String, String>>();


        IntentFilter i = new IntentFilter();
        i.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                if(!scanning){
                    handler.postDelayed(runnable, 100);
                    return;
                }

                List<ScanResult> other = mWifiManager.getScanResults();
                runOnUiThread(new Runnable() {
                    //Clear result text
                    @Override
                    public void run() {

                        TextView lbl = (TextView)findViewById(R.id.sigtext);
                        lbl.setText("");
                    }
                });



                StringBuilder sb = new StringBuilder();
                for(final ScanResult sr : other){

                    sb.append(sr.BSSID + "," +  sr.SSID + "," + sr.level + "," +
                            mWifiManager.calculateSignalLevel(sr.level, 100) + ", " +  " TILE " + tilecount + ", ACC (xy) " + acceleration[0]
                            + "," + acceleration[1] + "\n");

                    EditText edt = (EditText)findViewById(R.id.filename_prefix);
                    Hashtable<String, String> entry = new Hashtable<>();
                    entry.put("bssid", sr.BSSID);
                    entry.put("ssid",sr.SSID);
                    entry.put("level", String.valueOf(sr.level));
                    entry.put("levelnorm", String.valueOf( mWifiManager.calculateSignalLevel(sr.level, 100)));
                    entry.put("session", edt.getText() + ", " + scanmode + ", TILE " + tilecount);
                    entry.put("acc_x", acceleration[0] + "");
                    entry.put("acc_y", acceleration[1] + "");
                    entry.put("acc_z", acceleration[2] + "");

                    packet.add(entry);


                    runOnUiThread(new Runnable() {
                        //Update result text
                        @Override
                        public void run() {
                            TextView lbl = (TextView) findViewById(R.id.sigtext);
                            lbl.append(sr.BSSID + " (" + sr.SSID + ") = " + sr.level + "\n");
                            TextView cc = (TextView) findViewById(R.id.countlbl);
                            cc.setText(count + "");
                        }
                    });


                }

                try {



                    File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);





                    DateFormat dateFormat = new SimpleDateFormat("MM_dd-HH");
                    Date date = new Date();


                    String bucket = "477_" + dateFormat.format(date);
                    EditText edt = (EditText)findViewById(R.id.filename_prefix);
                    File file = new File(path, edt.getText() + bucket + ".txt");
                    if(!file.exists()){
                        file.createNewFile();
                    }

                    FileWriter fileWritter = new FileWriter( file.getAbsoluteFile(),true);
                    BufferedWriter bufferWritter = new BufferedWriter(fileWritter);
                    //write instruction header
                    bufferWritter.append( instruction );

                    //clear instruction
                    instruction = "--------------------\n";

                    //write wifi data
                    bufferWritter.append(sb.toString() + "\n");
                    bufferWritter.flush();
                    bufferWritter.close();

                }catch(Exception ex){
                    Log.e("seniordes", ex.toString());
                    ex.printStackTrace();
                }


                handler.postDelayed(runnable, 100);
            }
        }, i);

        //first scan
        handler.postDelayed(runnable, 100);
    }

    public void forwardlog(View v){
        TextView dist = (TextView)findViewById(R.id.textbox_cm);

        Log.i("477","recording forward");
        instruction = "=====fwd|" + dist.getText() +"\n";
        scanning = true;
        scanmode = "FORWARD";
    }

    public void leftlog(View v){
        TextView dist = (TextView)findViewById(R.id.textbox_cm);

        Log.i("477","recording left");
        instruction = "=====left|" + dist.getText() +"\n";
        scanning = true;
        scanmode = "LEFT";
    }

    public void rightlog(View v){
        TextView dist = (TextView)findViewById(R.id.textbox_cm);
        Log.i("477","recording right");
        instruction = "=====right|" + dist.getText() +"\n";
        scanning = true;
        scanmode = "RIGHT";
    }

    public void stoplog(View v){
        scanning = false;
        scanmode = "";
        count = 0;
        tilecount = 0;
        TextView tilect = (TextView)findViewById(R.id.textView3);
        tilect.setText("Tile " + tilecount);

        TextView scancount = (TextView)findViewById(R.id.countlbl);
        scancount.setText("" + count);


    }

    public void uploadlog(View v){
        final String json = gson.toJson(packet);

        Thread threadA = new Thread() {
            @Override
            public void run() {
                MediaType JSON = MediaType.parse("text/html ; charset=utf-8");
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(JSON, json);
                Request request = new Request.Builder()
                        .url("http://web.ics.purdue.edu/~ssabpisa/WINS/add.php")
                        .put(body)
                        .build();
                try{
                    Response response = client.newCall(request).execute();
                    String resp =  response.body().string();
                    Log.i("result", resp);
                    runOnUiThread(new Runnable() {
                        //Clear result text
                        @Override
                        public void run() {
                            Context context = getApplicationContext();
                            CharSequence text = "Uploaded Signal Data!";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    });
                    packet.clear();

                }catch(Exception ex){
                    runOnUiThread(new Runnable() {
                        //Clear result text
                        @Override
                        public void run() {
                            Context context = getApplicationContext();
                            CharSequence text = "Unable to upload Signal Data!";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    });

                    Log.e("error", ex.toString());
                }

            }
        };

        final String jsonACC = gson.toJson(accpacket);
        Thread threadB = new Thread() {
            @Override
            public void run() {
                MediaType JSON = MediaType.parse("text/html ; charset=utf-8");
                OkHttpClient client = new OkHttpClient();
                RequestBody body = RequestBody.create(JSON, json);
                Request request = new Request.Builder()
                        .url("http://web.ics.purdue.edu/~ssabpisa/WINS/add_acc.php")
                        .put(body)
                        .build();
                try{
                    Response response = client.newCall(request).execute();
                    String resp =  response.body().string();
                    Log.i("result", resp);


                    runOnUiThread(new Runnable() {
                        //Clear result text
                        @Override
                        public void run() {
                            Context context = getApplicationContext();
                            CharSequence text = "Uploaded Acceleration Data!";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    });
                    accpacket.clear();
                }catch(Exception ex){
                    runOnUiThread(new Runnable() {
                        //Clear result text
                        @Override
                        public void run() {
                            Context context = getApplicationContext();
                            CharSequence text = "Unable to upload Acceleration Data!";
                            int duration = Toast.LENGTH_SHORT;

                            Toast toast = Toast.makeText(context, text, duration);
                            toast.show();
                        }
                    });

                    Log.e("error", ex.toString());
                }

            }
        };

        threadB.start();
        threadA.start();



    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            CheckBox cb = (CheckBox)findViewById(R.id.checkBox);
            if(count == 50 &&  cb.isChecked()){
                stoplog(null);
            }
            if(!scanning) {
                count = 0;
                runOnUiThread(new Runnable() {
                    //Clear result text
                    @Override
                    public void run() {

                        TextView cc = (TextView)findViewById(R.id.countlbl);
                        cc.setText(""+count);
                    }
                });

                handler.postDelayed(this, 100);
                return;
            }
            count++;
            mWifiManager.startScan();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.i("accelerate", "x,y,z : (" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ")" );
        final EditText edt = (EditText)findViewById(R.id.filename_prefix);

        Hashtable<String, String> entry = new Hashtable<>();
        entry.put("acc_x", event.values[0] + "");
        entry.put("acc_y", event.values[1] + "");
        entry.put("acc_z", event.values[2] + "");
        entry.put("session", edt.getText() + ", " + scanmode);

        accpacket.add(entry);

        acceleration = event.values.clone();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
