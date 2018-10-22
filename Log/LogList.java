/**
 *This class keeps tracked of the parsed data from the log file in order to generate the new information
 */
package Log;

public class LogList
{
	private String date;
	private String IP;
	private String file;
	private String status; // download status: downloaded or failed

	/**
	 *Constructor
	 *@param	theDate	date of file download
	 *@param	theIP		IP address that downloaded the file
	 *@param	theFile	File name of the downloaded File
	 */
	public LogList(String theDate, String theIP, String theFile)
	{
		setDate(theDate);
		setIP(theIP);
		setFile(theFile);
	}
	
	/**
	 *Default constructor, sets class variables to empty Strings
	 */
	public LogList()
	{
		setDate("");
		setIP("");
		setFile("");
	}
	
	/**
	 *Sets the date of the file download
	 *@param theDate	date of file download
	 */
	public void setDate(String theDate)
	{
		date = theDate;
	}
	
	/**
	 *Returns the date of the file download
	 *@return date	date of file download
	 */
	public String getDate()
	{
		return date;
	}
	
	/**
	 *Sets the IP address that downloaded the file
	 *@param theIP	IP address that downloaded the file
	 */
	public void setIP(String theIP)
	{
		IP = theIP;
	}
	
	/**
	 *Returns the IP address that downloaded the file
	 *@return IP	IP address that downloaded the file
	 */
	public String getIP()
	{
		return IP;
	}

	/**
	 *Sets the file name of the downloaded file
	 *@param theFile	file name of the downloaded file
	 */
	public void setFile(String theFile)
	{
		file = theFile;
	}
	
	/**
	 *Returns the file name of the downloaded filee
	 *@return file	file name of the downloaded file
	 */
	public String getFile()
	{
		return file;
	}

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return IP+" "+status+" "+date+" "+file;
    }
}