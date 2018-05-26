/*
    Foilen Infra Resource MariaDB
    https://github.com/foilen/foilen-infra-resource-mariadb
    Copyright (c) 2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.resource.mariadb;

import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditor;
import com.foilen.infra.plugin.v1.core.visual.editor.simpleresourceditor.SimpleResourceEditorDefinition;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonFormatting;
import com.foilen.infra.plugin.v1.core.visual.helper.CommonValidation;
import com.foilen.infra.plugin.v1.model.resource.LinkTypeConstants;

public class MariaDBDatabaseEditor extends SimpleResourceEditor<MariaDBDatabase> {

    public static final String EDITOR_NAME = "MariaDB Database";

    @Override
    protected void getDefinition(SimpleResourceEditorDefinition simpleResourceEditorDefinition) {

        simpleResourceEditorDefinition.addInputText(MariaDBDatabase.PROPERTY_NAME, fieldConfigConsumer -> {
            fieldConfigConsumer.addFormator(CommonFormatting::trimSpacesAround);
            fieldConfigConsumer.addFormator(CommonFormatting::toLowerCase);
            fieldConfigConsumer.addValidator(CommonValidation::validateAlphaNumLower);
            fieldConfigConsumer.addValidator(CommonValidation::validateNotNullOrEmpty);
        });
        simpleResourceEditorDefinition.addInputText(MariaDBDatabase.PROPERTY_DESCRIPTION, fieldConfigConsumer -> {
            fieldConfigConsumer.addFormator(CommonFormatting::trimSpacesAround);
        });

        simpleResourceEditorDefinition.addResources("mariadbServers", LinkTypeConstants.INSTALLED_ON, MariaDBServer.class);

    }

    @Override
    public Class<MariaDBDatabase> getForResourceType() {
        return MariaDBDatabase.class;
    }

}
