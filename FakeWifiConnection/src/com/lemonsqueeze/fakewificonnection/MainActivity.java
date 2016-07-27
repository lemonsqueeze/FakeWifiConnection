package com.lemonsqueeze.fakewificonnection;

//import android.content.pm.*;
//import android.app.*;
//import java.util.*;
//import android.os.*;
//import android.view.*;
//import android.util.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

//import android.content.*;
//import android.net.*;

public class MainActivity extends Activity
{
  class PInfo {
      private String appname = "";
      private String pname = "";
  }

  SharedPreferences pref;  
  ListView app_list;		//listview with checkboxes which will contain apps
  Switch masterSwitch;
    Button scheduleChooser;
    
  ArrayList<PInfo> pinfos;	//PInfo object for each app
    
  @Override
  protected void onCreate(Bundle savedInstanceState) 
  {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      pref = getSharedPreferences("pref", Context.MODE_WORLD_READABLE);
      app_list = (ListView) findViewById(R.id.appList);

      pinfos = getInstalledApps(true);
      //sort the pinfo objects by name
      Collections.sort(pinfos, new Comparator<PInfo>()
      {
	  @Override
	  public int compare(PInfo lhs, PInfo rhs)
	  {  return lhs.appname.compareTo(rhs.appname); }
      });

      //add apps to installed_apps list
      ArrayList<String> installed_apps = new ArrayList<String>();
      for (int i = 0; i < pinfos.size(); i++)
	  installed_apps.add(pinfos.get(i).appname);      
      
      ArrayAdapter<String> adapter = new ArrayAdapter<String>(
	  this, 
	  android.R.layout.simple_list_item_multiple_choice,
	  installed_apps);
      app_list.setAdapter(adapter);
	
      app_list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

      load();
  }

  public ArrayList<PInfo> getInstalledApps(boolean getSysPackages)
  {
      ArrayList<PInfo> res = new ArrayList<PInfo>();        
      List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
      for (int i = 0; i < packs.size(); i++)
      {
	  PackageInfo p = packs.get(i);
	  if ((!getSysPackages) && (p.versionName == null)) 
	      continue;
	  PInfo newInfo = new PInfo();
	  newInfo.appname = p.applicationInfo.loadLabel(getPackageManager()).toString();
	  newInfo.pname = p.packageName;
	  res.add(newInfo);
      }
      return res;
  }
    
  //save all apps' names along with true or false for ticked or not ticked
  public void save(View v)
  {
      Editor editor = pref.edit();
      for (int i = 0; i < pinfos.size(); i++)
	  editor.putBoolean(pinfos.get(i).pname, app_list.isItemChecked(i));
      editor.putBoolean("master", this.masterSwitch.isChecked());
      editor.putInt("debug_level", pref.getInt("debug_level", 0));
      editor.apply();
      Toast.makeText(this, "Settings saved. Normal apps should pick up changes immediately.", Toast.LENGTH_LONG).show();
      finish();
  }

  //invert selected
  public void invert(View v)
  {
      for (int i = 0; i < pinfos.size(); i++)      
	  app_list.setItemChecked(i, !app_list.isItemChecked(i));
  }

  public void selectAll(View v)
  {
      for (int i = 0; i < pinfos.size(); i++)      
	    app_list.setItemChecked(i, true);
  }
    
  //load ticked or not from previous pref file
  public void load() 
  {
      for (int i = 0; i < pinfos.size(); i++)      
	  app_list.setItemChecked(i, pref.getBoolean(pinfos.get(i).pname, false));
      
      masterSwitch = (Switch) findViewById(R.id.masterswitch);
      masterSwitch.setChecked(pref.getBoolean("master", true));

      scheduleChooser = (Button) findViewById(R.id.scheduleChooser);
      loadSchedule();
      scheduleChooser.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
              scheduleClick();
          }
      });

      scheduleChooser.setOnLongClickListener(new View.OnLongClickListener() {
          @Override
          public boolean onLongClick(View v) {
              pref.edit()
                      .putInt("scheduleStartHour", -1)
                      .putInt("scheduleStartMinute", -1)
                      .putInt("scheduleEndHour", -1)
                      .putInt("scheduleEndMinute", -1)
                      .apply();
              loadSchedule();
              return true;
          }
      });
  }

    private int scheduleStartHour = -1;
    private int scheduleStartMinute = -1;
    private int scheduleEndHour = -1;
    private int scheduleEndMinute = -1;

    private void loadSchedule() {
        scheduleStartHour = pref.getInt("scheduleStartHour", -1);
        scheduleStartMinute = pref.getInt("scheduleStartMinute", -1);
        scheduleEndHour = pref.getInt("scheduleEndHour", -1);
        scheduleEndMinute = pref.getInt("scheduleEndMinute", -1);

        if (scheduleStartHour != -1) {
            scheduleChooser.setText("Scheduled from " +
                    scheduleStartHour + ":" + String.format("%02d", scheduleStartMinute) + " to " +
                    scheduleEndHour + ":" + String.format("%02d", scheduleEndMinute));
        } else {
            scheduleChooser.setText("Schedule");
        }
    }

    private void scheduleClick() {
        TimePickerDialog startTimePicker = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                final int newStartHour = hourOfDay;
                final int newStartMinute = minute;

                TimePickerDialog endTimePicker = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        scheduleStartHour = newStartHour;
                        scheduleStartMinute = newStartMinute;
                        scheduleEndHour = hourOfDay;
                        scheduleEndMinute = minute;

                        pref.edit()
                                .putInt("scheduleStartHour", scheduleStartHour)
                                .putInt("scheduleStartMinute", scheduleStartMinute)
                                .putInt("scheduleEndHour", scheduleEndHour)
                                .putInt("scheduleEndMinute", scheduleEndMinute)
                                .apply();

                        Toast.makeText(MainActivity.this, "Schedule is set. Long tap the button to clear.", Toast.LENGTH_LONG).show();

                        loadSchedule();
                    }
                }, scheduleEndHour != -1 ? scheduleEndHour : 0, scheduleEndMinute != -1 ? scheduleEndMinute : 0, true);
                endTimePicker.setTitle("Choose end time:");
                endTimePicker.show();
            }
        }, scheduleStartHour != -1 ? scheduleStartHour : 0, scheduleStartMinute != -1 ? scheduleStartMinute : 0, true);
        startTimePicker.setTitle("Choose start time:");
        startTimePicker.show();
  }
    
  //prompt to prevent quit without save
  @Override
  public void onBackPressed()
  {
      exitPrompt();
  }
    
  public void exitPrompt()
  {
      AlertDialog.Builder alert = new AlertDialog.Builder(this);
      alert.setTitle("Are you sure you want to leave this page?");
      alert.setMessage("You haven't saved your settings");
      alert.setPositiveButton("Stay on this page", new OnClickListener()
      {
	  @Override
	  public void onClick(DialogInterface dialog, int which)
	  {     }
      });
      alert.setNegativeButton("Leave!", new OnClickListener()
      {
	  @Override
	  public void onClick(DialogInterface dialog, int which)
	  {  finish(); }
      });
      alert.show();
  }
    

}
