package com.ylb2008318.envibus_realtime;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ylb2008318.envibus_realtime.EnvibusGetter.EnvibusSchedule;
import com.ylb2008318.envibus_realtime.EnvibusMapLoader.EnvibusItinerary;

public class MainActivity extends FragmentActivity
{
    public static MainActivity activity;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager            mViewPager;

    ScheduleMsgHandler   scheduleMsgHandler;

    EnvibusMapLoader     mapLoader;
    
    Spinner              stopSpinner = null; 
    ArrayAdapter<String> stopAdapter = null;
    
    String               selectedStopName = "";
    
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        activity = this;
        super.onCreate(savedInstanceState);
        
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.activity_main);
        //define custom title
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.title_layout);
        setTitleBar();
        
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(
                getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        // Read Envibus Configuration
        mapLoader = new EnvibusMapLoader();
        mapLoader.parse(getResources().openRawResource(R.raw.envibus_map));
        
        scheduleMsgHandler = new ScheduleMsgHandler();
    }



    private void setTitleBar()
    {
        ImageView refrashImage = (ImageView) findViewById(R.id.refreshImage);
        
        refrashImage.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                refrashSchedule(v);
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter
    {

        public static final String ARG_SECTION_NUMBER = "section_number";

        public SectionsPagerAdapter(FragmentManager fm)
        {
            super(fm);
        }

        @Override
        public Fragment getItem(int position)
        {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = null;
            switch (position)
            {
                case 0:
                    fragment = new FragmentGetRealTime();
                    Bundle args1 = new Bundle();
                    args1.putInt(SectionsPagerAdapter.ARG_SECTION_NUMBER,
                            position + 1);
                    fragment.setArguments(args1);
                    break;
                case 1:
                    fragment = new FragmentGetPosition();
                    Bundle args = new Bundle();
                    args.putInt(SectionsPagerAdapter.ARG_SECTION_NUMBER,
                            position + 1);
                    fragment.setArguments(args);
                    break;
                default:
                    Log.e(SectionsPagerAdapter.ARG_SECTION_NUMBER + "_outbound",
                            "Section position is :" + position);
                    break;
            }

            return fragment;
        }

        @Override
        public int getCount()
        {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position)
        {
            Locale l = Locale.getDefault();
            switch (position)
            {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section2).toUpperCase(l);
            }
            return null;
        }

        @Override
        public void finishUpdate(ViewGroup container)
        {
            super.finishUpdate(container);
        }
        
        
    }

    ReentrantReadWriteLock getScheduleStatusLock = new ReentrantReadWriteLock();
    Thread.State getScheduleStatus = Thread.State.TERMINATED;
    
    // get schedule button click
    public void refrashSchedule(View v)
    {
        if(getScheduleStatus == Thread.State.TERMINATED )
        {
            Toast.makeText(getApplicationContext(), "Retrieving Envibus Journeys",
                    Toast.LENGTH_SHORT).show();
            
            new Thread(new Runnable()
            {
                @Override
                public void run()
                {
                    List<EnvibusItinerary> iIti = mapLoader.map.getItinerariesByStopName(selectedStopName);
                    EnvibusSchedule schedule = new EnvibusGetter().getScheduleByItinerary(iIti);
//                    List<Integer> stopIds = mapLoader.map.getStopIdByStopName(selectedStopName);
//                    EnvibusSchedule schedule = new EnvibusGetter().getScheduleByStop(stopIds);
                    Message msg = new Message();
                    msg.obj = schedule;
                    MainActivity.this.scheduleMsgHandler.sendMessage(msg);
                }
            }).start();
            
            getScheduleStatusLock.writeLock().lock();
            getScheduleStatus = Thread.State.RUNNABLE;
        }
        else
        {
            Toast.makeText(getApplicationContext(), "Still retrieving",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public class ScheduleMsgHandler extends Handler
    {
        public ScheduleMsgHandler()
        {
        }

        public ScheduleMsgHandler(Looper L)
        {
            super(L);
        }

        @Override
        public void handleMessage(Message msg)
        {
            super.handleMessage(msg);
            EnvibusSchedule schedule = (EnvibusSchedule) msg.obj;
            TextView currentTimeText = (TextView) MainActivity.this
                    .findViewById(R.id.currentTimeTextView1);
            LinearLayout scheduleList = (LinearLayout) MainActivity.this
                    .findViewById(R.id.linearLayout_schedule_display);
            FragmentGetRealTime.refreshSchedule(scheduleList, schedule);

            if (currentTimeText != null)
            {
                currentTimeText.setText(DateFormat.format(EnvibusGetter.DEFAULT_TIME_FORMAT, Calendar.getInstance()));
            }

            getScheduleStatus = Thread.State.TERMINATED;
            getScheduleStatusLock.writeLock().unlock();
        }
    };
}
