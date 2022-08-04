package edu.mcw.rgd;

import edu.mcw.rgd.process.Utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LogSplitter {

    public static void main(String[] args) throws IOException {

        String[] inputFolders = {"data/hancock", "data/owen", "data/apollo", "data/booker"};
        String outputFolder = "data/prod";
        String pattern = "access_log";

        inputFolders = new String[] {"data/hoshi"};
        outputFolder = "data/prodhoshi";
        pattern = "download-access.log";

        // generate monthly log files
        for( String inputFolder: inputFolders ) {

            File dir = new File(inputFolder);
            File[] files = dir.listFiles();
            for( File f: files ) {
                if( f.isFile() && f.getName().contains(pattern)) {
                    String fname = f.getAbsolutePath();
                    System.out.println("processing "+fname);
                    BufferedReader in = Utils.openReader(fname);
                    String line;
                    while( (line=in.readLine())!=null ) {
                        // sample line
                        // 185.191.171.3 - - [09/Jun/2022:22:21:08 -0500] "GET /tools/genes/genes_view.cgi?id=2319481 HTTP/1.1" 301 - "-" "Mozilla/5.0 (compatible; SemrushBot/7~bl; +http://www.semrush.com/bot.html)"
                        //
                        // extract month and year
                        int bracketPos = line.indexOf('[');
                        int slashPos1 = line.indexOf('/', bracketPos);
                        int slashPos2 = line.indexOf('/', slashPos1+1);
                        if( bracketPos<0 || slashPos1<0 || slashPos2<0 ) {
                            continue;
                        }
                        String year = line.substring(slashPos2+1, slashPos2+5);
                        String month = line.substring(slashPos1+1, slashPos2);

                        String outName = outputFolder+"/"+pattern+"-"+year+"-"+month+".gz";
                        BufferedWriter out = outFiles.get(outName);
                        if( out==null ) {
                            out = Utils.openWriter(outName);
                            outFiles.put(outName, out);
                        }
                        out.write(line);
                        out.write("\n");
                    }
                    in.close();
                }
            }
        }

        System.out.println("closing files");
        for( Map.Entry<String, BufferedWriter> entry: outFiles.entrySet() ) {
            entry.getValue().close();
        }

        System.out.println("DONE!");
    }

    static Map<String, BufferedWriter> outFiles = new HashMap<>();
}
