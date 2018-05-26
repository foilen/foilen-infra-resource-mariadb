/*
    Foilen Infra Resource MariaDB
    https://github.com/foilen/foilen-infra-resource-mariadb
    Copyright (c) 2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.resource.mariadb;

import java.util.Arrays;

import com.foilen.infra.plugin.v1.core.context.CommonServicesContext;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionProvider;
import com.foilen.infra.plugin.v1.core.plugin.IPPluginDefinitionV1;

public class FoilenMariaDBPluginDefinitionProvider implements IPPluginDefinitionProvider {

    @Override
    public IPPluginDefinitionV1 getIPPluginDefinition() {
        IPPluginDefinitionV1 pluginDefinitionV1 = new IPPluginDefinitionV1("Foilen", "MariaDB", "To manage MariaDB databases", "1.0.0");

        pluginDefinitionV1.addCustomResource(MariaDBServer.class, "MariaDB Server", //
                Arrays.asList(MariaDBServer.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBServer.PROPERTY_NAME, //
                        MariaDBServer.PROPERTY_DESCRIPTION //
                ));

        pluginDefinitionV1.addCustomResource(MariaDBDatabase.class, "MariaDB Database", //
                Arrays.asList(MariaDBDatabase.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBDatabase.PROPERTY_NAME, //
                        MariaDBDatabase.PROPERTY_DESCRIPTION //
                ));

        pluginDefinitionV1.addCustomResource(MariaDBUser.class, "MariaDB User", //
                Arrays.asList(MariaDBUser.PROPERTY_NAME), //
                Arrays.asList( //
                        MariaDBUser.PROPERTY_NAME, //
                        MariaDBUser.PROPERTY_DESCRIPTION //
                ));

        // Resource editors
        pluginDefinitionV1.addTranslations("/com/foilen/infra/resource/mariadb/messages");
        pluginDefinitionV1.addResourceEditor(new MariaDBDatabaseEditor(), MariaDBDatabaseEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new MariaDBServerEditor(), MariaDBServerEditor.EDITOR_NAME);
        pluginDefinitionV1.addResourceEditor(new MariaDBUserEditor(), MariaDBUserEditor.EDITOR_NAME);

        // Update events
        pluginDefinitionV1.addUpdateHandler(new MariaDBServerUpdateHandler());

        return pluginDefinitionV1;
    }

    @Override
    public void initialize(CommonServicesContext commonServicesContext) {
    }

}
