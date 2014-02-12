package com.ylb2008318.envibus_realtime;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.xml.sax.SAXException;

import com.ylb2008318.envibus_realtime.EnvibusMapLoader.EnvibusItinerary;

import android.os.NetworkOnMainThreadException;
import android.text.format.DateFormat;
import android.util.Log;

enum Status {OK, NO_HOURS, TECH_ERR, UNKNOWN}

public class EnvibusGetter
{

    public static final String DEFAULT_TIME_FORMAT = "yyyy/MM/dd HH:mm:ss";
    public static final String HOUR_MINUTE_FORMAT = "HH:mm";
    public static final String ARRIVE_TIME_FORMAT = "yyyy/MM/dd HH:mm";
    
    public EnvibusGetter()
    {
        super();

    }

    // part 1 get result from server
    protected String result;
    
    protected String uID = "CA06SA";

    public EnvibusSchedule getScheduleByItinerary(List<EnvibusItinerary> iIti)
    {
        EnvibusSchedule ret = null;
        String filterKeys = "";
        for(int i = 0; i < iIti.size(); ++i)
        {
            filterKeys += iIti.get(i).toSearchString();
            if(i != iIti.size()-1)
            {
                filterKeys += ",";
            }
        }
        
        try
        {
            // Add your data
            HttpClient httpclient = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost(
                    "http://envibus.tsi.cityway.fr/CasaSIRIClient/GetStopTimetable.aspx?uID="
                    + uID + "&filterKeys=" + filterKeys);
            
            Log.d("DEBUG", httppost.getURI().toString());
            
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity httpEntity = response.getEntity();
            result = new String(EntityUtils.toString(httpEntity).getBytes(),
                    "UTF-16LE");
            result = cleanResponse(result);
            ret = decodeXmlResponse(result);
        }
        catch (NetworkOnMainThreadException e)
        {
            Log.e("ERROR", e.toString());
        }
        catch (IOException e)
        {
            Log.e("ERROR", e.toString());
        }
        return ret;
    }
    
    public EnvibusSchedule getScheduleByStop(List<Integer> iStopIds)
    {
        EnvibusSchedule ret = null;
        String filterKeys = "";
        for(int i = 0; i < iStopIds.size(); ++i)
        {
            filterKeys += String.valueOf(iStopIds.get(i));
            if(i != iStopIds.size()-1)
            {
                // to form id1$id2$id3
                filterKeys += "$";
            }
        }
        
        try
        {
            // Add your data
            HttpClient httpclient = new DefaultHttpClient();
            
            HttpPost httppost = new HttpPost(
                    "http://envibus.tsi.cityway.fr/CasaSIRIClient/GetLinesByStopIds.aspx?uID="
                    + uID + "&stopIds=" + filterKeys);
            
            Log.d("DEBUG", httppost.getURI().toString());
            
            HttpResponse response = httpclient.execute(httppost);

            HttpEntity httpEntity = response.getEntity();
            result = new String(EntityUtils.toString(httpEntity).getBytes(),
                    "UTF-16LE");
            result = cleanResponse(result);
            ret = decodeXmlResponse(result);
        }
        catch (NetworkOnMainThreadException e)
        {
            Log.e("ERROR", e.toString());
        }
        catch (IOException e)
        {
            Log.e("ERROR", e.toString());
        }
        return ret;
    }

    private String cleanResponse(String result)
    {
        while ((!result.startsWith("<")) && (result.length() > 0))
        {
            result = result.substring(1);
        }
        return result;
    }

    // part 2 decode response
    // Define Schedule Classes
    public String retrieveNodeValue(Element aElt, String nodeName)
    {
        String ret = "";
        Element aNode = (Element) aElt.getElementsByTagName(nodeName).item(0);
        if(aNode != null)
        {
            Node aNodeChild = aNode.getFirstChild();
            if(aNodeChild != null)
            {
                ret = aNodeChild.getNodeValue();
                Log.d("Debug", "Find " + nodeName + ": " + ret);
            }
        }
        else
        {
            Log.d("Debug", "Can't Find Node : " + nodeName);
        }
        return ret;
    }
    
    public class EnvibusSchedule
    {
        @Override
        public String toString()
        {
            return "EnvibusSchedule [status=" + status + ",\n journeys=\n"
                    + journeys + "\n]";
        }

        Status status;
        List<VehicleJourneyAtStop> journeys;
        
        public EnvibusSchedule()
        {
            super();
            status = Status.UNKNOWN;
            journeys = new ArrayList<VehicleJourneyAtStop>();
        }

        public void parseStatus(Element statusNode)
        {
            String statusCode = retrieveNodeValue(statusNode, "code");
            if(statusCode.equals("0"))
            {
                this.status = Status.OK;
            }
            else if(statusCode.equals("-2"))
            {
                this.status = Status.NO_HOURS;
            }
            Log.i("Info", "Find Code : " + this.status);
        }
    }
    
    public class VehicleJourneyAtStop implements Comparable<Object>
    {
        @Override
        public String toString()
        {
            return "From " + stopPoint.toString() + " to " +destination.toString() + 
                    " line " + line.toString() + ", in " + delay + " minute(s), at " + DateFormat.format(ARRIVE_TIME_FORMAT, passingTime) + "\n";
        }

        public VehicleJourneyAtStop()
        {
            super();
            stopPoint = new StopPoint();
            recordedAtTime = Calendar.getInstance();
            passingTime = Calendar.getInstance();
            delay = 0;
            line = new Line();
            destination = new StopPoint();
        }

        public StopPoint stopPoint;  // stop Name
        public Calendar recordedAtTime;
        public Calendar passingTime;
        public int       delay;      // the bus will come in X minutes
        public Line      line;
        public StopPoint destination;
        
        public void parse(Element journeyNode)
        {
            Element stopNode = (Element) journeyNode.getElementsByTagName("stopPoint").item(0);
            if(stopNode != null)
            {
                this.stopPoint.parse(stopNode);
            }
            
            Element recordTimeNode = (Element) journeyNode.getElementsByTagName("recordedAtTime").item(0);
            if(recordTimeNode != null)
            {
                this.parseRecordedTime(true, recordTimeNode);
            }
            
            Element passingNode = (Element) journeyNode.getElementsByTagName("passingTime").item(0);
            if(passingNode != null)
            {
                this.parseRecordedTime(false, passingNode);
            }
            
            Element waitingTimeNode = (Element) journeyNode.getElementsByTagName("waitingTime").item(0);
            
            if(waitingTimeNode != null)
            {
                String waitingTimeStr = waitingTimeNode.getAttribute("minute");
                if(waitingTimeStr != null)
                {
                    this.computateDelay();
                    int aTempDelay = Integer.parseInt(waitingTimeStr);
                    if(aTempDelay < 0)
                    {
                        aTempDelay += 1440;
                    }
                    Log.i("Info", "Temp Waiting time is :" + aTempDelay);
                }
            }
            
            Element lineNode = (Element) journeyNode.getElementsByTagName("line").item(0);
            if(lineNode != null)
            {
                this.line.parse(lineNode);
            }
            
            Element destinationNode = (Element) journeyNode.getElementsByTagName("journeyPattern").item(0);
            if(destinationNode != null)
            {
                this.destination.parse(destinationNode);
            }
        }

        private void computateDelay()
        {
            if(this.recordedAtTime.before(this.passingTime))
            {
                long diff = this.passingTime.getTimeInMillis() - this.recordedAtTime.getTimeInMillis();
                this.delay = (int) Math.ceil(diff/60000);
                
                if(this.delay > 30)
                {
                    diff = this.passingTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
                    this.delay = (int) Math.ceil(diff/60000);
                }
                Log.i("Info", "Delay is :" + this.passingTime.getTimeInMillis() + " - " + this.recordedAtTime.getTimeInMillis() + " = " + this.delay);
            }
            else
            {
                Log.e("ERROR", "Time parse is not correcte");
            }
        }

        private void parseRecordedTime(boolean updateRecordTime, Element timeNode)
        {
            Calendar aNewDate = Calendar.getInstance();
            String sHour = timeNode.getAttribute("hour");
            String sMinute = timeNode.getAttribute("minute");
            
            if((!sHour.isEmpty()) && (!sMinute.isEmpty()))
            {
                aNewDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sHour));
                aNewDate.set(Calendar.MINUTE, Integer.parseInt(sMinute));
                if(updateRecordTime)
                {
//                    aNewDate.set(Calendar.SECOND, 0);
//                    Log.i("Info", "Execute Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, this.recordedAtTime));
//                    Log.i("Info", "temp Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, aNewDate));
//                    switch(aNewDate.compareTo(this.recordedAtTime))
//                    {
//                        case -1:
//                        {
//                            // this is Ok, update recordedAtTime
//                            this.recordedAtTime = aNewDate;
//                            break;
//                        }
//                        case 1:
//                        {
//                            // this is KO, aNewDate is not the real recordate
//                            if((aNewDate.getTimeInMillis() - this.recordedAtTime.getTimeInMillis()) > 300000)
//                            {
//                                aNewDate.add(Calendar.DATE, -1);
//                            }
//                            this.recordedAtTime = aNewDate;
//                            break;
//                        }
//                    }
                    Log.i("Info", "Record Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, this.recordedAtTime));
                }
                else
                {
                    aNewDate.set(Calendar.SECOND, 59);
                    Log.i("Info", "Execute Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, this.passingTime));
                    Log.i("Info", "temp Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, aNewDate));
                    switch(aNewDate.compareTo(this.passingTime))
                    {
                        case -1:
                        {
                            // this is KO, aNewDate is not the real passingTime
                            aNewDate.add(Calendar.DATE, 1);
                            this.passingTime = aNewDate;
                            break;
                        }
                        case 1:
                        {
                            // this is OK, update passingTime
                            this.passingTime = aNewDate;
                            break;
                        }
                    }
                    Log.i("Info", "Passing Time is :" + DateFormat.format(DEFAULT_TIME_FORMAT, this.passingTime));
                }
            }
        }
        
        @Override
        public int compareTo(Object arg0) {
            VehicleJourneyAtStop oJ = (VehicleJourneyAtStop)arg0;
            
            if (this.stopPoint.compareTo(oJ.stopPoint) == 0) {
                if(this.line.compareTo(oJ.line) == 0)
                {
                    if(this.destination.compareTo(destination) == 0)
                    {
                        if(this.delay < oJ.delay)
                        {
                            return -1;
                        }
                        else if(this.delay == oJ.delay)
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
                        return this.destination.compareTo(destination);
                    }
                }
                else
                    return this.line.compareTo(oJ.line);
            }
            else 
                return this.stopPoint.compareTo(oJ.stopPoint);
        }
        
        
    }

    public class StopPoint implements Comparable<Object>
    {
        @Override
        public String toString()
        {
            return stopName + "(" + id + ")";
        }

        public int    id;
        public String stopName;

        public StopPoint()
        {
            super();
            id = 0;
            stopName = "Undefined";
        }

        public void parse(Element stopNode)
        {
            String stopId = retrieveNodeValue(stopNode, "id");
            this.id = Integer.parseInt(stopId);
            
            String stopName = retrieveNodeValue(stopNode, "name");
            if(stopName.isEmpty())
            {
                this.stopName = retrieveNodeValue(stopNode, "destination");
            }
            else
            {
                this.stopName = stopName;
            }
        }

        @Override
        public int compareTo(Object another)
        {
            StopPoint oS = (StopPoint)another;
            if(this.stopName.compareTo(oS.stopName) == 0)
            {
                if(this.id < oS.id)
                {
                    return -1;
                }
                else if(this.id == oS.id)
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
            else
                return this.stopName.compareTo(oS.stopName);
        }
    }

    public class Line implements Comparable<Object>
    {
        @Override
        public String toString()
        {
            return lineName + "(" + id + ")";
        }

        public Line()
        {
            super();
            id = 0;
            lineName = "Undefined";
        }

        public void parse(Element lineNode)
        {
            String lineId = retrieveNodeValue(lineNode, "id");
            this.id = Integer.parseInt(lineId);
            
            String lineName = retrieveNodeValue(lineNode, "name");
            if(!lineName.isEmpty())
            {
                this.lineName = lineName;
            }
        }

        public int    id;
        public String lineName;
        
        @Override
        public int compareTo(Object another)
        {
            Line oL = (Line)another;
            return this.lineName.compareTo(oL.lineName);
        }
    }

    public EnvibusSchedule decodeXmlResponse(String in) throws IOException
    {
        InputStream streamIn = new ByteArrayInputStream(in.getBytes("UTF-16LE"));
        EnvibusSchedule schedule = new EnvibusSchedule();
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document doc = builder.parse(streamIn);
            Element root = doc.getDocumentElement();

            NodeList list = root.getElementsByTagName("Status");
            if(list.getLength() == 1)
            {
                Log.d("Debug", "Find Status");
                schedule.parseStatus((Element)list.item(0));
            }
            
            Element timetableNode = (Element) root.getElementsByTagName("StopRealTimetable").item(0);
            if(timetableNode != null)
            {
                Log.d("Debug", "Find StopRealTimetable");
                
                list = timetableNode.getElementsByTagName("VehicleJourneyAtStop");

                for(int i = 0; i < list.getLength(); ++i)
                {
                    Element journeyNode = (Element) list.item(i);
                    VehicleJourneyAtStop vehicleJourneyAtStop = new VehicleJourneyAtStop();
                    vehicleJourneyAtStop.parse(journeyNode);
                    schedule.journeys.add(vehicleJourneyAtStop);
                }
                
                Collections.sort(schedule.journeys);
            }
        }
        catch (IOException e)
        {
            Log.e("ERROR", e.toString());
        }
        catch (ParserConfigurationException e)
        {
            Log.e("ERROR", e.toString());
        }
        catch (SAXException e)
        {
            Log.e("ERROR", e.toString());
        }
        finally
        {
            streamIn.close();
        }

        return schedule;
    }
}
