/**
 *This program takes a log file that contains information on outside
 *entities that interact with the RGD and parses the information. It then 
 *selects information pertaining to what files were downloaded, when the files
 *were downloaded, what IP address downloaded the files, and the location of
 *the IP address. It writes this information grouping together files downloaded 
 *by a certain IP in chronological order.
 */
package edu.mcw.rgd;

import edu.mcw.rgd.process.Utils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.FileSystemResource;

import java.io.*;

import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class LogAnalyzer extends AnalyzerBase {
	ArrayList<LogList> logList = new ArrayList<LogList>();
	Set<String> IPList = new HashSet<String>();
	int countOK = 0;
    int countFAIL = 0;
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
	DateFormat folderFormat = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss");
	Calendar cal = Calendar.getInstance();
	File outputDir;
    PrintWriter log_pw;
	PrintWriter pw6, pw7, pw8, pw9;
    PrintWriter pw;
    private String version;
    private Map<String,String> ftpCategories;

    /**
	 *Main method, throws IOException due to file reader and file writer,
	 *creates directory based on parameters, reads log file searching for
	 *information on files downloaded from the server, passes information to
	 *writeFile() method
	 *@param	args	an array of String arguments that determine where the log file is stored and where to create the new directory for the created fils
	 */
	public static void main (String[] args) throws Exception {

        DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
        new XmlBeanDefinitionReader(bf).loadBeanDefinitions(new FileSystemResource("properties/AppConfigure.xml"));
        LogAnalyzer instance = (LogAnalyzer) (bf.getBean("manager"));
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

        log_pw.println(dateFormat.format(cal.getTime()) + " Getting log file name");
        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        log_pw.println(dateFormat.format(cal.getTime()) + " Reading contents of file");


        logList.clear();
        log_pw.println(dateFormat.format(cal.getTime()) + " Clearing logList memory");
        IPList.clear();
        log_pw.println(dateFormat.format(cal.getTime()) + " Clearing IPList memory");

        String line = br.readLine();
        log_pw.println(dateFormat.format(cal.getTime()) + " Reading first line");
        while (line != null) {

            if( line.contains("FAIL DOWNLOAD:") ) {
                writeFile(line, pw, "FAIL");
                //log_pw.println(dateFormat.format(cal.getTime()) + " Writing line containing \"RETR\"");
                countFAIL++;
            }
            else if( line.contains("OK DOWNLOAD:") ) {
                writeFile(line, pw, "OK");
                //log_pw.println(dateFormat.format(cal.getTime()) + " Writing line containing \"RETR\"");
                countOK++;
            }

            line = br.readLine();
        }

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
        log_pw.println(dateFormat.format(cal.getTime()) + " Populating IP list");
        makeIPArray();

        log_pw.println(dateFormat.format(cal.getTime()) + " Re-engineering data");
        writeFile4();
        writeFile6();
        writeProviderSummaries();
        writeFileSummaries();

        System.out.println("___DONE___");
        log_pw.close();
        pw.close();
        pw6.close();
    }

	/**
	 *This method receives the string passed from main which is a line containing
	 *the information needed to parse out and create the generated file
	 *After parsing this line, the information is stored an ArrayList of objects
	 *from the LogList class
	 *@param	s	a String passed in from main for parsing
	 */
	public void writeFile(String s, PrintWriter pw, String status) throws java.io.IOException
	{
		String file = "";
		String delim = "[ ]";
		String[] tokens = s.split(delim);
		

		LogList log = new LogList();
        log.setStatus(status);

        if (tokens[2].equals("")) {

            log.setDate(tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + " " + tokens[4] + " " + tokens[5]);
            log.setIP(tokens[12].substring(1, tokens[12].length() - 2));

            String fileName = "";
            for (int i = 13; i < tokens.length-3; i++) {
                fileName += tokens[i];
            }
            file = normalizeFileName(fileName);
        }
        else {
            log.setDate(tokens[0] + " " + tokens[1] + " " + tokens[2] + " " + tokens[3] + " " + tokens[4]);
            log.setIP(tokens[11].substring(1, tokens[11].length() - 2));

            String fileName = "";
            for (int i = 12; i < tokens.length-1; i++) {
                fileName += tokens[i];
            }
            file = normalizeFileName(fileName);
        }

        log.setFile(file);

        logList.add(log);
        if( status.equals("OK") ) {
            pw.println("Record: " + (countOK + 1));
            pw.println(log.getIP());
            pw.println(log.getDate());
            pw.println(log.getFile());
        }
        pw.println();
	}


    /**
     * file name could be within double quotes
     * then normalize access paths:
     *   replace /../../../../../../../../pub/RGD_genome_annotations/sequence_files/RGD_mRNA.dat
     *   with /pub/RGD_genome_annotations/sequence_files/RGD_mRNA.dat
     * @param str
     * @return
     */
    String normalizeFileName(String str) {

        String file;
        int posDoubleQuote1 = str.indexOf('\"');
        if( posDoubleQuote1>=0 ) {
            int posDoubleQuote2 = str.lastIndexOf('\"');
            if( posDoubleQuote2>posDoubleQuote1 ) {
                file = str.substring(posDoubleQuote1+1, posDoubleQuote2);
            }
            else
                file = str.substring(posDoubleQuote1+1);
        }
        else
            file = str;

        if( file.contains("../") )
            file = file.replace("../", "");

        return file;
    }

	/**
	 *This method will store all the IP addresses that occur in the retrieved lines
	 *containing information for parsing. It will loop through the logList ArrayList
	 *and add the IP addresses for each object to the IPList ArrayList made up of 
	 *String objects This list is then passed to removeDuplicates() to remove duplicate
	 *IP addresses so that each IP address is only listed once
	 */
	public void makeIPArray() throws java.io.IOException
	{
        for (LogList aLogList : logList) {
            IPList.add(aLogList.getIP());
        }
	}
	
	/**
	 *This method writes a file that prints each IP address that is stored in IPList in order
	 *to get an accurate account of each IP address from the log file that accesses the server and 
	 *downloads a file and to compare with the file generated document, throws IOException due
	 *to creating directory and file
	 */
	public void writeFile4() throws Exception
	{
        String fileName = outputDir + "/NumberOfIP.txt";
		PrintWriter pw4 = new PrintWriter (new BufferedWriter (new FileWriter (fileName, true)));
        int n = 1;
        for( String ip: IPList ) {
            pw4.println("Record: " + n );
            pw4.println(ip);
            n++;
        }
        pw4.close();
	}
	
	/**
	 *This method writes the file generated document that writes the IP address, where the IP address
	 *is located, and all the files in chronological order that the IP address attempted to download.
	 *This information includes the date range of the log file, the date time of each file that was downloaded
	 *and if the attempt to download the file failed, the IP address is sent to a website to gather the 
	 *the information needed to determine location and ISP, throws IOException due to creating a new directory and file
	 *throws net exception due to accessing the website and receiving information from the website
	 */
	public void writeFile6() throws Exception {

        List<String> ipList = new ArrayList<>(IPList);
        Collections.shuffle(ipList);

		pw6.println("Number of IP addresses that access the server: " + ipList.size());
		pw6.println("Number of files downloaded: " + countOK);
        pw6.println("Number of failed downloads: " + countFAIL);
		pw6.println("Date: " + logList.get(0).getDate() + " to " + logList.get(logList.size() - 1).getDate());
		pw6.println();

        int ipListIndex = 0;
        for (String IP : ipList) {
            int numOfRecords = 0;

            GeoLocation geo = resolveReverseIP(IP);
            ipListIndex++;
            System.out.println("[" + ipListIndex + "/" + ipList.size() + "] " + IP + " " + geo.getProvider() + " " + geo.getLocation()+" ["+geo.getLastResolvedBy()+"]");

            pw6.println(geo.getIp() + " - " + geo.getProvider() + " ["+geo.getLocation()+"]" );

            for (LogList aLogList : logList) {

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
	}

    public void writeProviderSummaries() throws Exception {

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

            for (LogList aLogList : logList) {
                if (ip.equals(aLogList.getIP())) {
                    if( aLogList.getStatus().equals("OK") ) {
                        requests.add(aLogList.getFile());
                    }
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
    void grid(Map<String, List<String>> providerMap) {

        // dump ftp category names
        for( String ftpCat: ftpCategories.keySet() ) {
            pw9.print('\t'+ftpCat);
        }
        pw9.print('\t'+"Other");
        pw9.print('\t'+"Total");
        pw9.println();

        // dump providers data
        for( Map.Entry<String, List<String>> entry: providerMap.entrySet() ) {
            String providerName = entry.getKey();
            pw9.print(providerName);
            System.out.println("grid for "+providerName);

            // construct unique list of files
            Map<String, Integer> fileHitCountMap = buildHitCountMap(entry.getValue());
            // map provider's file list to ftp categories -- count hit counts per ftp category
            Map<String, Integer> ftpCatHitCountMap = mapFilesToFtpCategories(fileHitCountMap);

            int totalHitCountForProvider = 0;
            for( String ftpCat: ftpCategories.keySet() ) {
                Integer hitCount = ftpCatHitCountMap.get(ftpCat);
                pw9.print('\t'+(hitCount==null ? "" : hitCount.toString()));
                if( hitCount!=null )
                    totalHitCountForProvider+=hitCount;
            }

            Integer hitCount = ftpCatHitCountMap.get("Other");
            pw9.print('\t'+(hitCount==null ? "" : hitCount.toString()));
            if( hitCount!=null )
                totalHitCountForProvider+=hitCount;
            pw9.print('\t'+(Integer.toString(totalHitCountForProvider)));

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


    /**
     * get ftp categories mapped to number of hits
     * @param fileHitCountMap map of files to hit counts (how many times given file was downloaded)
     * @return
     */
    Map<String, Integer> mapFilesToFtpCategories(Map<String, Integer> fileHitCountMap) {

        Map<String, Integer> hitCountMap = new HashMap<>();

        for( Map.Entry<String, Integer> entry: fileHitCountMap.entrySet()) {

            String file = entry.getKey();
            Integer fileHitCount = entry.getValue();

            Collection<String> cats = mapFileToFtpCategories(file);
            if( cats.isEmpty() )
                cats.add("Other");
            for( String cat: cats ) {
                Integer hitCount = hitCountMap.get(cat);
                if( hitCount==null )
                    hitCount = 0;
                hitCount += fileHitCount;
                hitCountMap.put(cat, hitCount);
            }
        }

        return hitCountMap;
    }

    Collection<String> mapFileToFtpCategories(String file) {

        Set<String> cats = new HashSet<>();

        for( Map.Entry<String, String> entry: ftpCategories.entrySet() ) {

            String[] patterns = entry.getValue().split("\\s+");
            for( String pattern: patterns ) {
                if( file.contains(pattern) ) {
                    cats.add(entry.getKey());
                }
            }
        }

        return cats;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setFtpCategories(Map ftpCategories) {
        // normalize the map values and keys:
        // values must be trimmed
        // keys must be sorted and then the map should preserve the key order
        Map<String, String> map = new LinkedHashMap<>();

        List<String> ftpCats = new ArrayList<String>(ftpCategories.keySet());
        Collections.sort(ftpCats);

        for( String ftpCat: ftpCats ) {
            String files = ftpCategories.get(ftpCat).toString().trim();
            map.put(ftpCat.substring(3), files);
        }

        this.ftpCategories = map;
    }

    public Map getFtpCategories() {
        return ftpCategories;
    }
}