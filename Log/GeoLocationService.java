package Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/30/13
 * Time: 8:39 AM
 * <p>
 *     Service that for given ip address gives location and provider (ISP) information
 * </p>
 */
public class GeoLocationService {

    private static GeoLocationService _instance = null;

    private List<GeoData> geoLocations;
    private List<GeoData> geoProviders;

    public static synchronized GeoLocationService getInstance() throws Exception {

        if( _instance==null ) {
            _instance = new GeoLocationService();
        }
        return _instance;
    }

    public String getName() {
        return "GeoLite";
    }

    private GeoLocationService() throws Exception {

        // load map of country codes
        // f.e. 'CN' --> 'China'
        Map<String, String> countryMap = loadCountryCodes();

        // load map of locids to location names
        Map<Integer, String> locationMap = loadLocations(countryMap);

        // load geo data: start-stop ips and locations
        Set<GeoData> geoDataSet = loadLocationBlocks(locationMap);
        this.geoLocations = new ArrayList<GeoData>(geoDataSet);

        // load providers for geo data
        geoDataSet = loadProviders();
        this.geoProviders = new ArrayList<GeoData>(geoDataSet);

    }

    // return array of provider and location for given ip
    public String[] sendRequest(String ip) {

        // convert ip to numeric value
        long ipNumeric = convertIpToNumeric(ip);
        String[] result = new String[12];
        result[0] = resolveLocation(ipNumeric);
        result[1] = resolveProvider(ipNumeric);
        for( int i=2; i<12; i++ ) {
            result[i] = "";
        }
        return result;
    }

    long convertIpToNumeric(String ipAddress) {

        // Parse IP parts into an int array
        String[] parts = ipAddress.split("\\.");

        long ipNumeric = 0;
        for (int i = 0; i < 4; i++) {
            long n = Integer.parseInt(parts[i]);
            ipNumeric += n << (24L - (8L * i));
        }
        return ipNumeric;
    }

    String resolveProvider(long ip) {

        return resolve(ip, this.geoProviders);
    }

    String resolveLocation(long ip) {

        return resolve(ip, this.geoLocations);
    }

    String resolve(long ip, List<GeoData> list) {

        GeoData key = new GeoData();
        key.ipFrom = ip;
        int index = Collections.binarySearch(list, key);
        if( index<0 )
            index = -index - 2;
        if( index < 0 || index >= list.size() )
            return null;
        GeoData data = list.get(index);
        return data.data;
    }

    Map<String, String> loadCountryCodes() throws IOException {

        Map<String, String> countryMap = new HashMap<String, String>();

        String fname = "db/GeoIPCountryWhois.csv.gz";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fname))));
        String line;
        while( (line=reader.readLine())!=null ) {
            String[] cols = line.split("[,]", -1);
            String countryCode = stripDoubleQuotes(cols[4]);
            String countryName = stripDoubleQuotes(cols[5]);
            countryMap.put(countryCode, countryName);
        }
        reader.close();
        return countryMap;
    }

    Map<Integer, String> loadLocations(Map<String, String> countryMap) throws IOException {

        Map<Integer, String> locMap = new HashMap<Integer, String>();

        String fname = "db/GeoLiteCity-Location.csv.gz";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fname))));
        String line;
        while( (line=reader.readLine())!=null ) {
            String[] cols = line.split("[,]", -1);
            if( cols.length<9 || !cols[1].startsWith("\"") )
                continue;
            int locId = Integer.parseInt(cols[0]);
            String countryCode = stripDoubleQuotes(cols[1]);
            String countryName = countryMap.get(countryCode);
            if( countryName==null )
                countryName = "";
            String region = stripDoubleQuotes(cols[2]);
            if( !region.isEmpty() && region.equals(countryName) )
                region = "";
            String city = stripDoubleQuotes(cols[3]);
            if( !city.isEmpty() && (city.equals(region) || city.equals(countryName)) )
                city = "";

            String location = countryName;
            if( !region.isEmpty() )
                location += ", " + region;
            if( !city.isEmpty() )
                location += ", " + city;
            if( location.isEmpty() )
                location = countryCode;

            locMap.put(locId, location);
        }

        reader.close();
        countryMap.clear();

        return locMap;
    }

    Set<GeoData> loadLocationBlocks(Map<Integer, String> locMap) throws Exception {

        Set<GeoData> geoDataSet = new TreeSet<GeoData>();

        String fname = "db/GeoLiteCity-Blocks.csv.gz";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fname))));
        String line;
        while( (line=reader.readLine())!=null ) {
            String[] cols = line.split("[,]", -1);
            if( cols.length<3 || !cols[1].startsWith("\""))
                continue;
            long startIp = Long.parseLong(stripDoubleQuotes(cols[0]));
            long stopIp = Long.parseLong(stripDoubleQuotes(cols[1]));
            int locId = Integer.parseInt(stripDoubleQuotes(cols[2]));
            String location = locMap.get(locId);

            GeoData data = new GeoData();
            data.ipFrom = startIp;
            data.ipTo = stopIp;
            data.data = location;

            geoDataSet.add(data);
        }

        reader.close();
        locMap.clear();

        return geoDataSet;
    }

    Set<GeoData> loadProviders() throws Exception {

        Set<GeoData> geoDataSet = new TreeSet<GeoData>();

        String fname = "db/GeoIPASNum2.csv.gz";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fname))));
        String line;
        GeoData key = new GeoData();
        while( (line=reader.readLine())!=null ) {
            int firstCommaPos = line.indexOf(',');
            int secondCommaPos = line.indexOf(',', firstCommaPos+1);

            Long startIp = Long.parseLong(line.substring(0, firstCommaPos));
            Long stopIp = Long.parseLong(line.substring(firstCommaPos+1, secondCommaPos));
            String AS = stripDoubleQuotes(line.substring(secondCommaPos+1));
            int spaceAt = AS.indexOf(' ');
            String provider = spaceAt>0 ? AS.substring(spaceAt+1) : AS;

            GeoData data = new GeoData();
            if( startIp<0 ) {
                System.out.println(provider);
            }
            data.ipFrom = startIp;
            data.ipTo = stopIp;
            data.data = provider;

            geoDataSet.add(data);
        }

        reader.close();

        return geoDataSet;
    }

    String stripDoubleQuotes(String str) {
        return str.substring(1, str.length()-1);
    }

    class GeoData implements Comparable<GeoData> {
        long ipFrom;
        long ipTo;
        String data;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GeoData geoData = (GeoData) o;

            if (ipFrom != geoData.ipFrom) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return (int) ipFrom;
        }

        public int compareTo(GeoData l) {
            long diff = this.ipFrom - l.ipFrom;
            return diff<0 ? -1 : diff>0 ? 1 : 0;
        }
    }
}
