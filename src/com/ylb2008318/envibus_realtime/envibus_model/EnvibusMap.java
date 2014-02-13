package com.ylb2008318.envibus_realtime.envibus_model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

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
        Collections.sort(this.stops);
        int lastStopId = -1;
        for(int i = 0; i < this.stops.size(); ++i)
        {
            if(lastStopId != this.stops.get(i).stopId)
            {
                lastStopId = this.stops.get(i).stopId;
            }
            else
            {
                this.stops.remove(i);
                i--;
            }
        }
    }
    
    public List<EnvibusStop> getStopsByStopName(String iStopName)
    {
        List<EnvibusStop> oStops = new ArrayList<EnvibusStop>();
        
        for(int i = 0; i < this.stops.size(); ++i)
        {
            int compareResult = iStopName.compareTo(this.stops.get(i).stopName);
            if(compareResult == 0)
            {
                oStops.add(this.stops.get(i));
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
        return oIti;
    }

    public List<String> getStopsName()
    {
        List<String> oStr = new ArrayList<String>();

        String lastStopName = "";
        
        for(int i = 0; i < this.stops.size(); ++i)
        {
            int compareResult = lastStopName.compareTo(this.stops.get(i).stopName);
            if(compareResult != 0)
            {
                lastStopName = this.stops.get(i).stopName;
                oStr.add(this.stops.get(i).stopName);
            }
        }
        return oStr;
    }

    public List<String> getAvailableStopsName()
    {
        List<String> oStr = new ArrayList<String>();

        String lastStopName = "";
        
        for(int i = 0; i < this.stops.size(); ++i)
        {
            int compareResult = lastStopName.compareTo(this.stops.get(i).stopName);
            if((compareResult != 0) && (this.stops.get(i).itineraries.size() > 0))
            {
                lastStopName = this.stops.get(i).stopName;
                oStr.add(this.stops.get(i).stopName);
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
    
    public static EnvibusMap parse(InputStream iStream)
    {
        EnvibusMap oMap = new EnvibusMap();
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
                aEnvibusStop.latitude = Double.parseDouble(aStop.getAttribute("latitude"));
                aEnvibusStop.longitude = Double.parseDouble(aStop.getAttribute("longitude"));
                
                NodeList itiList = aStop.getElementsByTagName("itinerary");
                for ( int k = 0; k < itiList.getLength(); k++ )
                {
                    Element aIti = (Element) itiList.item(k);
                    aEnvibusStop.addItinerary(
                        Integer.parseInt(aIti.getAttribute("destinationCode")), 
                        Integer.parseInt(aIti.getAttribute("lineId")));
                }
                oMap.stops.add(aEnvibusStop);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        
        oMap.removeDuplicate();
        return oMap;
    }
}
