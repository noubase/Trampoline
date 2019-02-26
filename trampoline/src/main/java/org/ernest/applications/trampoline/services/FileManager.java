package org.ernest.applications.trampoline.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.ernest.applications.trampoline.entities.BuildTools;
import org.ernest.applications.trampoline.entities.Ecosystem;
import org.ernest.applications.trampoline.entities.MicroService;
import org.ernest.applications.trampoline.exceptions.*;
import org.ernest.applications.trampoline.utils.ScriptContentsProvider;
import org.ernest.applications.trampoline.utils.VMParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class FileManager {

    private final Logger log = LoggerFactory.getLogger(FileManager.class);

    @Value("${settings.folder.path.mac}")
    private String settingsFolderPathMac;

    @Value("${settings.folder.path.linux}")
    private String settingsFolderPathLinux;

    @Value("${settings.folder.path.windows}")
    private String settingsFolderPathWindows;

    @Value("${settings.file.name}")
    private String settingsFileName;

    @Value("${trampoline.version}")
    private float currentVersion;

    private final Map<String, Process> processes = new HashMap<>();

    public Ecosystem getEcosystem() throws CreatingSettingsFolderException, ReadingEcosystemException {
        checkIfFileExistsAndCreatedIfNeeded();
        Ecosystem ecosystem = null;

        try {
            ecosystem = new Gson().fromJson(FileUtils.readFileToString(new File(getSettingsFolder() + "/" + settingsFileName)), Ecosystem.class);
        } catch (JsonSyntaxException | IOException e) {
            e.printStackTrace();
            throw new ReadingEcosystemException();
        }

        updateMicroservicesInformationStored(ecosystem);

        return ecosystem;
    }

    private void updateMicroservicesInformationStored(Ecosystem ecosystem) {
        boolean ecosystemChanged = false;
        ecosystemChanged = createBasicInformation(ecosystem, ecosystemChanged);
        ecosystemChanged = createBuildTool(ecosystem, ecosystemChanged);
        ecosystemChanged = createVersion(ecosystem, ecosystemChanged, currentVersion);
        ecosystemChanged = createIp(ecosystem, ecosystemChanged);
        ecosystemChanged = createGroupDelays(ecosystem, ecosystemChanged);

        if (ecosystemChanged) {
            saveEcosystem(ecosystem);
        }
    }

    private boolean createGroupDelays(Ecosystem ecosystem, boolean ecosystemChanged) {
        if (ecosystem.getMicroservicesGroups().stream().anyMatch(i -> i.getMicroservicesDelays() == null)) {
            ecosystem.getMicroservicesGroups().stream().filter(i -> i.getMicroservicesDelays() == null).forEach(i -> i.setMicroservicesDelays(i.getMicroservicesIds().stream().map(s -> new Integer(0)).collect(Collectors.toList())));
            ecosystemChanged = true;
        }
        return ecosystemChanged;
    }

    private boolean createIp(Ecosystem ecosystem, boolean ecosystemChanged) {
        if (ecosystem.getInstances().stream().anyMatch(i -> i.getIp() == null)) {
            ecosystem.getInstances().stream().filter(i -> i.getIp() == null).forEach(i -> i.setIp("127.0.0.1"));
            ecosystemChanged = true;
        }
        return ecosystemChanged;
    }

    private boolean createVersion(Ecosystem ecosystem, boolean ecosystemChanged, float currentVersion) {
        if (ecosystem.getMicroservices().stream().anyMatch(m -> m.getVersion() == null)) {
            ecosystem.getMicroservices().stream().filter(m -> m.getVersion() == null).forEach(m -> {
                m.setVersion(currentVersion);
                createScript(m);
            });
            ecosystemChanged = true;
        }
        return ecosystemChanged;
    }

    private boolean createBuildTool(Ecosystem ecosystem, boolean ecosystemChanged) {
        if (ecosystem.getMicroservices().stream().anyMatch(m -> m.getBuildTool() == null)) {
            ecosystem.getMicroservices().stream().filter(m -> m.getBuildTool() == null).forEach(m -> m.setBuildTool(BuildTools.MAVEN));
            ecosystemChanged = true;
        }
        return ecosystemChanged;
    }

    private boolean createBasicInformation(Ecosystem ecosystem, boolean ecosystemChanged) {
        if (ecosystem.getMicroservices().stream().anyMatch(m -> m.getActuatorPrefix() == null || m.getVmArguments() == null)) {
            ecosystem.getMicroservices().stream().filter(m -> m.getActuatorPrefix() == null || m.getVmArguments() == null).forEach(m -> {
                m.setVmArguments("");
                m.setActuatorPrefix("");
                m.setBuildTool(BuildTools.MAVEN);
                createScript(m);
            });
            ecosystemChanged = true;
        }
        return ecosystemChanged;
    }

    public void saveEcosystem(Ecosystem ecosystem) throws SavingEcosystemException {
        log.info("Saving Ecosystem");
        try {
            FileUtils.writeStringToFile(new File(getSettingsFolder() + "/" + settingsFileName), new Gson().toJson(ecosystem));
        } catch (IOException e) {
            e.printStackTrace();
            throw new SavingEcosystemException();
        }
    }

    public void stopScript(String microServiceId) throws ShuttingDownInstanceException {
        try {
            File shFile = Paths.get(getSettingsFolder(), microServiceId + ".sh").toFile();
                if (shFile.exists())
                new ProcessBuilder("sh", getSettingsFolder() + "/" + microServiceId + ".sh", "stop").start();
            else
                log.warn("sh file [{}] does not exists", shFile.getAbsolutePath());
            processes.remove(microServiceId);
        } catch (IOException e) {
            throw new ShuttingDownInstanceException();
        }
    }

    public void runScript(MicroService microservice, String mavenBinaryLocation, String mavenHomeLocation, String port, String vmArguments, String appArguments) throws RunningMicroserviceScriptException {
        try {
            String binaryLocation = (mavenBinaryLocation != null && mavenBinaryLocation.trim().length() > 0) ? mavenBinaryLocation : mavenHomeLocation + "/bin";
            if (System.getProperties().getProperty("os.name").contains("Windows")) {
                String commands = FileUtils.readFileToString(new File(getSettingsFolder() + "/" + microservice.getId() + ".txt"));
                commands = commands.replace("#port", port);

                if (microservice.getBuildTool().equals(BuildTools.MAVEN)) {
                    commands = commands.replace("#mavenBinaryLocation", binaryLocation);
                    commands = commands.replace("#mavenHomeLocation", mavenHomeLocation);
                    commands = commands.replace("#vmArguments", vmArguments);
                } else {
                    commands = commands.replace("#vmArguments", VMParser.toWindowsEnviromentVariables(vmArguments));
                }
                log.info("Starting [" + microservice.getId() + "] with following command [" + commands + "]");
                Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"" + commands + "\"");
            } else {
                String scriptPath = getSettingsFolder() + "/" + microservice.getId() + ".sh";
                if (microservice.getBuildTool().equals(BuildTools.MAVEN)) {
                    Optional.ofNullable(processes.get(microservice.getId()))
                            .ifPresent(process -> {
                                process.destroy();
                                log.warn("Destroy previous process for [{}]", microservice.getId());
                            });
                    log.info("Script path: [{}]", scriptPath);
                    log.info("Port: [{}]", port);
                    log.info("VM Arguments: [{}]", vmArguments);
                    log.info("App Arguments: [{}]", appArguments);
                    ProcessBuilder builder = new ProcessBuilder("sh", scriptPath, "start", port, vmArguments, appArguments);
                    Process process = builder.start();
                    try {
                        log.info("Output: \r\n[{}]", IOUtils.toString(process.getInputStream()));
                    } catch (Exception e) {
                        log.warn("Failed [{}]", e);
                    }
                    processes.put(microservice.getId(), process);
                } else {
                    Runtime.getRuntime().exec("chmod 777 " + microservice.getPomLocation() + "//gradlew");
                    new ProcessBuilder("sh", scriptPath, port, VMParser.toUnixEnviromentVariables(vmArguments)).start();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RunningMicroserviceScriptException();
        }
    }

    public void createScript(MicroService microservice) throws CreatingMicroserviceScriptException {
        log.info("Creating deployment script for microservice [{}]", microservice.getId());
        try {
            boolean isWin = System.getProperties().getProperty("os.name").contains("Windows");
            String ext = isWin ? ".txt" : ".sh";
            String template;
            if (isWin) {
                try {
                    FileUtils.forceDelete(new File(getSettingsFolder() + "/" + microservice.getId() + ".txt"));
                } catch (Exception e) {
                }

                if (microservice.getBuildTool().equals(BuildTools.MAVEN)) {
                    template = ScriptContentsProvider.getMavenWindows(microservice.getPomLocation());
                } else {
                    template = ScriptContentsProvider.getGradleWindows(microservice.getPomLocation());
                }
            } else {
                try {
                    FileUtils.forceDelete(new File(getSettingsFolder() + "/" + microservice.getId() + ".sh"));
                } catch (Exception e) {
                }

                if (microservice.getBuildTool().equals(BuildTools.MAVEN)) {
                    template = ScriptContentsProvider.getMavenUnix(microservice);
                } else {
                    template = ScriptContentsProvider.getGradleUnix(microservice.getPomLocation());
                }
            }
            Path path = Paths.get(getSettingsFolder(), microservice.getId() + ext);
            FileUtils.writeStringToFile(path.toFile(), template);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CreatingMicroserviceScriptException();
        }
    }

    public String getSettingsFolder() {
        if (System.getProperties().getProperty("os.name").contains("Mac")) {
            return settingsFolderPathMac.replaceAll("#userName", System.getProperties().getProperty("user.name"));
        } else if (System.getProperties().getProperty("os.name").contains("Windows")) {
            return settingsFolderPathWindows;
        } else {
            return settingsFolderPathLinux.replaceAll("#userName", System.getProperties().getProperty("user.name"));
        }
    }

    private void checkIfFileExistsAndCreatedIfNeeded() throws CreatingSettingsFolderException {
        try {
            File file = new File(getSettingsFolder());
            if (!file.exists()) {
                file.mkdirs();
                FileUtils.writeStringToFile(new File(getSettingsFolder() + "/" + settingsFileName), new Gson().toJson(new Ecosystem()));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new CreatingSettingsFolderException();
        }
    }


}
