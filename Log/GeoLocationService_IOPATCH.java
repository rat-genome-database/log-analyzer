package Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 6/5/13
 * Time: 1:36 PM
 * geo location service from ipatch.com
 * API CODE: MCW-20130530-74485
 * POST request to http://iopatch.com/api/index.php?key=xxxxx&ip=000.000.000.000
 *
 * API returns a plain text string with several comma separated fields
 * f.e.: UnitedStates,Medical College of Wisconsin,212,NewYork,US,40.7494,-73.9758,501,10158,NY,Medical College of Wisconsin,NewYork
 * fields:
 *0   country_name: The country name.
 *1   isp: Provider name.
 *2   areacode: Area code.
 *3   city: City.
 *4   country: Country code.
 *5   latitude: Latitude.
 *6   longitude: Longitude.
 *7   metrocode: Metro code.
 *8   postalcode: Postal code.
 *9   region: Region code.
 *10   organization: Organization or corporation.
 *11   region_name: State or province.
 */
public class GeoLocationService_IOPATCH {

    static private GeoLocationService_IOPATCH _instance = null;
    static long _lastRequestTime = 0;

    static public GeoLocationService_IOPATCH getInstance() {
        if( _instance==null )
            _instance = new GeoLocationService_IOPATCH();

        return _instance;
    }

    private GeoLocationService_IOPATCH() {

    }

    public String getName() {
        return "TELIZE";
    }

    // return array of geolocation data
    public String[] sendRequest(String ip) throws Exception {

        // execute timeout since last request
        if( _lastRequestTime!=0 ) {
            long timeElapsedInMsSinceLastRequest = System.currentTimeMillis() - _lastRequestTime;
            long cutoff = 5000;
            long sleepPeriod = cutoff;
            if( timeElapsedInMsSinceLastRequest>0 && timeElapsedInMsSinceLastRequest<cutoff )
                sleepPeriod = cutoff - timeElapsedInMsSinceLastRequest;

            // sleep 5s to be nice to this geolocation service (requests faster than 4sec are considered spam)
            //System.out.println("SLEEP "+sleepPeriod);
            Thread.sleep(sleepPeriod);
        }

        URL url = new URL ("http://iopatch.com/api/index.php");
        URLConnection urlConn = url.openConnection();
        // Let the run-time system (RTS) know that we want input.
        urlConn.setDoInput (true);
        // Let the RTS know that we want to do output.
        urlConn.setDoOutput (true);
        // No caching, we want the real thing.
        urlConn.setUseCaches (false);
        // Specify the content type.
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Send POST output.
        DataOutputStream printout = new DataOutputStream (urlConn.getOutputStream ());
        String content =
            "key=" + URLEncoder.encode("MCW-20130530-74485") +
            "&ip=" + URLEncoder.encode (ip);
        printout.writeBytes (content);
        printout.flush ();
        printout.close ();

        // Get response data.
        DataInputStream input = new DataInputStream (urlConn.getInputStream ());
        String[] data = null;
        String str;
        while (null != ((str = input.readLine()))) {
            System.out.println("IOPATCH "+ip+"> "+str);
            data = normalizeData(str);
        }
        input.close ();

        _lastRequestTime = System.currentTimeMillis();

        return data;
    }

    String[] normalizeData(String str) {

        // count number of commas; if it is more than 11, some fields must be combined
        int commaCount = countCommas(str);
        if( commaCount!=11 ) {
            str = str.replace(", ", "; ");
            commaCount = countCommas(str);
            if( commaCount!=11 ) {
                System.out.println("comma count logic error");
            }
        }
        String[] data = str.split(",");

        for( int i=0; i<data.length; i++ ) {
            if( data[i].equals("Unknowed") )
                data[i] = "";
        }
        return data;
    }

    int countCommas(String str) {
        int commaCount = 0;
        int pos = 0;
        while( (pos=str.indexOf(',', pos)) > 0 ) {
            commaCount++;
            pos++;
        }
        return commaCount;
    }
}
