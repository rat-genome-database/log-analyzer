package Log;


import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 6/5/13
 * Time: 1:20 PM
 * <p>
 * Data model class for representing geo-location data;
 * Used primarily by GeoLocationDAO
 * </p>
 */
public class GeoLocation {

    private String ip; // IP4 in dot format, i.e. 127.0.0.1
    private String location; //location associated with this IP
    private String provider; // provider associated with this IP
    private String lastResolvedBy; // name of IP resolver
    private Date lastResolvedDate; // last date the IP was resolved

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getLastResolvedBy() {
        return lastResolvedBy;
    }

    public void setLastResolvedBy(String lastResolvedBy) {
        this.lastResolvedBy = lastResolvedBy;
    }

    public Date getLastResolvedDate() {
        return lastResolvedDate;
    }

    public void setLastResolvedDate(Date lastResolvedDate) {
        this.lastResolvedDate = lastResolvedDate;
    }
}
