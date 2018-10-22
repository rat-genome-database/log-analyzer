package Log;

import edu.mcw.rgd.dao.AbstractDAO;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.object.SqlUpdate;

import java.sql.Types;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: mtutaj
 * Date: 6/5/13
 * Time: 1:16 PM
 * <p>
 * GeoLocation API
 * <pre>
 *     CREATE TABLE GEOLOCATIONS (
 * IP VARCHAR2(15) NOT NULL PRIMARY KEY,
 * LOCATION VARCHAR2(500),
 * PROVIDER VARCHAR2(500),
 * LAST_RESOLVED_BY VARCHAR2(50),
 * LAST_RESOLVED_DATE DATE
 * );
 */
public class GeoLocationDAO extends AbstractDAO {

    public GeoLocation getGeoLocationData(String ip) throws Exception {

        String query = "SELECT g.* FROM geolocations g WHERE ip=?";
        GeoLocationQuery q = new GeoLocationQuery(this.getDataSource(), query);
        q.declareParameter(new SqlParameter(Types.VARCHAR));
        q.compile();
        List<GeoLocation> list = q.execute(new Object[]{ip});
        return list.isEmpty() ? null : list.get(0);
    }

    public int insertGeoLocationData(GeoLocation data) throws Exception{

        String sql = "INSERT INTO geolocations (ip,location,provider,last_resolved_by,last_resolved_date) "+
            "VALUES(?,?,?,?,?)";

        SqlUpdate su = new SqlUpdate(this.getDataSource(), sql);

        su.declareParameter(new SqlParameter(Types.VARCHAR)); // IP
        su.declareParameter(new SqlParameter(Types.VARCHAR)); // LOCATION
        su.declareParameter(new SqlParameter(Types.VARCHAR)); // PROVIDER
        su.declareParameter(new SqlParameter(Types.VARCHAR)); // last resolved by
        su.declareParameter(new SqlParameter(Types.TIMESTAMP)); // last resolved date

        su.compile();
        Object[] oa = new Object[]{data.getIp(), data.getLocation(), data.getProvider(), data.getLastResolvedBy(), data.getLastResolvedDate()};

        return su.update(oa);
    }
}
