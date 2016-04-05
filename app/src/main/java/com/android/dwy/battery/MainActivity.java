package com.android.dwy.battery;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

import library.src.main.java.com.jaredrummler.android.processes.ProcessManager;
import library.src.main.java.com.jaredrummler.android.processes.models.AndroidAppProcess;
import library.src.main.java.com.jaredrummler.android.processes.models.AndroidProcess;

/**
 * For the running process statistics, library from    https://github.com/jaredrummler/AndroidProcesses
 */


public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    ListView listView;
    List<AndroidAppProcess> processList;
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            //Get Battery %
            int level = i.getIntExtra("level", 0);
            //Find textview control created in main.xml
            TextView tv = (TextView) findViewById(R.id.textfield);
            //Set TextView with text
            tv.setText("Battery Level: " + Integer.toString(level) + "%");
        }

    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Hide the title of App
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        registerReceiver(mBatInfoReceiver, new IntentFilter(
                Intent.ACTION_BATTERY_CHANGED));

        /*
         ** get the running processes by using the customized library
         */
//        ActivityManager actvityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
//        List<ActivityManager.RunningAppProcessInfo> processList =  actvityManager.getRunningAppProcesses();
        processList = ProcessManager.getRunningAppProcesses();

        //Display the current process number and CPU usage, and memory usage
        displayGeneralInfo(processList);
        //Display the processes in the listView
        displayProcessList(processList);

        //set listener for buttons
        Button exit = (Button)findViewById(R.id.exit);
        Button refresh = (Button) findViewById(R.id.refresh);
        exit.setOnClickListener(this);
        refresh.setOnClickListener(this);


    }

    private void displayProcessList(List<AndroidAppProcess> processList)
    {
        //find the listView
        listView = (ListView)findViewById(R.id.processes_list);

        /*
         * The Customer adapter can handle List data instead of array
         * It will also display the process name and Pid in the text view.
         * Can add extra information if needed!!!!!
         */
        final CustomArrayAdapter myAdapter = new CustomArrayAdapter(this, processList);
        listView.setAdapter(myAdapter);
        setAdapterListener(myAdapter);
    }

    private void setAdapterListener(final CustomArrayAdapter myAdapter)
    {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position,
                                    long id) {
                AndroidProcess item = (AndroidProcess) myAdapter.getItem(position);
                Log.d("Process", "Process Choosen    " + item.name);
                startNewIntent(item);
            }
        });
    }

    private void startNewIntent(AndroidProcess processChosen)
    {
        Intent displayDetail = new Intent(this, ProcessDetail.class);
        displayDetail.putExtra("PID",processChosen.pid);
        displayDetail.putExtra("Name", processChosen.name);
        // These time are used to calculate CPU usage per process
        long utime = 0;
        long stime = 0;
        long cutime = 0;
        long cstime = 0;
        float uptime = 0;
        long starttime = 0;
        try {
            utime = processChosen.stat().utime();
            stime = processChosen.stat().stime();
            cutime = processChosen.stat().cutime();
            cstime = processChosen.stat().cstime();
            starttime = processChosen.stat().starttime();

        } catch (IOException e) {
            e.printStackTrace();
        }
        long total_time = utime + stime + cutime + cstime;
        uptime = readUpTime();
        displayDetail.putExtra("total_time", total_time);
        displayDetail.putExtra("start_time",starttime);
        displayDetail.putExtra("uptime", uptime);
        Log.d("start_time", String.valueOf(starttime));
        Log.d("Time elapsed", String.valueOf(uptime - starttime/100));
        startActivity(displayDetail);
    }

    /*
        The helper method to get the uptime, which is the current system time.
     */
    private float readUpTime()
    {
        RandomAccessFile reader = null;
        float uptime = 0;
        try {
            reader = new RandomAccessFile("/proc/uptime", "r");
            String load = reader.readLine();
            String[] toks = load.split(" +");  // Split on one or more spaces
            uptime = Float.parseFloat(toks[0]);
            Log.d("uptime", String.valueOf(uptime));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uptime;
    }

    private void displayGeneralInfo(List<AndroidAppProcess> processList)
    {
        displayMemInfo();
        displayCPU();
    }

    /*
        Function to display the overall CPU usage
     */
    private void displayCPU(){
        float usage = readUsage()*100;
        TextView cpuView = (TextView) findViewById(R.id.general_info);
        StringBuilder sb = new StringBuilder("Current CPU Usage: ");
        sb.append(usage);
        sb.append("%");
        cpuView.setText(sb.toString());
    }

    /*
     * Display the available memory and total memory in MB
     */
    private void displayMemInfo()
    {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableMegs = mi.availMem / 1048576L;
        long totalMem = mi.totalMem / 1048576L;
        Log.d("Memory info", "Available memory: " + availableMegs + ", Total Available " + totalMem + "," + "\n");
        TextView memoryView = (TextView) findViewById(R.id.memory_info);
        StringBuilder sb = new StringBuilder("Available memory ");
        sb.append(availableMegs).append("MB").append(" Total Available ").append(totalMem).append("MB");
        memoryView.setText(sb.toString());
    }



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
        switch (id)
        {
            case R.id.action_information:
                break;
            case R.id.action_setting:
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
        Helper function to get the current CPU usage by parsing the /proc/stat file
     */
    private float readUsage() {
        try {
            RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
            String load = reader.readLine();

            String[] toks = load.split(" +");  // Split on one or more spaces
            Log.d("1111", String.valueOf(toks[1]));
            long work1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2]) + Long.parseLong(toks[3]);
            long total1 = Long.parseLong(toks[1]) + Long.parseLong(toks[2])
                    + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[5]) + Long.parseLong(toks[6])
                    + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);
            long idle1 = Long.parseLong(toks[4]);
            long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            try {
                Thread.sleep(360);
            } catch (Exception e) {}

            reader.seek(0);
            load = reader.readLine();
            reader.close();

            toks = load.split(" +");

            long idle2 = Long.parseLong(toks[4]);
            long cpu2 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[5])
                    + Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            long work2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2])
                    + Long.parseLong(toks[3]);
            long total2 = Long.parseLong(toks[1]) + Long.parseLong(toks[2])
                    + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
                    + Long.parseLong(toks[5]) + Long.parseLong(toks[6])
                    + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

            return (float)(cpu2 - cpu1) / ((cpu2 + idle2) - (cpu1 + idle1));
//            return (float) (work2 - work1) / ((total2 - total1));

        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    private void refreshList()
    {
        processList = ProcessManager.getRunningAppProcesses();
        //Display the current process number and CPU usage, and memory usage
        displayGeneralInfo(processList);
        //Display the processes in the listView
        displayProcessList(processList);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.exit:
                finish();
                break;
            case R.id.refresh:
                refreshList();
                break;
            default:
                break;
        }
    }
}
