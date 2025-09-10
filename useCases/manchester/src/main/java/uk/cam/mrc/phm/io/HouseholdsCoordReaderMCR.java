package uk.cam.mrc.phm.io;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoHousehold;
import de.tum.bgu.msm.data.MitoVacantDwelling;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Node;


import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Qin on 02.07.2018.
 */
public class HouseholdsCoordReaderMCR extends AbstractCsvReader {

    private int posId = -1;
    private int posHHId = -1;
    private int posCoordX = -1;
    private int posCoordY = -1;
    private int posTAZId = -1;
    private final Map<Integer, List<Node>> nodesByZone = new ConcurrentHashMap<>();

    private SimpleFeature shapeFeature; // Alona added

    private static final Logger logger = LogManager.getLogger(HouseholdsCoordReaderMCR.class);

    public HouseholdsCoordReaderMCR(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        logger.info("Reading household microlocation coordinate from dwelling file");
        Path filePath = Resources.instance.getDwellingsFilePath();
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        posId = MitoUtil.findPositionInArray("id", header);
        posHHId = MitoUtil.findPositionInArray("hhID", header);
        posTAZId = MitoUtil.findPositionInArray("zone", header);
        posCoordX = MitoUtil.findPositionInArray("coordX", header);
        posCoordY = MitoUtil.findPositionInArray("coordY", header);
    }

    @Override
    protected void processRecord(String[] record) {
        int hhId = Integer.parseInt(record[posHHId]);


        Coordinate homeLocation = new Coordinate(Double.parseDouble(record[posCoordX]), Double.parseDouble(record[posCoordY]));
        int taz = Integer.parseInt(record[posTAZId]);
        MitoZone zone = dataSet.getZones().get(taz);
        if(zone == null) {
            logger.warn(String.format("Household %d is supposed to live in zone %d but this zone does not exist.", hhId, taz));
        }

        if (hhId > 0) {
            MitoHousehold hh = dataSet.getHouseholds().get(hhId);
            if (hh == null) {
                logger.warn(String.format("Household %d does not exist in mito.", hhId));
                return;
            }

            hh.setHomeLocation(homeLocation);
            hh.setHomeZone(zone);
            zone.addHousehold(hh);
        } else {
            int ddId = Integer.parseInt(record[posId]);
            zone.addVacantDwelling(new MitoVacantDwelling(ddId, zone, homeLocation));
        }
    }

}
