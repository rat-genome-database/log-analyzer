package Log;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 5/28/13
 * Time: 12:06 PM
 * <p>
 *     - make a copy of a part of apache access_log, say from last 12 months
 *     - preprocess the file to consist of two TAB separated columns:
 *        - first word of every line of that file is IP address
 *        - second word of every line of that file is the accessed file
 *     - copy IP addresses from that file into a separate file, say 'ip_annual.log.gz'
 *     - generate frequencies for every IP address: zcat ip_annual.log.gz | sort | uniq -c > ips_freq.log
 *       the file will look like that:
 *      1 100.0.101.34
 *      1 100.0.12.59
 *      1 100.0.22.165
 *     30 100.0.22.24
 * </p>
 */
public class ApacheLogAnalysis {

    private String version;

    public static void main(String[] args) throws Exception {

        String fileName = args[0];
        XmlBeanFactory bf=new XmlBeanFactory(new FileSystemResource("properties/AppConfigure.xml"));
        ApacheLogAnalysis instance = (ApacheLogAnalysis) (bf.getBean("apacheLogAnalyzer"));
        instance.run(fileName);
    }

    void run(String fileName) throws Exception {
        System.out.println("processing "+fileName);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));

        String line;
        while( (line=reader.readLine())!=null ) {
            line = line.trim();

            // extract ip address and request url
            String ip, requestUrl;

        }

        reader.close();
    }

    /*
    * old version of code that is analyzing ips with ip frequency
    *
    void run(String fileName) throws Exception {
        System.out.println("processing "+fileName);

        BufferedReader reader = new BufferedReader(new FileReader(fileName));

        HashMap<String, HostData> mapHost = new HashMap<String, HostData>();
        HashMap<String, HostData> mapLocation = new HashMap<String, HostData>();
        HashMap<String, HostData> mapISP = new HashMap<String, HostData>();

        int skipCount = 0;
        int lineCount = 0;
        String line;
        while( (line=reader.readLine())!=null ) {
            line = line.trim();
            int spaceCharPos = line.indexOf(' ');
            int freq = Integer.parseInt(line.substring(0, spaceCharPos));
            String ip = line.substring(spaceCharPos+1);
            if( ip.equals("::1") ) {
                skipCount++;
                continue;
            }
            String[] hostData = resolveIPAddress2(ip);
            if( hostData==null ) {
                System.out.println("SKIPPED "+line);
                skipCount++;
                continue;
            }
            String location = hostData[0];
            String isp = hostData[1];
            String hostName = isp +"; "+location;

            HostData data = update(mapHost, hostName, ip, freq);
            data = update(mapLocation, location, ip, freq);
            data = update(mapISP, isp, ip, freq);

            lineCount++;

            System.out.println("[" + lineCount + "] " + hostName);

            if( lineCount%100==0 ) {
                //System.out.println();
                //System.out.println("*** mapHost.size="+mapHost.size()+", mapLoc.size="+mapLocation.size()+", mapISP.size="+mapISP.size());
                //System.out.println();

                write(mapISP, "ip_to_isp.txt", null);
                write(mapHost, "ip_to_full_name.txt", "; ");
                write(mapLocation, "ip_to_location.txt", null);
            }
        }

        reader.close();

        System.out.println("LINE COUNT: "+lineCount);
        System.out.println("SKIP COUNT: "+skipCount);

        System.out.println("ISP COUNT: "+mapISP.size());
        write(mapISP, "ip_to_isp.txt", null);

        System.out.println("HOST COUNT: "+mapHost.size());
        write(mapHost, "ip_to_full_name.txt", "; ");

        System.out.println("LOCATION COUNT: "+mapLocation.size());
        write(mapLocation, "ip_to_location.txt", null);

        System.out.println("DONE!");
    }

    void write(HashMap<String, HostData> map, String fileName, String splitPhrase) throws IOException {

        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        for( HostData data: map.values() ) {
            if( splitPhrase!=null ) {
                String[] res = data.key.split(splitPhrase, -1);
                if( res.length==1 ) {
                    writer.write(data.key);
                    writer.write('\t');
                    writer.write('\t');
                }
                else {
                    writer.write(res[0]);
                    writer.write('\t');
                    writer.write(res[1]);
                    writer.write('\t');
                }
            }
            else {
                if( data.key!=null )
                    writer.write(data.key);
                writer.write('\t');
            }
            writer.write(Integer.toString(data.freqCount));
            //writer.write('\t');
            //writer.write(data.ips);
            writer.newLine();
        }
        writer.close();
    }

    HostData update(HashMap<String, HostData> map, String key, String ip, int freq ) {

        HostData data = map.get(key);
        if( data==null ) {
            data = new HostData(key);
            data.ips = ip;
            data.freqCount = freq;
            map.put(key, data);
        }
        else {
            data.ips += ", " + ip;
            data.freqCount += freq;
        }
        return data;
    }

    String[] resolveIPAddress(String ip) throws Exception {

        String xmlText = getDocForReverseIP(ip);
        if( xmlText==null )
            return null;
        String[] tokens = xmlText.split(" ");
        int hold2 = 0;
        int hold3 = 0;

        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].matches("(?i).*location.*")) {
                hold2 = i;
            }
            if (tokens[i].matches("(?i).*ISP.*")) {
                hold3 = i;
            }
        }

        String location = tokens[hold2];
        if( location.length()>9 )
            location = location.substring(9);

        String isp = tokens[hold3];
        if( isp.length()>4 )
            isp = isp.substring(4);

        for (int i = hold2+1; i < hold3; i++) {
            location += ' ' + tokens[i];
        }

        for (int i = hold3+1; i < tokens.length; i++) {
            isp += ' ' + tokens[i];
        }

        return new String[]{location, isp};
    }

    String getDocForReverseIP(String ip) throws Exception {
        Thread.sleep(5000); // sleep 5s

        try {
            String url = "http://www.ip-adress.com/reverse_ip/";
            Document doc = Jsoup.connect(url + ip).userAgent("Mozilla").timeout(0).get();
            Elements div = doc.select("div.box");
            return div.text();
        } catch( IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String getDocForReverseIP2(String ip) throws Exception {
        Thread.sleep(5000); // sleep 5s

        try {
            String url = "http://whatismyipaddress.com/ip/"+ip;
            BufferedReader bufferedReader = new BufferedReader(
                                     new InputStreamReader(
                                          new URL(url)
                                              .openConnection()
                                              .getInputStream() ));

            StringBuilder sb = new StringBuilder();
            String line = null;
            while( ( line = bufferedReader.readLine() ) != null ) {
                sb.append( line ) ;
                sb.append( "\n");
            }
            return sb.toString();

        } catch( IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    String[] resolveIPAddress2(String ip) throws Exception {

        return GeoLocationService.getInstance().resolve(ip);
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    class HostData {
        String key;
        String ips;
        int freqCount;

        public HostData(String key) {
            this.key = key;
        }
    }
    */

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
}
