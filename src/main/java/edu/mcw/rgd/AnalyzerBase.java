package edu.mcw.rgd;

import java.util.Date;

public class AnalyzerBase {

    public String resolveProvider(String ip) throws Exception {
        GeoLocation geo = resolveReverseIP(ip);

        String provider = geo.getProvider();
        if( provider==null )
            provider = geo.getLocation();
        else if( provider.startsWith("11 (") || provider.startsWith("19 (") || provider.startsWith("28 ("))
            provider += geo.getLocation();
        if( provider==null )
            provider = "";

        return provider;
    }

    public GeoLocation resolveReverseIP(String ip) throws Exception {

        GeoLocationDAO dao = new GeoLocationDAO();
        GeoLocation geo = dao.getGeoLocationData(ip);
        if( geo==null ) {

            // NOTE: IOPATCH and TELIZE services has been discontinued
            //
            //GeoLocationService_IOPATCH service = GeoLocationService_IOPATCH.getInstance();
            //GeoLocationService_Telize service = GeoLocationService_Telize.getInstance();

            GeoLocationService service = GeoLocationService.getInstance();
            String[] data = service.sendRequest(ip);
            //0   country_name: The country name.
            //1   isp: Provider name.
            //2   areacode: Area code.
            //3   city: City.
            //4   country: Country code.
            //5   latitude: Latitude.
            //6   longitude: Longitude.
            //7   metrocode: Metro code.
            //8   postalcode: Postal code.
            //9   region: Region code.
            //10   organization: Organization or corporation.
            //11   region_name: State or province.
            String provider = data[10];
            if( !data[1].isEmpty() ) {
                if( provider.isEmpty() )
                    provider = data[1];
                else if( !data[1].equals(provider) )
                    provider += " ("+data[1]+")";
            }
            String location = data[0];
            if( !data[11].isEmpty() )
                location += ", "+data[11];
            if( !data[3].isEmpty() )
                location += ", "+data[3];

            geo = new GeoLocation();
            geo.setIp(ip);
            geo.setProvider(provider);
            geo.setLocation(location);
            geo.setLastResolvedBy(service.getName());
            geo.setLastResolvedDate(new Date());
            dao.insertGeoLocationData(geo);
        }
        return geo;
    }


}
