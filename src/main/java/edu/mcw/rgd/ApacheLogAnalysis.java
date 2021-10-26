package edu.mcw.rgd;

import edu.mcw.rgd.process.Utils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: mtutaj
 * Date: 5/28/13
 */
public class ApacheLogAnalysis extends AnalyzerBase {

    private String version;
    private List<String> consolidatedUrls;

    boolean ftpMode = false;
    boolean jbrowseMode = false;

    List<LogList> logList = new ArrayList<>();
    Set<String> IPList = new HashSet<>();
    DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    DateFormat folderFormat = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
    Calendar cal = Calendar.getInstance();
    File outputDir;
    PrintWriter log_pw;
    PrintWriter pw6, pw7, pw8, pw9;
    PrintWriter pw;

    public static void main(String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        ApacheLogAnalysis instance = (ApacheLogAnalysis) (bf.getBean("apacheLogAnalyzer"));
        System.out.println(instance.getVersion());

        try {
            instance.run(args);
        }
        catch(Exception e) {
            e.printStackTrace();
            throw new Exception(e);
        }
    }

    public void run(String[] args) throws Exception {

        if( args.length <2  ) {
            System.out.println("Provide file to parse and output directory. Exiting...");
            return;
        }
        prepare(args[1]);

        if( args.length>=3 ) {
            if( args[2].equals("ftp") ) {
                ftpMode = true;
            }
            else if( args[2].equals("jbrowse") ) {
                jbrowseMode = true;
            }
        }

        log_pw.println(dateFormat.format(cal.getTime()) + " Getting log file name");
        BufferedReader br = Utils.openReader(args[0]);
        log_pw.println(dateFormat.format(cal.getTime()) + " Reading contents of file");


        logList.clear();
        log_pw.println(dateFormat.format(cal.getTime()) + " Clearing logList memory");
        IPList.clear();
        log_pw.println(dateFormat.format(cal.getTime()) + " Clearing IPList memory");

        String line = br.readLine();
        log_pw.println(dateFormat.format(cal.getTime()) + " Reading first line");

        int linesProcessed = 0;
        int emptyRequests = 0;

        while (line != null) {

            if( !writeFile(line, pw) ) {
                emptyRequests++;
            }

            linesProcessed++;
            line = br.readLine();

            //if( linesProcessed>350000 ) {
            //    break;
            //}
        }

        System.out.println("lines processed: "+linesProcessed);
        System.out.println("lines with empty requests: "+emptyRequests);

        finalizeProcessing(pw);
    }

    void prepare(String outputDir) throws IOException {

        this.outputDir = new File(outputDir + folderFormat.format(cal.getTime()) + "/");
        if (this.outputDir.mkdirs()) {
            System.out.println("directory "+outputDir+" was created successfully");
        } else {
            System.out.println("failed trying to create the directory "+outputDir);
        }

        pw6 = new PrintWriter (new BufferedWriter(new FileWriter (this.outputDir + "/LogParse.txt", true)));
        log_pw = new PrintWriter (new BufferedWriter (new FileWriter (this.outputDir + "/parser_log.log", true)));
        pw = new PrintWriter(new BufferedWriter(new FileWriter (this.outputDir + "/NumberOfFiles.txt", true)));
        pw7 = new PrintWriter (new BufferedWriter(new FileWriter (this.outputDir + "/ProviderSummary.txt", true)));
        pw8 = new PrintWriter (new BufferedWriter(new FileWriter (this.outputDir + "/FileSummary.txt", true)));
        pw9 = new PrintWriter (new BufferedWriter(new FileWriter (this.outputDir + "/FtpCategoryCrossProvider.txt", true)));

        System.out.println(dateFormat.format(cal.getTime()));
    }

    void finalizeProcessing(PrintWriter pw) throws Exception {
        printMemoryUsage();
        log_pw.println(dateFormat.format(cal.getTime()) + " Populating IP list");
        writeIPFile();
        printMemoryUsage();

        log_pw.println(dateFormat.format(cal.getTime()) + " Re-engineering data");
        Map<String, List<LogList>> ipMap = writeFile6();
        printMemoryUsage();
        writeProviderSummaries(ipMap);
        printMemoryUsage();
        writeFileSummaries();
        printMemoryUsage();

        System.out.println("___DONE___");
        log_pw.close();
        pw.close();
        pw6.close();

        if( ftpMode || jbrowseMode ) {
            showHitCountsByDay();
        }
    }

    /**
     *This method receives the string passed from main which is a line containing
     *the information needed to parse out and create the generated file
     *After parsing this line, the information is stored an ArrayList of objects
     *from the LogList class
     *@param	s	a String passed in from main for parsing
     */
    public boolean writeFile(String s, PrintWriter pw) {

        // sample line:
        //
        //114.119.142.8 - - [03/May/2021:07:32:19 -0500] "GET /rgdweb/ontology/annot.html?acc_id=XCO%3A0000265&species=Human HTTP/1.1" 200 77556 "-" "Mozilla/5.0 (Linux; Android 7.0;) AppleWebKit/537.36 (KHTML, like Gecko) Mobile Safari/537.36 (compatible; PetalBot;+https://webmaster.petalsearch.com/site/petalbot)"

        LogList log = new LogList();

        int spacePos = s.indexOf(' ');
        String ip = s.substring(0, spacePos).trim();
        log.setIP(ip);

        int parPos = s.indexOf('[');
        spacePos = s.indexOf(' ', parPos);
        String dt = s.substring(parPos+1, spacePos);
        log.setDate(dt);

        int dblQPos = s.indexOf('"');
        int dblQPos2 = s.indexOf('"', dblQPos+1);
        String request = s.substring(dblQPos+1, dblQPos2-1);
        // strip request method "GET ..." from beginning of 'request''
        // strip protocol "... HTTP/1/1" from end of 'request'
        spacePos = request.indexOf(' ');
        int spacePos2 = request.lastIndexOf(' ');
        String url = null;
        if( spacePos>=0 ) {
            if( spacePos2<=spacePos ) {
                url = request.substring(spacePos + 1);
            } else {
                url = request.substring(spacePos + 1, spacePos2);
            }

            if( ftpMode ) {

                if( url.endsWith("/") ){
                    return false; // empty line
                }
                // there is a bug with '/pub' prefix
                while( url.startsWith("/pub") ) {
                    url = url.substring(4);
                }

                // regular files will have at least two '/'
                if( !url.startsWith("/") ||  url.startsWith("/icons") ) {
                    return false; // not a download file
                }
                int slash = url.indexOf('/', 1);
                if( slash<=1 ) {
                    return false; // empty line
                }
            } else {
                // do not keep request parameters
                // example: '/rgdweb/ontology/annot.html?acc_id=XCO%3A0000265&species=Human'
                //    keep: '/rgdweb/ontology/annot.html'
                int qpos = url.indexOf('?');
                if (qpos > 0) {
                    url = url.substring(0, qpos);
                }

                if( jbrowseMode ) {
                    if( url.endsWith(".json") || url.contains(".json?")) {
                        return false;
                    }
                    if( !url.startsWith("/jbrowse/data") ) {
                        return false;
                    }
                }

                // consolidate jbrowse data_ urls
                if (url.startsWith("/jbrowse/data_")) {
                    int slashPos = url.indexOf('/', 14);
                    url = url.substring(0, slashPos) + "/xxx";
                } else if (url.startsWith("/pathway/PW")) {
                    url = "/pathway/PWxxxxxxx/xxx";
                } else {
                    // consolidate urls (other than jbrowse data urls)
                    for (String curl : getConsolidatedUrls()) {
                        if (url.startsWith(curl)) {
                            url = curl + "xxx";
                            break;
                        }
                    }
                }
            }

        } else {
            return false; // empty line
        }
        log.setFile(url);

        spacePos = s.indexOf(' ', dblQPos2);
        spacePos2 = s.indexOf(' ', spacePos+1);
        String status = s.substring(spacePos+1, spacePos2);
        log.setStatus(status);

        logList.add(log);
        pw.println(log.getIP());
        pw.println(log.getDate());
        pw.println(log.getFile());
        pw.println(log.getStatus());
        pw.println();

        return true;
    }

    /**
     *This method writes a file that prints each IP address that is stored in IPList in order
     *to get an accurate account of each IP address from the log file that accesses the server and
     *downloads a file and to compare with the file generated document
     */
    public void writeIPFile() throws Exception {
        String fileName = outputDir + "/IPAddresses.txt";
        BufferedWriter out = new BufferedWriter (new FileWriter (fileName, true));

        for (LogList entry : logList) {
            if( IPList.add(entry.getIP()) ) {
                out.write(entry.getIP()+"\n");
            }
        }

        out.close();
    }

    /**
     *Generate a file: write the IP address, where the IP address
     *is located, and all the files in chronological order that the IP address attempted to download.
     *This information includes the date range of the log file, the date time of each file that was downloaded
     *and if the attempt to download the file failed, the IP address is sent to a website to gather the
     *the information needed to determine location and ISP
     */
    public Map<String, List<LogList>> writeFile6() throws Exception {

        List<String> ipList = new ArrayList<>(IPList);
        Collections.shuffle(ipList);

        pw6.println("Number of IP addresses that access the server: " + ipList.size());
        pw6.println("Date: " + logList.get(0).getDate() + " to " + logList.get(logList.size() - 1).getDate());
        pw6.println();

        System.out.println("building IP hashmap ...");
        Map<String, List<LogList>> ipMap = new HashMap<>();
        for( LogList l: logList ) {
            List<LogList> list = ipMap.get(l.getIP());
            if( list==null ) {
                list = new ArrayList<>();
                ipMap.put(l.getIP(), list);
            }
            list.add(l);
        }
        System.out.println("IP hashmap OK!");

        int ipListIndex = 0;
        for (String IP : ipList) {
            int numOfRecords = 0;

            GeoLocation geo = resolveReverseIP(IP);
            ipListIndex++;
            //System.out.println("[" + ipListIndex + "/" + ipList.size() + "] " + IP + " " + geo.getProvider() + " " + geo.getLocation()+" ["+geo.getLastResolvedBy()+"]");

            pw6.println(geo.getIp() + " - " + geo.getProvider() + " ["+geo.getLocation()+"]" );

            List<LogList> hitList = ipMap.get(IP);
            if( hitList !=null )
            for (LogList aLogList : hitList) {

                if (IP.equals(aLogList.getIP())) {

                    pw6.println();
                    pw6.print("\t");
                    pw6.print(aLogList.getDate() + " - ");
                    pw6.print(aLogList.getFile());

                    if (aLogList.getStatus().equals("FAIL") ) {
                        pw6.print(" *** FAILED ***");
                    } else {
                        numOfRecords++;
                    }

                }
            }
            pw6.println();
            pw6.println("Number of Downloads: " + numOfRecords);
            pw6.println();
            pw6.println();
        }

        pw6.close();

        return ipMap;
    }

    public void writeProviderSummaries(Map<String, List<LogList>> ipMap) throws Exception {

        System.out.println("building provider summaries");

        // combine ip addresses in groups, grouped by provider name, case-insensitive
        Map<String, List<String>> providerMap = new TreeMap<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return Utils.stringsCompareToIgnoreCase(o1, o2);
            }
        });
        for (String ip : IPList) {
            String provider = resolveProvider(ip);

            List<String> requests = providerMap.get(provider);
            if( requests==null ) {
                requests = new ArrayList<>();
                providerMap.put(provider, requests);
            }

            List<LogList> hitList = ipMap.get(ip);
            if( hitList !=null )
            for (LogList aLogList : hitList) {
                if (ip.equals(aLogList.getIP())) {
                    requests.add(aLogList.getFile());
                }
            }
        }

        System.out.println("writing out provider summaries "+providerMap.size());

        pw7.println("Note: numbers in brackets indicate count of successful downloads");
        pw7.println();

        // now dump the data for provider groups
        for( Map.Entry<String, List<String>> entry: providerMap.entrySet() ) {

            List<String> files = entry.getValue();
            pw7.println(entry.getKey()+" ["+files.size()+"]");

            // group files and count them
            Map<String, Integer> fileHitCounts = buildHitCountMap(files);
            for( Map.Entry<String, Integer> fentry: fileHitCounts.entrySet() ) {
                pw7.println("   ["+fentry.getValue()+"] "+fentry.getKey());
            }
            pw7.println();
        }
        pw7.close();

        grid(providerMap);
    }

    public void writeFileSummaries() throws Exception {

        System.out.println("building file summaries");

        // group provider names by file name
        Map<String, List<String>> fileNameMap = new TreeMap<>();
        for (LogList entry : logList) {

            if( entry.getStatus().equals("OK") ) {
                String provider = resolveProvider(entry.getIP());
                List<String> providers = fileNameMap.get(entry.getFile());
                if( providers==null ) {
                    providers = new ArrayList<>();
                    fileNameMap.put(entry.getFile(), providers);
                }
                providers.add(provider);
            }
        }

        System.out.println("writing out file summaries "+fileNameMap.size());

        pw8.println("Note: numbers in brackets indicate count of successful downloads");
        pw8.println();

        // now dump the data for file groups
        for( Map.Entry<String, List<String>> entry: fileNameMap.entrySet() ) {

            List<String> providers = entry.getValue();
            pw8.println(entry.getKey()+" ["+providers.size()+"]");

            // group providers and count them
            Map<String, Integer> providerHitCounts = buildHitCountMap(providers);
            for( Map.Entry<String, Integer> fentry: providerHitCounts.entrySet() ) {
                pw8.println("   ["+fentry.getValue()+"] "+fentry.getKey());
            }

            pw8.println();
        }
        pw8.close();
    }

    /**
     *
     * @param providerMap map of provider names to list of files
     */
    void grid(Map<String, List<String>> providerMap) throws IOException {

        // build frequency hit map of urls
        Map<String, Integer> urlFreqMap = new HashMap<>();
        for( LogList e: logList ) {
            Integer hitCount = urlFreqMap.get(e.getFile());
            if( hitCount==null ) {
                hitCount = 1;
            } else {
                hitCount++;
            }
            urlFreqMap.put(e.getFile(), hitCount);
        }

        // reverse frequency map
        Map<Integer, List<String>> reverseFreqMap = new TreeMap<>();
        for( Map.Entry<String, Integer> entry: urlFreqMap.entrySet() ) {
            int hitCount = -entry.getValue();
            List<String> list = reverseFreqMap.get(hitCount);
            if( list==null ) {
                list = new ArrayList<>();
                reverseFreqMap.put(hitCount, list);
            }
            list.add(entry.getKey());
        }

        // build list of urls starting from most popular
        BufferedWriter out = new BufferedWriter(new FileWriter(this.outputDir+"/UrlHitFreqList.txt"));
        List<String> rankedUrlList = new ArrayList<>();
        for( Map.Entry<Integer, List<String>> entry: reverseFreqMap.entrySet() ) {
            int freq = entry.getKey();
            for( String url: entry.getValue() ) {
                rankedUrlList.add(url);
                out.write(url+" ["+(-freq)+"]\n");
            }
        }
        out.close();

        // dump ftp category names
        pw9.print('\t'+"Total");
        int i=0;
        for( String url: rankedUrlList ) {
            pw9.print('\t'+url);
            if( ++i>=250 ) {
                pw9.print("\tOther");
                break;
            }
        }
        pw9.print('\t'+"Total");
        pw9.println();

        // dump providers data
        for( Map.Entry<String, List<String>> entry: providerMap.entrySet() ) {
            String providerName = entry.getKey();
            pw9.print(providerName);
            //System.out.println("grid for "+providerName);

            // construct unique list of files
            List<String> list = entry.getValue();
            Map<String, Integer> urlHitCountMap = buildHitCountMap(list);

            StringBuffer buf = new StringBuffer();

            int totalHitCountForProvider = 0;
            for( i=0; i<250 && i<rankedUrlList.size(); i++ ) {
                String url = rankedUrlList.get(i);
                Integer hitCount = urlHitCountMap.get(url);
                if( hitCount==null ) {
                    buf.append("\t");
                } else {
                    buf.append("\t").append(hitCount.toString());
                    totalHitCountForProvider += hitCount;
                }
            }

            int hitCountForOther = 0;
            for( ; i<rankedUrlList.size(); i++ ) {
                String url = rankedUrlList.get(i);
                Integer hitCount = urlHitCountMap.get(url);
                if( hitCount!=null ) {
                    hitCountForOther += hitCount;
                }
            }
            if( hitCountForOther==0 ) {
                buf.append("\t");
            } else {
                buf.append("\t").append(hitCountForOther);
                totalHitCountForProvider += hitCountForOther;
            }

            pw9.print("\t");
            pw9.print(totalHitCountForProvider);
            pw9.print(buf.toString());
            pw9.print("\t");
            pw9.print(totalHitCountForProvider);

            pw9.println();
        }
        pw9.close();

        System.out.println("grid done!");
    }

    Map<String, Integer> buildHitCountMap(Collection<String> list) {

        Map<String, Integer> fileHitCount = new TreeMap<>();
        for( String item: list ) {
            Integer hitCount = fileHitCount.get(item);
            if( hitCount==null )
                fileHitCount.put(item, 1);
            else
                fileHitCount.put(item, hitCount+1);
        }
        return fileHitCount;
    }

    void printMemoryUsage() {
        Runtime rt = Runtime.getRuntime();
        float totalMemory = (float) (rt.totalMemory() / 1024.0 / 1024.0 / 1024.0);
        float freeMemory = (float) (rt.freeMemory() / 1024.0 / 1024.0 / 1024.0);
        System.out.println("=== TOTAL MEMORY: "+totalMemory+" GB");
        System.out.println("===  FREE MEMORY: "+freeMemory+" GB");
    }

    void showHitCountsByDay() throws IOException {

        // Date line is like this:
        // 17/Oct/2021:03:59:43
        Map<String, Integer> hitCountMap = new TreeMap<>();
        Map<String, Set<String>> uniqueIpMap = new TreeMap<>();
        int totalHits = 0;
        int skipped = 0;

        String fname = outputDir+"/NumberOfFiles.txt";
        BufferedReader in = Utils.openReader(fname);
        String line1, line2, line3, line4, line5;
        while( (line1=in.readLine())!=null ) {
            line2 = in.readLine(); // date line
            line3 = in.readLine(); // file
            line4 = in.readLine(); // status
            line5 = in.readLine(); // separator line


            String dt = line2.substring(7, 11)+line2.substring(2,6);

            // skip the lines from 2021/Jun/15, IP 141.106.224.149
            // because on those this day there ware 300,000 requests from thjs location
            if( line2.startsWith("15/Jun/2021") && line1.equals("141.106.224.149") ) {
                skipped++;
                continue;
            }
            Integer hitCount = hitCountMap.get(dt);
            if( hitCount==null ) {
                hitCount = 0;
            }
            hitCount++;
            hitCountMap.put(dt, hitCount);

            Set<String> ips = uniqueIpMap.get(dt);
            if( ips==null ) {
                ips = new TreeSet<>();
                uniqueIpMap.put(dt, ips);
            }
            ips.add(line1);

            totalHits++;
        }
        in.close();

        Set<String> allIps = new HashSet<>();

        System.out.println("good hits: "+totalHits);
        System.out.println("skip hits: "+skipped);
        System.out.println("");
        for( Map.Entry<String,Integer> entry: hitCountMap.entrySet() ) {
            String dt = entry.getKey();
            Set<String> ips = uniqueIpMap.get(dt);
            allIps.addAll(ips);
            int uniqueIps = ips.size();
            System.out.println(dt+": "+entry.getValue()+",  unique ips: "+uniqueIps);
        }
        System.out.println("");
        System.out.println("Unique IP addresses in entire time range: "+allIps.size());
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getConsolidatedUrls() {
        return consolidatedUrls;
    }

    public void setConsolidatedUrls(List<String> consolidatedUrls) {
        this.consolidatedUrls = consolidatedUrls;
    }
}
