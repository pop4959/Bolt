package org.popcraft.bolt.migration.lwc.data;

import java.util.List;

public class Data {
    private List<DataFlag> flags;
    private List<DataRights> rights;

    public List<DataFlag> getFlags() {
        return flags;
    }

    public void setFlags(List<DataFlag> flags) {
        this.flags = flags;
    }

    public List<DataRights> getRights() {
        return rights;
    }

    public void setRights(List<DataRights> rights) {
        this.rights = rights;
    }
}
