package org.ernest.applications.trampoline.controller;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.ernest.applications.trampoline.entities.*;
import org.ernest.applications.trampoline.exceptions.CreatingMicroserviceScriptException;
import org.ernest.applications.trampoline.exceptions.CreatingSettingsFolderException;
import org.ernest.applications.trampoline.exceptions.ReadingEcosystemException;
import org.ernest.applications.trampoline.exceptions.SavingEcosystemException;
import org.ernest.applications.trampoline.model.CreateMicroService;
import org.ernest.applications.trampoline.model.UpdateMicroService;
import org.ernest.applications.trampoline.services.EcosystemManager;
import org.ernest.applications.trampoline.services.FileManager;
import org.ernest.applications.trampoline.services.GitManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    private static final String SETTINGS_VIEW = "settings";

    @Autowired
    EcosystemManager ecosystemManager;

    @Autowired
    FileManager fileManager;

    @Autowired
    GitManager gitManager;

    @RequestMapping("")
    public String getSettingsView(Model model) {
        Ecosystem ecosystem = ecosystemManager.getEcosystem();

        model.addAttribute("microservices", ecosystem.getMicroservices());
        model.addAttribute("microservicesgroups", ecosystem.getMicroservicesGroups());
        model.addAttribute("externalInstances", ecosystem.getExternalInstances());
        model.addAttribute("mavenHomeLocation", ecosystem.getMavenHomeLocation());
        model.addAttribute("mavenBinaryLocationMessage", ecosystem.getMavenBinaryLocation() == null ? "Set Maven Binary Location if necessary. Otherwise it will automatically be searched in a bin folder inside your Maven Home Location" : ecosystem.getMavenBinaryLocation());
        model.addAttribute("mavenHomeLocationMessage", ecosystem.getMavenHomeLocation() == null ? "Please set maven Home Location. Ex: /Users/ernest/Documents/workspace/tools/apache-maven-3.2.1" : ecosystem.getMavenHomeLocation());
        model.addAttribute("gitUsername", gitManager.getRegisteredUsername(ecosystem));
        model.addAttribute("gitSshKeyLocation", gitManager.getSshKeyLocation(ecosystem));
        model.addAttribute("settingsFolder", fileManager.getSettingsFolder());

        return SETTINGS_VIEW;
    }

    @RequestMapping(value = "/setmaven", method = RequestMethod.POST)
    @ResponseBody
    public void setMavenBinaryLocation(@RequestParam(value = "mavenHomeLocationPath") String mavenHomeLocationPath, @RequestParam(value = "mavenBinaryLocationPath") String mavenBinaryLocationPath) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException {
        ecosystemManager.setMavenBinaryLocation(mavenBinaryLocationPath);
        ecosystemManager.setMavenHomeLocation(mavenHomeLocationPath);
    }

    @RequestMapping(value = "/setNewMicroService", method = RequestMethod.POST)
    @ResponseBody
    public void setNewMicroService(@Valid @NotNull CreateMicroService create) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        ecosystemManager.setNewMicroService(create);
    }

    @RequestMapping(value = "/setNewMicroService/git", method = RequestMethod.POST)
    @ResponseBody
    public void setNewMicroServiceFromGit(@RequestParam(value = "gitRepo") @NotBlank String gitRepo,
                                          @RequestParam(value = "destinationFolder") @NotBlank String destinationFolder,
                                          @NotNull @Valid CreateMicroService create) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException, GitAPIException {
        gitManager.cloneRepository(gitRepo, destinationFolder);
        ecosystemManager.setNewMicroService(create);
    }

    @ResponseBody
    @PostMapping(value = "/updateMicroService/{id}")
    public void updateMicroService(@PathVariable(value = "id") String id, @NotNull @Valid UpdateMicroService update) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        ecosystemManager.updateMicroService(id, update);
    }

    @ResponseBody
    @RequestMapping(value = "/removemicroservice", method = RequestMethod.POST)
    public void removeMicroservice(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException {
        ecosystemManager.removeMicroservice(id);
    }

    @RequestMapping(value = "/microserviceinfo", method = RequestMethod.POST)
    @ResponseBody
    public MicroService getMicroserviceInfo(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        return ecosystemManager.getEcosystem().getMicroservices().stream().filter(m -> m.getId().equals(id)).findFirst().get();
    }

    @RequestMapping(value = "/setmicroservicesgroup", method = RequestMethod.POST)
    @ResponseBody
    public void getMicroserviceInfo(@RequestParam(value = "name") String name, @RequestParam(value = "idsMicroservicesGroup[]") List<String> idsMicroservicesGroup, @RequestParam(value = "delaysMicroservicesGroup[]") List<Integer> delaysMicroservicesGroup) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        ecosystemManager.setMicroserviceGroup(name, idsMicroservicesGroup, delaysMicroservicesGroup);
    }

    @RequestMapping(value = "/groupinfo", method = RequestMethod.POST)
    @ResponseBody
    public MicroserviceGroupInfo getGroupInfo(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        MicroservicesGroup microservicesGroup = ecosystemManager.getEcosystem().getMicroservicesGroups().stream().filter(m -> m.getId().equals(id)).findFirst().get();
        List<MicroService> microservices = ecosystemManager.getEcosystem().getMicroservices();

        MicroserviceGroupInfo info = new MicroserviceGroupInfo();
        info.setName(microservicesGroup.getName());

        Map<String, Integer> delays = new HashMap<>();
        for (int index = 0; index < microservicesGroup.getMicroservicesIds().size(); index++) {
            delays.put(microservicesGroup.getMicroservicesIds().get(index), microservicesGroup.getMicroservicesDelays().get(index));
        }

        info.setMicroservicesNames(microservices.stream()
                .filter(m -> microservicesGroup.getMicroservicesIds().contains(m.getId()))
                .map(microservice -> microservice.getName() + " [" + delays.get(microservice.getId()) + " sec]")
                .collect(Collectors.toList()));
        return info;
    }

    @RequestMapping(value = "/removegroup", method = RequestMethod.POST)
    @ResponseBody
    public void removeGroup(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException {
        ecosystemManager.removeGroup(id);
    }

    @RequestMapping(value = "/microservicegitbranches", method = RequestMethod.POST)
    @ResponseBody
    public MicroserviceGitInfo getGitMicroservice(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException, IOException, GitAPIException {
        return gitManager.getMicroseriviceBranches(id);
    }

    @RequestMapping(value = "/checkoutpullbranch", method = RequestMethod.POST)
    @ResponseBody
    public void checkoutAndPull(@RequestParam(value = "id") String id, @RequestParam(value = "branchName") String branchName) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException, IOException, GitAPIException {
        gitManager.checkoutAndPull(id, branchName);
    }

    @RequestMapping(value = "/checkoutpullbranchrestartinstances", method = RequestMethod.POST)
    @ResponseBody
    public void checkoutAndPullAndRestart(@RequestParam(value = "id") String id, @RequestParam(value = "branchName") String branchName) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException, IOException, GitAPIException {
        gitManager.checkoutAndPull(id, branchName);
        List<String> intancesIds = ecosystemManager.getEcosystem().getInstances().stream().filter(i -> i.getMicroserviceId().equals(id)).map(Instance::getId).collect(Collectors.toList());
        intancesIds.forEach(intancesId -> {
            try {
                ecosystemManager.restartInstance(intancesId);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    @RequestMapping(value = "/git/https/config/save", method = RequestMethod.POST)
    @ResponseBody
    public void saveGitHttpsCred(@RequestParam(value = "user") String user,
                                 @RequestParam(value = "pass") String pass) {
        gitManager.saveHttpsCred(user, pass);
    }

    @RequestMapping(value = "/git/ssh/config/save", method = RequestMethod.POST)
    @ResponseBody
    public void saveGitSshCred(@RequestParam(value = "sshKeyLocation", defaultValue = "~/.ssh/id_rsa") String privateKeyLocation,
                               @RequestParam(value = "sshKeyPassword", required = false) String privateKeyPassword) {
        gitManager.saveSshCred(privateKeyLocation, privateKeyPassword);
    }

    @RequestMapping(value = "/git/config/clean", method = RequestMethod.GET)
    @ResponseBody
    public void cleanGitCred() {
        gitManager.cleanCred();
    }

    @RequestMapping(value = "/setexternalinstance", method = RequestMethod.POST)
    @ResponseBody
    public void setExternalInstance(@RequestParam(value = "name") String name, @RequestParam(value = "port") String port,
                                    @RequestParam(value = "actuatorPrefix") String actuatorPrefix, @RequestParam(value = "ip") String ip) throws CreatingSettingsFolderException, ReadingEcosystemException, CreatingMicroserviceScriptException, SavingEcosystemException {
        ecosystemManager.setNewExternalInstance(name, port, actuatorPrefix, ip);
    }

    @RequestMapping(value = "/removeexternalinstance", method = RequestMethod.POST)
    @ResponseBody
    public void removeExternalInstance(@RequestParam(value = "id") String id) throws CreatingSettingsFolderException, ReadingEcosystemException, SavingEcosystemException {
        ecosystemManager.removeExternalInstance(id);
    }


}