/*
    Foilen Infra Resource MariaDB
    https://github.com/foilen/foilen-infra-resource-mariadb
    Copyright (c) 2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.resource.mariadb;

import com.foilen.infra.plugin.v1.model.resource.AbstractIPResource;
import com.foilen.infra.plugin.v1.model.resource.InfraPluginResourceCategory;
import com.google.common.collect.ComparisonChain;

/**
 * This is for a MariaDB Server installation. <br>
 * Links to:
 * <ul>
 * <li>UnixUser: RUN_AS - The user that executes that databse.</li>
 * <li>Machine: (optional / 1) INSTALLED_ON - The machines where to install that database</li>
 * </ul>
 *
 * Manages:
 * <ul>
 * <li>Application: The application that is the database. Will automatically point to the Machines on which it is INSTALLED_ON and run as the RUN_AS user.</li>
 * </ul>
 */
public class MariaDBServer extends AbstractIPResource implements Comparable<MariaDBServer> {

    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_DESCRIPTION = "description";
    public static final String PROPERTY_ROOT_PASSWORD = "rootPassword";

    // Basics
    private String name;
    private String description;

    // Settings
    private String rootPassword;

    public MariaDBServer() {
    }

    public MariaDBServer(String name, String description, String rootPassword) {
        this.name = name;
        this.description = description;
        this.rootPassword = rootPassword;
    }

    @Override
    public int compareTo(MariaDBServer o) {
        return ComparisonChain.start() //
                .compare(this.name, o.name) //
                .result();
    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    @Override
    public InfraPluginResourceCategory getResourceCategory() {
        return InfraPluginResourceCategory.DATABASE;
    }

    @Override
    public String getResourceDescription() {
        return description;
    }

    @Override
    public String getResourceName() {
        return name;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

}
