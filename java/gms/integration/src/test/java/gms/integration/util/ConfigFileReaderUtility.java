package gms.integration.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import gms.shared.frameworks.configuration.Configuration;
import gms.shared.frameworks.configuration.ConfigurationOption;
import gms.shared.frameworks.configuration.util.ObjectSerialization;
import gms.shared.frameworks.osd.coi.datatransferobjects.CoiObjectMapperFactory;
import gms.shared.frameworks.test.utils.containers.GmsDeploymentContainer;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Utility class for reading in configurations from json files and posting them to the processing config service.

The class can be created with the base path (the directory containing all the subdirectories corresponding to the configurations)
and a list of filenames. There should not be more than one filename in the list in any configuration subdirectory.
The ConfigFileReaderUtility will attempt to find all the files and load them as configurations into
the configurationByName map. If there is more than one file with a filename in the list in a single subdirectory,
 all but one of the files will be ignored.

 The class can also be created with the base path and a map of configuration name to file name.
 The configuration name should be the name of the configuration's subdirectory. The ConfigFileReader Utility
 will load the specified file in the specified directory.
 */
public class ConfigFileReaderUtility {

  private static final Logger logger = LoggerFactory.getLogger(ConfigFileReaderUtility.class);

  private final Map<String, Configuration> configurationByName;

  public ConfigFileReaderUtility(Path basepath, List<String> filenames){
    // get a list of all the subdirectories in the basepath
    Collection<String> subDirectories = getSubDirectories(basepath.toFile());
    this.configurationByName=loadConfigurations(subDirectories, filenames).stream()
        .collect(Collectors.toMap(Configuration::getName, Function.identity()));
  }

  public ConfigFileReaderUtility(Path basepath, Map<String, String> configFilenameMap){
    this.configurationByName=loadConfigurations(basepath.toFile(), configFilenameMap).stream()
        .collect(Collectors.toMap(Configuration::getName, Function.identity()));
  }

  public Optional<Configuration> get(String key) {
    return Optional.of(this.configurationByName.get(key));
  }

  // send configurations to processing config service in test container
  public boolean postConfigurationsforTest(GmsDeploymentContainer deploymentContainer)
  throws IOException{
    boolean allConfigurationPosted=true;
    for (Configuration configuration : configurationByName.values()){
      Optional<String> retVal = null;
      try{
        retVal=ProcessingConfigUtility.postConfiguration(deploymentContainer, configuration);
      }
      catch (IOException e){
        throw e;
      }
      allConfigurationPosted=allConfigurationPosted && retVal.isPresent();
    }
    return allConfigurationPosted;
  }

  // load configurations based on a list of file names
  private List<Configuration> loadConfigurations(Collection<String> subDirectories, List<String> filenames) {

    //Maps json to GMS objects
    final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    List<Configuration> configurations = new ArrayList<>();

    List<String> loadedFiles=new ArrayList<>();
    String foundFile;

    // Files in each subdirectory are the ConfigurationOptions
    for (String subDir : subDirectories) {
      foundFile=(loadFilesInSubdirectory(subDir, filenames, configurations, objectMapper));
      if(foundFile!=null){
        loadedFiles.add(foundFile);
      }
    }

    List<String>  unloadedFiles=filenames;
    unloadedFiles.removeAll(loadedFiles);
    if(!unloadedFiles.isEmpty()){
      logger.warn("The following files were not loaded as configurations: {}", unloadedFiles);
    }

    return configurations;
  }

  // load configurations based on a mapping of configuration to file name
  private  List<Configuration> loadConfigurations(File configDirectory, Map<String, String> configFilenameMap){

    List<String> subDirectories=Arrays.asList(configDirectory.list((current, name) -> new File(current, name).isDirectory()));
    String fullSubDirectory;
    String loadedFile;

    //Maps json to GMS objects
    final ObjectMapper objectMapper = CoiObjectMapperFactory.getJsonObjectMapper();
    List<Configuration> configurations = new ArrayList<>();

    for( Map.Entry<String, String> configFilePair : configFilenameMap.entrySet()){
      fullSubDirectory=getSubDirectory(configDirectory, subDirectories, configFilePair.getKey());
      if(fullSubDirectory==null){
        logger.warn("Configuration directory for {} not found", configFilePair.getKey());
      }
      else{
        loadedFile=loadFilesInSubdirectory(fullSubDirectory, List.of(configFilePair.getValue()), configurations, objectMapper);
        if(loadedFile==null){
          logger.warn("File {} not found for configuration {}", configFilePair.getValue(), configFilePair.getKey());
        }
      }
    }

    return configurations;
  }

  //In subdirectory subDir, add the configurations of files in List filenames to configurations.
  private String loadFilesInSubdirectory(String subDir, List<String> filenames, List<Configuration> configurations, ObjectMapper objectMapper){

    List<ConfigurationOption> configOptions = new ArrayList<>();
    String loadedFile=null;

    Collection<String> foundFiles=getFileswithNames(subDir, filenames);
    if (foundFiles.size()>1){
      logger.warn("More than one file found for a single configuration in subdirectory {} based on list of filenames: only one will be loaded", subDir);
    }

    //only load one configuration per subdirectory (configurations in same subdirectory will overwrite each other)
    Optional <String> firstFile=foundFiles.stream().findFirst();
    if(!firstFile.isEmpty()){

      String filename=firstFile.get();

      String [] fullPath = filename.split(File.separator);
      loadedFile=fullPath[fullPath.length-1];

      logger.info("Loading configuration from file {} in subdirectory {}", filename, subDir);
      try {
        configOptions.add(ObjectSerialization.fromFieldMap(
            objectMapper.readValue(getUrl(filename), Map.class),
            ConfigurationOption.class));
      } catch (IllegalArgumentException | IOException e) {
        logger.error("Could not load configuration from disk", e);
      }
      String[] pathComponents = subDir.split(File.separator);
      String configurationName = pathComponents[pathComponents.length - 1];
      configurations.add(Configuration.from(configurationName, configOptions));
    }

    return loadedFile;
  }

  //gets a list of all subdirectories in the base path
  private Collection<String> getSubDirectories(File configDirectory) {
    return Optional
        .ofNullable(
            configDirectory.list((current, name) -> new File(current, name).isDirectory()))
        .stream()
        .flatMap(dirs -> Arrays.stream(dirs).map(d -> configDirectory + File.separator + d))
        .collect(Collectors.toList());
  }

  private String getSubDirectory(File configDirectory, List<String> subDirectories, String subDir){
    if(subDirectories.contains(subDir)){
      return configDirectory + File.separator + subDir;
    }
    return null;
  }


  private Collection<String> getFileswithNames(String path, List<String> filenames) {
    return Optional
        .ofNullable(
            new File(path).list())
        .stream()
        .flatMap(arr -> Arrays.stream(arr))
        .filter(s->filenames.contains(s))
        .map(f->path + File.separator + f)
        .collect(Collectors.toList());

  }

  private URL getUrl(String path) {
    try {
      return new File(path).toURI().toURL();
    } catch (MalformedURLException e) {
      String message = "Could not create URL to file at path: " + path;
      throw new IllegalStateException(message, e);
    }
  }


}