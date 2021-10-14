package edu.mcw.rgd;

import com.google.gson.Gson;
import edu.mcw.rgd.process.FileDownloader;
import edu.mcw.rgd.process.Utils;

import java.io.File;
import java.util.HashMap;

/**
 * User: mtutaj
 * Date: 10/14/15
 * <p>
 * geo location code for telize
 * it return json like that:
 *
 *{"dma_code":"0",
 * "ip":"128.175.240.202",
 * "asn":"AS34",
 * "city":"Newark",
 * "latitude":39.5645,
 * "country_code":"US",
 * "offset":"-4",
 * "country":"United States",
 * "region_code":"DE",
 * "isp":"University of Delaware",
 * "timezone":"America\/New_York",
 * "area_code":"0",
 * "continent_code":"NA",
 * "longitude":-75.597,
 * "region":"Delaware",
 * "postal_code":"19716",
 * "country_code3":"USA"
 * }
 **/
public class GeoLocationService_Telize {

    static private GeoLocationService_Telize _instance = null;
    static long _lastRequestTime = 0;

    static public GeoLocationService_Telize getInstance() {
        if( _instance==null )
            _instance = new GeoLocationService_Telize();

        return _instance;
    }

    private Gson gson = new Gson();
    private GeoLocationService_Telize() {

    }

    public String getName() {
        return "TELIZE";
    }

    // return array of geolocation data
    public String[] sendRequest(String ip) throws Exception {

        // execute timeout since last request
        if( _lastRequestTime!=0 ) {
            long timeElapsedInMsSinceLastRequest = System.currentTimeMillis() - _lastRequestTime;
            long cutoff = 7000;
            long sleepPeriod = cutoff;
            if( timeElapsedInMsSinceLastRequest>0 && timeElapsedInMsSinceLastRequest<cutoff )
                sleepPeriod = cutoff - timeElapsedInMsSinceLastRequest;

            // sleep 7s to be nice to this geolocation service
            //System.out.println("SLEEP "+sleepPeriod);
            Thread.sleep(sleepPeriod);
        }

        _lastRequestTime = System.currentTimeMillis();

        FileDownloader downloader = new FileDownloader();
        downloader.setExternalFile("http://www.telize.com/geoip/"+ip);
        downloader.setLocalFile("data/telize.json");
        String fileName = downloader.download();
        String json = Utils.readFileAsString(fileName);
        new File(fileName).delete();

        HashMap data = gson.fromJson(json, HashMap.class);

        //System.out.println("   "+getName()+" "+NVL(data,"ip")+" ==> "+NVL(data,"isp"));

        return new String[]{
                NVL(data, "country"),
                NVL(data, "isp"),
                NVL(data, "area_code"),
                NVL(data, "city"),
                NVL(data, "country_code"),
                NVL(data, "latitude"),
                NVL(data, "longitude"),
                NVL(data, "timezone"),
                NVL(data, "postal_code"),
                NVL(data, "region_code"),
                NVL(data, "isp"),
                NVL(data, "region"),
        };
    }

    String NVL(HashMap data, String field) {
        Object val = data.get(field);
        if( val==null )
            return "";
        else
            return val.toString();
    }
}
