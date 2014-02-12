package com.ylb2008318.envibus_realtime;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

public class EnvibusMapLoader
{
    public EnvibusMap map;

    public EnvibusMapLoader()
    {
        super();
        map = new EnvibusMap();
    }

    public class EnvibusMap
    {
        @Override
        public String toString()
        {
            String mapStr = "stops=\n";
            for(EnvibusStop eS: stops)
            {
                mapStr += eS.toString() + "\n";
            }
            
            return mapStr;
        }

        public EnvibusMap()
        {
            super();
            stops = new ArrayList<EnvibusStop>();
        }

        List<EnvibusStop> stops;

        public void removeDuplicate()
        {
            Collections.sort(map.stops);
            int lastStopId = -1;
            for(int i = 0; i < map.stops.size(); ++i)
            {
                if(lastStopId != map.stops.get(i).stopId)
                {
                    lastStopId = map.stops.get(i).stopId;
                }
                else
                {
                    map.stops.remove(i);
                    i--;
                }
            }
        }
        
        public List<EnvibusStop> getStopsByStopName(String iStopName)
        {
            List<EnvibusStop> oStops = new ArrayList<EnvibusStop>();
            
            for(int i = 0; i < map.stops.size(); ++i)
            {
                int compareResult = iStopName.compareTo(map.stops.get(i).stopName);
                if(compareResult == 0)
                {
                    oStops.add(map.stops.get(i));
                }
                else if(compareResult < 0)
                {
                    break;
                }
            }
            
            return oStops;
        }
        
        public List<EnvibusItinerary> getItinerariesByStopName(String iStopName)
        {
            List<EnvibusItinerary> oIti = new ArrayList<EnvibusItinerary>();

            List<EnvibusStop> aStops = getStopsByStopName(iStopName);
            
            for(int i = 0; i < aStops.size(); ++i)
            {
                for(int j = 0; j < aStops.get(i).itineraries.size(); ++j)
                {
                    oIti.add(aStops.get(i).itineraries.get(j));
                }
            }
            Log.d("DEBUG", "Itinerary list size: " + oIti.size());
            return oIti;
        }

        public List<String> getStopsName()
        {
            List<String> oStr = new ArrayList<String>();

            String lastStopName = "";
            
            for(int i = 0; i < map.stops.size(); ++i)
            {
                int compareResult = lastStopName.compareTo(map.stops.get(i).stopName);
                if(compareResult != 0)
                {
                    lastStopName = map.stops.get(i).stopName;
                    oStr.add(map.stops.get(i).stopName);
                }
            }
            return oStr;
        }

        public List<String> getAvailableStopsName()
        {
            List<String> oStr = new ArrayList<String>();

            String lastStopName = "";
            
            for(int i = 0; i < map.stops.size(); ++i)
            {
                int compareResult = lastStopName.compareTo(map.stops.get(i).stopName);
                if((compareResult != 0) && (map.stops.get(i).itineraries.size() > 0))
                {
                    lastStopName = map.stops.get(i).stopName;
                    oStr.add(map.stops.get(i).stopName);
                }
            }
            return oStr;
        }

        public List<Integer> getStopIdByStopName(String iStopName)
        {
            List<Integer> oStopIds = new ArrayList<Integer>();

            List<EnvibusStop> aStops = getStopsByStopName(iStopName);
            
            for(EnvibusStop s : aStops)
            {
                oStopIds.add(s.stopId);
            }
            return oStopIds;
        }
    }

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

    public class EnvibusItinerary implements Comparable<Object>
    {
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

    public EnvibusMap parse(InputStream iStream)
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(iStream);
            Element root = doc.getDocumentElement();

            NodeList stopList = root.getElementsByTagName("Stop");
            for ( int j = 0; j < stopList.getLength(); j++ )
            {
                Element aStop = (Element) stopList.item(j);
                EnvibusStop aEnvibusStop = new EnvibusStop();
                aEnvibusStop.stopId = Integer.parseInt(
                        aStop.getAttribute("stopId"));
                aEnvibusStop.stopName = aStop.getAttribute("stopName");
                NodeList itiList = aStop.getElementsByTagName("itinerary");
                for ( int k = 0; k < itiList.getLength(); k++ )
                {
                    Element aIti = (Element) itiList.item(k);
                    aEnvibusStop.addItinerary(
                        Integer.parseInt(aIti.getAttribute("destinationCode")), 
                        Integer.parseInt(aIti.getAttribute("lineId")));
                }
                map.stops.add(aEnvibusStop);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        map.removeDuplicate();
        return map;
    }
}
