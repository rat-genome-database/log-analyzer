package Log;

import org.springframework.jdbc.object.MappingSqlQuery;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 6/5/13
 * Time: 1:29 PM
 */
public class GeoLocationQuery extends MappingSqlQuery {

    public GeoLocationQuery(DataSource ds, String query) {
        super(ds, query);
    }

    protected Object mapRow(ResultSet rs, int rowNum) throws SQLException {

        GeoLocation obj = new GeoLocation();
        obj.setIp(rs.getString("ip"));
        obj.setLocation(rs.getString("location"));
        obj.setProvider(rs.getString("provider"));
        obj.setLastResolvedBy(rs.getString("last_resolved_by"));
        obj.setLastResolvedDate(rs.getTimestamp("last_resolved_date"));
        return obj;
    }
}
