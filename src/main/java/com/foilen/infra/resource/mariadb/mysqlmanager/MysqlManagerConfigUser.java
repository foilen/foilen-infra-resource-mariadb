/*
    Foilen Infra Resource MariaDB
    https://github.com/foilen/foilen-infra-resource-mariadb
    Copyright (c) 2018 Foilen (http://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.infra.resource.mariadb.mysqlmanager;

import com.foilen.smalltools.tools.AbstractBasics;

public class MysqlManagerConfigUser extends AbstractBasics {

    private String name;
    private String host;

    public MysqlManagerConfigUser() {
    }

    public MysqlManagerConfigUser(String name, String host) {
        this.name = name;
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public String getName() {
        return name;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setName(String name) {
        this.name = name;
    }

}
