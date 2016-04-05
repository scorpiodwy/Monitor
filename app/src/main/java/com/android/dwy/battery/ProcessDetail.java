package com.android.dwy.battery;

import android.app.ActivityManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ProcessDetail extends AppCompatActivity implements View.OnClickListener{

    float process_usage = 0;
    double memory_usage = 0;
    float uptime = 0;
    long start_time = 0;
    long total_time = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_process_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Button exit_button = (Button) findViewById(R.id.exit_button);
        exit_button.setOnClickListener(this);
        int pid = getIntent().getExtras().getInt("PID");
        String name = getIntent().getExtras().getString("Name");
//        Toast.makeText(this, name + " PID " + pid, Toast.LENGTH_SHORT).show();
        uptime = getIntent().getExtras().getFloat("uptime");
        start_time = getIntent().getExtras().getLong("start_time");
        total_time = getIntent().getExtras().getLong("total_time");

        process_usage = parseCPUUsage();
        memory_usage = getMemory(pid, name);

        TextView displayView = (TextView) findViewById(R.id.process_info);
        StringBuilder sb = new StringBuilder("The Process name:    ");
        sb.append(name).append("\n");
        sb.append("The PID    ").append(pid).append("\n");
        sb.append("The CPU usage    ").append(process_usage).append("%").append("\n");
        sb.append("The Memory usage    ").append(memory_usage).append(" MB").append("\n");
        sb.append("The battery percentage used since the app started ").append(process_usage).append("%").append("\n");
        displayView.setText(sb.toString());
    }

    /**
     * total_time = utime + stime
     * total_time = total_time + cutime + cstime
     * seconds = uptime - (starttime / Hertz)
     * cpu_usage = 100 * ((total_time / Hertz) / seconds)
     */

    private float parseCPUUsage()
    {
        float usage = 0;
        //Linux default value, sysconf(_SC_CLK_TCK) return value
        int hertz = 100;
        float seconds = uptime - start_time/hertz;
        usage = 100*((total_time/hertz)/seconds);
        return usage;
    }

    private double getMemory(int pid, String name)
    {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        int [] pids = {pid};
        android.os.Debug.MemoryInfo[] memoryInfoArray = activityManager.getProcessMemoryInfo(pids);
        android.os.Debug.MemoryInfo pidMemoryInfo = memoryInfoArray[0];

        Log.d("Memory", String.format("** MEMINFO in pid %d [%s] **\n", pids[0], name));
        Log.d("Memory", " pidMemoryInfo.getTotalPrivateDirty(): " + pidMemoryInfo.getTotalPrivateDirty() + "\n");
        Log.d("Memory", " pidMemoryInfo.getTotalPss(): " + pidMemoryInfo.getTotalPss() + "\n");
        Log.d("Memory", " pidMemoryInfo.getTotalSharedDirty(): " + pidMemoryInfo.getTotalSharedDirty() + "\n");
        return (pidMemoryInfo.getTotalPrivateDirty() + pidMemoryInfo.getTotalPss() + pidMemoryInfo.getTotalSharedDirty())/1024;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.exit_button:
                finish();
                break;
            default:
                break;
        }
    }
}
