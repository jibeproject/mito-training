package de.tum.bgu.msm.io.input.readers;

import de.tum.bgu.msm.data.DataSet;
import de.tum.bgu.msm.data.MitoPoi;
import de.tum.bgu.msm.data.MitoZone;
import de.tum.bgu.msm.io.input.AbstractCsvReader;
import de.tum.bgu.msm.modules.tripGeneration.ExplanatoryVariable;
import de.tum.bgu.msm.resources.Resources;
import de.tum.bgu.msm.util.MitoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Coordinate;

import java.nio.file.Path;

/**
 * Created by Corin on 05.03.2025.
 */
public class PoiReader extends AbstractCsvReader {

    private int posId = -1;
    private int posZoneId = -1;
    private int posWt = -1;
    private int posCode = -1;
    private int posCoordX = -1;
    private int posCoordY = -1;
    private static final Logger logger = LogManager.getLogger(PoiReader.class);

    public PoiReader(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void read() {
        logger.info("  Reading household micro data from ascii file");
        Path filePath = Resources.instance.getPoiWeightsFilePath();
        super.read(filePath, ",");
    }

    @Override
    protected void processHeader(String[] header) {
        posId = MitoUtil.findPositionInArray("id", header);
        posZoneId = MitoUtil.findPositionInArray("zone", header);
        posWt = MitoUtil.findPositionInArray("WT", header);
        posCode = MitoUtil.findPositionInArray("code",header);
        posCoordX = MitoUtil.findPositionInArray("X",header);
        posCoordY = MitoUtil.findPositionInArray("Y",header);
    }

    @Override
    protected void processRecord(String[] record) {

        int id = Integer.parseInt(record[posId]);
        int zoneId = Integer.parseInt(record[posZoneId]);
        double weight = posWt > -1 ? Double.parseDouble(record[posWt]) : 1;
        ExplanatoryVariable code = ExplanatoryVariable.valueOf(record[posCode]);
        double coordX = Double.parseDouble(record[posCoordX]);
        double coordY = Double.parseDouble(record[posCoordY]);

        MitoZone zone = dataSet.getZones().get(zoneId);

        MitoPoi bb = new MitoPoi(id, zone, new Coordinate(coordX,coordY), code, weight);
        zone.addPoi(bb);
    }
}
