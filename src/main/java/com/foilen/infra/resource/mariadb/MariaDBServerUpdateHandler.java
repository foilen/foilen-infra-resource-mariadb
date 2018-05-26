/*
    Foilen Infra Resource MariaDB
    https://github.com/foilen/foilen-infra-resource-mariadb
    Copyright (c) 2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.resource.mariadb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.foilen.infra.plugin.v1.core.context.ChangesContext;
import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.eventhandler.AbstractCommonMethodUpdateEventHandler;
import com.foilen.infra.plugin.v1.core.eventhandler.CommonMethodUpdateEventHandlerContext;
import com.foilen.infra.plugin.v1.core.exception.ProblemException;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinition;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionAssetsBundle;
import com.foilen.infra.plugin.v1.model.base.IPApplicationDefinitionVolume;
import com.foilen.infra.plugin.v1.model.docker.DockerContainerEndpoints;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;
import com.foilen.infra.resource.application.Application;
import com.foilen.infra.resource.machine.Machine;
import com.foilen.infra.resource.mariadb.mysqlmanager.MysqlManagerConfig;
import com.foilen.infra.resource.mariadb.mysqlmanager.MysqlManagerConfigAdmin;
import com.foilen.infra.resource.mariadb.mysqlmanager.MysqlManagerConfigDatabaseGrants;
import com.foilen.infra.resource.mariadb.mysqlmanager.MysqlManagerConfigPermission;
import com.foilen.infra.resource.mariadb.mysqlmanager.MysqlManagerConfigUser;
import com.foilen.infra.resource.unixuser.UnixUser;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.SecureRandomTools;
import com.google.common.base.Strings;

public class MariaDBServerUpdateHandler extends AbstractCommonMethodUpdateEventHandler<MariaDBServer> {

    @Override
    protected void commonHandlerExecute(CommonServicesContext services, ChangesContext changes, CommonMethodUpdateEventHandlerContext<MariaDBServer> context) {

        context.setManagedResourcesUpdateContentIfExists(true);
        context.getManagedResourceTypes().add(Application.class);

        MariaDBServer mariaDBServer = context.getResource();

        String serverName = mariaDBServer.getName();
        logger.debug("[{}] Processing", serverName);

        // Create a root password if none is set
        if (Strings.isNullOrEmpty(mariaDBServer.getRootPassword())) {
            mariaDBServer.setRootPassword(SecureRandomTools.randomHexString(25));
            changes.resourceUpdate(mariaDBServer.getInternalId(), mariaDBServer);
        }

        // Get the user and machines
        List<UnixUser> unixUsers = services.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(mariaDBServer, LinkTypeConstants.RUN_AS, UnixUser.class);
        List<Machine> machines = services.getResourceService().linkFindAllByFromResourceAndLinkTypeAndToResourceClass(mariaDBServer, LinkTypeConstants.INSTALLED_ON, Machine.class);

        logger.debug("[{}] Running as {} on {}", serverName, unixUsers, machines);

        // Prepare the config
        MysqlManagerConfig mysqlManagerConfig = new MysqlManagerConfig();
        mysqlManagerConfig.setAdmin(new MysqlManagerConfigAdmin("root", mariaDBServer.getRootPassword()));
        mysqlManagerConfig.getUsersToIgnore().add(new MysqlManagerConfigUser("root", "%"));

        Map<String, MysqlManagerConfigPermission> userConfigByName = new HashMap<>();

        services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBDatabase.class, LinkTypeConstants.INSTALLED_ON, mariaDBServer).forEach(mariaDBDatabase -> {
            String databaseName = mariaDBDatabase.getName();
            logger.debug("[{}] Has database {}", serverName, databaseName);
            mysqlManagerConfig.getDatabases().add(databaseName);

            // ADMIN
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_ADMIN, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as ADMIN", serverName, databaseName, mariaDBUser.getName());
                List<String> grants = getGrantsByUserAndDatabase(userConfigByName, mariaDBUser, databaseName);
                grants.add("ALTER");
                grants.add("CREATE");
                grants.add("CREATE ROUTINE");
                grants.add("CREATE TEMPORARY TABLES");
                grants.add("CREATE VIEW");
                grants.add("DROP");
                grants.add("EVENT");
                grants.add("INDEX");
                grants.add("LOCK TABLES");
                grants.add("SHOW VIEW");
                grants.add("TRIGGER");
            });

            // READ
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_READ, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as READ", serverName, databaseName, mariaDBUser.getName());
                List<String> grants = getGrantsByUserAndDatabase(userConfigByName, mariaDBUser, databaseName);
                grants.add("SELECT");
            });

            // WRITE
            services.getResourceService().linkFindAllByFromResourceClassAndLinkTypeAndToResource(MariaDBUser.class, MariaDBUser.LINK_TYPE_WRITE, mariaDBDatabase).forEach(mariaDBUser -> {
                logger.debug("[{}] Database {} has user {} as WRITE", serverName, databaseName, mariaDBUser.getName());
                List<String> grants = getGrantsByUserAndDatabase(userConfigByName, mariaDBUser, databaseName);
                grants.add("INSERT");
                grants.add("UPDATE");
                grants.add("DELETE");
            });

        });

        // Apply users permissions
        userConfigByName.values().forEach(userConfig -> {
            mysqlManagerConfig.getUsersPermissions().add(userConfig);
        });

        if (unixUsers.size() > 1) {
            throw new ProblemException("Cannot run as more than 1 unix user");
        }
        if (machines.size() > 1) {
            throw new ProblemException("Cannot be installed on multiple machines");
        }
        if (unixUsers.size() == 1) {

            UnixUser unixUser = unixUsers.get(0);

            // Application
            Application application = new Application();
            application.setName(serverName);
            application.setDescription(mariaDBServer.getDescription());

            IPApplicationDefinition applicationDefinition = application.getApplicationDefinition();

            applicationDefinition.setFrom("foilen/fcloud-docker-mariadb:10.3.5-1.1.0-001");

            applicationDefinition.addService("app", "/mariadb-start.sh");
            IPApplicationDefinitionAssetsBundle assetsBundle = applicationDefinition.addAssetsBundle();
            applicationDefinition.addContainerUserToChangeId("mysql", unixUser.getId());

            applicationDefinition.addPortEndpoint(3306, DockerContainerEndpoints.MYSQL_TCP);

            applicationDefinition.setRunAs(unixUser.getId());

            // Data folder
            String baseFolder = unixUser.getHomeFolder() + "/mysql/" + serverName;
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/data", "/var/lib/mysql", unixUser.getId(), unixUser.getId(), "770"));

            // Run folder
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/run", "/var/run/mysqld/", unixUser.getId(), unixUser.getId(), "770"));

            // Save the root password
            applicationDefinition.addVolume(new IPApplicationDefinitionVolume(baseFolder + "/config", "/volumes/config/", unixUser.getId(), unixUser.getId(), "770"));
            String newPass = mariaDBServer.getRootPassword();
            assetsBundle.addAssetContent("/newPass", newPass);
            assetsBundle.addAssetContent("/newPass.cnf", "[client]\npassword=" + newPass);

            // Save the database config for the manager
            applicationDefinition.addCopyWhenStartedContent("/manager-config.json", JsonTools.prettyPrint(mysqlManagerConfig));
            applicationDefinition.addExecuteWhenStartedCommand("/mariadb-update-manager.sh");

            // Manage the app
            context.getManagedResources().add(application);

            // add Machine INSTALLED_ON to mysqlApplicationDefinition (only 0 or 1)
            if (machines.size() == 1) {
                Machine machine = machines.get(0);
                changes.linkAdd(application, LinkTypeConstants.INSTALLED_ON, machine);
            }

            // add UnixUser RUN_AS to mysqlApplicationDefinition (only 1)
            changes.linkAdd(application, LinkTypeConstants.RUN_AS, unixUser);
        }
    }

    private List<String> getGrantsByUserAndDatabase(Map<String, MysqlManagerConfigPermission> userConfigByName, MariaDBUser mariaDBUser, String databaseName) {
        MysqlManagerConfigPermission userConfig = userConfigByName.get(mariaDBUser.getName());
        if (userConfig == null) {
            userConfig = new MysqlManagerConfigPermission(mariaDBUser.getName(), "%", mariaDBUser.getPassword());
            userConfigByName.put(mariaDBUser.getName(), userConfig);
        }

        Optional<MysqlManagerConfigDatabaseGrants> grantsOptional = userConfig.getDatabaseGrants().stream().filter(it -> databaseName.equals(it.getDatabaseName())).findAny();
        List<String> grants;
        if (grantsOptional.isPresent()) {
            grants = grantsOptional.get().getGrants();
        } else {
            MysqlManagerConfigDatabaseGrants databaseGrants = new MysqlManagerConfigDatabaseGrants(databaseName);
            grants = databaseGrants.getGrants();
            userConfig.getDatabaseGrants().add(databaseGrants);
        }
        return grants;

    }

    @Override
    public Class<MariaDBServer> supportedClass() {
        return MariaDBServer.class;
    }

}
