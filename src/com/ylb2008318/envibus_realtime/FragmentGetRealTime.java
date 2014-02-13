package com.ylb2008318.envibus_realtime;

import java.util.Calendar;
import java.util.List;

import com.ylb2008318.envibus_realtime.EnvibusGetter.EnvibusSchedule;
import com.ylb2008318.envibus_realtime.EnvibusGetter.StopPoint;
import com.ylb2008318.envibus_realtime.EnvibusGetter.VehicleJourneyAtStop;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentGetRealTime extends Fragment
{
    /**
     * The fragment argument representing the section number for this fragment.
     */
    View rootView;
    
    private Button buttonGetSchedule = null;

    public FragmentGetRealTime()
    {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState)
    {
        rootView = inflater.inflate(R.layout.fragment_get_realtime,
                container, false);
        setSpinner();
        return rootView;
    }
    
    protected void setSpinner()
    {
        final MainActivity A = MainActivity.activity;
                
        final List<String> availableStopsName = A.map.getAvailableStopsName();        
        
        Spinner stopSpinner = (Spinner) rootView.findViewById(R.id.spinner_stop);
        
        if((availableStopsName.size() > 0) && (stopSpinner != null))
        {
            ArrayAdapter<String> stopAdapter = new ArrayAdapter<String>(
                    rootView.getContext(),
                    android.R.layout.simple_spinner_item, availableStopsName);
            stopSpinner.setAdapter(stopAdapter);
            stopSpinner.setSelection(0,true);
            A.selectedStopName = availableStopsName.get(0);
            
            stopSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long arg3)
                {
                    A.selectedStopName = availableStopsName.get(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0)
                {
                }
            });
        }
    }
    
    public static void fillSchedule(LinearLayout iScheduleLayout, EnvibusSchedule iSchedule)
    {
        if(iSchedule.status != Status.OK)
        {
            // Add Status Line
            addStatus(iScheduleLayout, iSchedule.status);
        }
        else
        {
            // No need to show the status
            // addStatus(iScheduleLayout, iSchedule.status);
            
            // Loop Journeys
            int lastStopCode = -1;
            
            int i = 0;
            
            for(VehicleJourneyAtStop v : iSchedule.journeys)
            {  
                
                if(lastStopCode != v.stopPoint.id)
                {
                    // Update Stop, Insert Line
                    lastStopCode = v.stopPoint.id;
                    addStopInfo(iScheduleLayout, v.stopPoint);
                    i = 0;
                }
                
                // Read Journey Info and Fill
                if(v.delay > 0)
                {
                    LayoutInflater flater = LayoutInflater.from(iScheduleLayout.getContext());
                    RelativeLayout stopLine = (RelativeLayout) flater.inflate(R.layout.journey_layout, null);
                    ((TextView) stopLine.findViewById(R.id.lineName)).setText(v.line.lineName);
                    ((TextView) stopLine.findViewById(R.id.destinationName)).setText(v.destination.stopName);
                    ((TextView) stopLine.findViewById(R.id.delay)).setText(String.valueOf(v.delay));
                    ((TextView) stopLine.findViewById(R.id.delayConcat2)).setText((v.delay == 1)?" minute":" minutes");
                    ((TextView) stopLine.findViewById(R.id.arriveTime)).setText(
                            DateFormat.format(EnvibusGetter.HOUR_MINUTE_FORMAT, v.passingTime));
                    ((ViewGroup) iScheduleLayout).addView(stopLine);
                    if(i%2 == 0)
                    {
                        stopLine.setBackgroundColor(
                                iScheduleLayout.getContext().getResources().getColor(R.color.light_grayEFEFEF));
                    }
                    i++;
                }
            }  
        }
    }
    
    private static void addStopInfo(View ioView, StopPoint iS)
    {
        LayoutInflater flater = LayoutInflater.from(ioView.getContext());
        LinearLayout stopLine = (LinearLayout) flater.inflate(R.layout.bus_stop_layout, null);
        ((TextView) stopLine.findViewById(R.id.stopName)).setText(iS.stopName);
        ((TextView) stopLine.findViewById(R.id.stopId)).setText(String.valueOf(iS.id));
        ((ViewGroup) ioView).addView(stopLine);
    }
    
    private static void addStatus(View ioView, Status iS)
    {
        LayoutInflater flater = LayoutInflater.from(ioView.getContext());
        LinearLayout statusLine = (LinearLayout) flater.inflate(R.layout.status_layout, null);
        ((TextView) statusLine.findViewById(R.id.statusCode)).setText(""+iS);
        if(iS == Status.NO_HOURS)
        {
            ((TextView) statusLine.findViewById(R.id.statusCode)).setTextColor(
                    ioView.getContext().getResources().getColor(R.color.warning_color));
        }
        else if((iS == Status.UNKNOWN) && (iS == Status.TECH_ERR))
        {
            ((TextView) statusLine.findViewById(R.id.statusCode)).setTextColor(
                    ioView.getContext().getResources().getColor(R.color.error_color));
        }
        ((ViewGroup) ioView).addView(statusLine);
    }


    public static void cleanSchedule(LinearLayout iScheduleList) 
    {
        if(iScheduleList.getId() == R.id.linearLayout_schedule_display)
        {
            iScheduleList.removeAllViews();
        }
    }
    
    public static void refreshSchedule(LinearLayout iScheduleList, EnvibusSchedule iSchedule)
    {
        cleanSchedule(iScheduleList);
        fillSchedule(iScheduleList, iSchedule);
    }
}