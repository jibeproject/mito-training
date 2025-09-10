package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.AreaTypes;
import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Properties;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.matsim.core.utils.gis.ShapeFileReader;


/**
 * @author Nico
 */
public class ZonesReaderMCR extends AbstractCsvReader {

    private static final Logger logger = LogManager.getLogger(ZonesReaderMCR.class);
    private int posId;
    private int posAreaType;
    private int posVacancyRate;
    private int posCentroidX;
    private int posCentroidY;

    public ZonesReaderMCR(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        super.read(Resources.instance.getZonesInputFile().toAbsolutePath(), ",");
        mapFeaturesToZones(dataSet);
    }

    private static void mapFeaturesToZones(DataSet dataSet) {
        for (SimpleFeature feature: ShapeFileReader.getAllFeatures(Resources.instance.getZoneShapesInputFile().toString())) {
            int zoneId = Integer.parseInt(feature.getAttribute(Resources.instance.getString(Properties.ZONE_SHAPEFILE_ID_FIELD)).toString());
            MitoZone zone = dataSet.getZones().get(zoneId);
            if (zone != null){
                zone.setGeometry((Geometry) feature.getDefaultGeometry());
            }else{
                logger.warn("zoneId " + zoneId + " doesn't exist in mito zone system");
            }
        }
    }

    @Override
    protected void processHeader(String[] header) {
        posId = MitoUtil.findPositionInArray("oaID", header);
        posAreaType = MitoUtil.findPositionInArray("urbanType", header);
        posVacancyRate = MitoUtil.findPositionInArray("percentageVacantDwellings", header);
        posCentroidX = MitoUtil.findPositionInArray("popCentroid_x",header);
        posCentroidY = MitoUtil.findPositionInArray("popCentroid_y",header);
    }

    @Override
    protected void processRecord(String[] record) {
        int zoneId = Integer.parseInt(record[posId]);
        int urbanType = Integer.parseInt(record[posAreaType]);
        double vacancyRate = Double.parseDouble(record[posVacancyRate]);
        double centroidX = Double.parseDouble(record[posCentroidX]);
        double centroidY = Double.parseDouble(record[posCentroidY]);
        MitoZone zone = new MitoZone(zoneId, null);
        zone.setAreaTypeR(urbanType == 1? AreaTypes.RType.URBAN : AreaTypes.RType.RURAL);
        zone.setVacancyRate(vacancyRate);
        zone.setCentroid(new Coordinate(centroidX,centroidY));
        dataSet.addZone(zone);
    }
}
