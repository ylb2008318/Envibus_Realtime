package com.ylb2008318.envibus_realtime.envibus_model;

import java.util.ArrayList;
import java.util.List;


public class EnvibusStop implements Comparable<Object>
{
    @Override
    public String toString()
    {
        String stopStr = "<Stop stopId=\"" + stopId
                + "\" stopName=\"" + stopName + "\" longitude=\"" + longitude 
                + "\" latitude=\"" + latitude + "\">";
        
        for(int i = 0; i < itineraries.size(); ++i)
        {
            stopStr += "\n    " + itineraries.get(i).toString();
        }
        stopStr+= "</Stop>";
        
        return stopStr;
    }

    public int                    stopId    = -1;
    public String                 stopName  = "";
    public double                 longitude = 0.0;
    public double                 latitude  = 0.0;
    public List<EnvibusItinerary> itineraries;

    public EnvibusStop()
    {
        super();
        this.stopId = -1;
        this.stopName = "";
        this.longitude = 0.0;
        this.latitude = 0.0;
        itineraries = new ArrayList<EnvibusItinerary>();
    }

    public void addItinerary(int iToStopId, int iLineId)
    {
        EnvibusItinerary aItinerary = new EnvibusItinerary(this.stopId);
        aItinerary.toStopId = iToStopId;
        aItinerary.lineId = iLineId;
        this.itineraries.add(aItinerary);
    }

    @Override
    public int compareTo(Object another)
    {
        EnvibusStop aStop = (EnvibusStop) another;
        int result = this.stopName.compareTo(aStop.stopName);
        if(result == 0)
        {
            if(this.stopId < aStop.stopId)
            {
                return -1;
            }
            else if(this.stopId == aStop.stopId)
            {
                return 0;
            }
            else
            {
                return 1;
            }
        }
        else
        {
            return result;
        }
    }
}