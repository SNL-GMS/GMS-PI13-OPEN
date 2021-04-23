package gms.shared.frameworks.injector.ui;

import gms.shared.frameworks.injector.Modifier;
import gms.shared.frameworks.osd.coi.signaldetection.Station;
import gms.shared.frameworks.osd.coi.signaldetection.StationGroup;
import gms.shared.frameworks.osd.coi.systemmessages.SystemMessage;
import java.security.SecureRandom;
import java.util.*;

/**
 * System message modifier class called by the data injector to randomly create System Messages to
 * be displayed in the UI.
 */
public class SystemMessageModifier implements Modifier<Iterable<SystemMessage>> {
    private ArrayList<SystemMessageGenerator> systemMessageGenerators = new ArrayList<>();
    private SecureRandom random;
    private static class Config {
        private static final String BASE_FILE_PATH = "gms/shared/frameworks/injector/";
        static final String STATION_LIST_FILE_PATH = BASE_FILE_PATH + "StationGroupMap.json";
    }

    /* Constructor initialize station list and SystemMessageGenerator for each station */
    public SystemMessageModifier() {
        this.random = new SecureRandom();
        List<StationGroup> stationGroups =
            UiDataInjectorUtility.loadStationGroupsFromFile(Config.STATION_LIST_FILE_PATH);
        Set<Station> stations = UiDataInjectorUtility.getStationSet(stationGroups);
        for(Station station : stations) {
            systemMessageGenerators.add(new SystemMessageGenerator(station));
        }
    }

    /**
     * Called by data injector to generate System Messages.
     * Publishes the system messages to a Kafka topic.
     *
     * @param systemMessageList the number of these is how many we will produce
     * @return an empty list. (messages are produced as a side effect)
     */
    @Override
    public List<SystemMessage> apply(Iterable<SystemMessage> systemMessageList) {
        List<SystemMessage> newSystemMessageMaterializedViewList = new ArrayList<>();
    SystemMessageGenerator generator = systemMessageGenerators.get((int)(
        systemMessageGenerators.size() * this.random.nextDouble()));
    newSystemMessageMaterializedViewList.add(generator.getSystemMessage());
       return newSystemMessageMaterializedViewList;
    }
}
