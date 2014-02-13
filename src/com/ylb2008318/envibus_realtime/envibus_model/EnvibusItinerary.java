package com.ylb2008318.envibus_realtime.envibus_model;

public class EnvibusItinerary implements Comparable<Object>
{
    public EnvibusItinerary(int fromStopId, int toStopId, int lineId)
    {
        super();
        this.fromStopId = fromStopId;
        this.toStopId = toStopId;
        this.lineId = lineId;
    }

    @Override
    public String toString()
    {
        return  "<itinerary destinationCode=\"" + toStopId 
                + "\" lineId=\"" + lineId + "\" />";
    }
    
    public String toSearchString()
    {
        return  fromStopId + "$" + lineId + "$" + toStopId;
    }
    
    public EnvibusItinerary(int fromStopId)
    {
        super();
        this.fromStopId = fromStopId;
        this.toStopId = -1;
        this.lineId = -1;
    }

    public int fromStopId = -1;
    public int toStopId   = -1;
    public int lineId     = -1;
    
    @Override
    public int compareTo(Object another)
    {
        EnvibusItinerary aIti = (EnvibusItinerary) another;
        if(this.lineId < aIti.lineId)
        {
            return -1;
        }
        else if(this.lineId == aIti.lineId)
        {
            return 0;
        }
        else
        {
            return 1;
        }
    }
}
