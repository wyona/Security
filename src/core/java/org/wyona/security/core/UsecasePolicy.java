package org.wyona.security.core;

import org.wyona.security.core.api.Identity;
import org.wyona.security.core.GroupPolicy;

import java.util.Vector;

import org.apache.log4j.Logger;

/**
 *
 */
public class UsecasePolicy {

    private static Logger log = Logger.getLogger(UsecasePolicy.class);

    private String name;

    private Vector idps = null;
    private Vector gps = null;

    /**
     *
     */
    public UsecasePolicy(String name) {
        this.name = name;
        idps = new Vector();
        gps = new Vector();
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public void addIdentity(Identity identity, boolean permission) {
        idps.add(new IdentityPolicy(identity, permission));
    }

    /**
     *
     */
    public Identity[] getIdentities() {
        Identity[] ids = new Identity[idps.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = ((IdentityPolicy) idps.elementAt(i)).getIdentity();
        }
        return ids;
    }

    /**
     *
     */
    public IdentityPolicy[] getIdentityPolicies() {
        IdentityPolicy[] ip = new IdentityPolicy[idps.size()];
        for (int i = 0; i < ip.length; i++) {
            ip[i] = (IdentityPolicy) idps.elementAt(i);
        }
        return ip;
    }

    /**
     *
     */
    public void addGroupPolicy(GroupPolicy groupPolicy) {
        gps.add(groupPolicy);
    }

    /**
     *
     */
    public GroupPolicy[] getGroupPolicies() {
        GroupPolicy[] gs = new GroupPolicy[gps.size()];
        for (int i = 0; i < gs.length; i++) {
            gs[i] = (GroupPolicy) gps.elementAt(i);
        }
        return gs;
    }

    /**
     * Merge UsecasePolicy into this UsecasePolicy
     */
    public void merge(UsecasePolicy up) {
        if (!getName().equals(up.getName())) {
            log.error("Usecase policies do not have the same names: " + getName() + " != " + up.getName());
            return;
        }

        // Merge identities
        IdentityPolicy[] upIdps = up.getIdentityPolicies();
        for (int i = 0; i < upIdps.length; i++) {
            boolean identityAlreadyExists = false;
            for (int k = 0; k < idps.size(); k++) {
                if (((IdentityPolicy) idps.elementAt(k)).getIdentity().getUsername().equals(upIdps[i].getIdentity().getUsername())) {
                    identityAlreadyExists = true;
                    break;
                }
            }
            if (!identityAlreadyExists) {
                addIdentity(upIdps[i].getIdentity(), upIdps[i].getPermission());
            }
        }

        // Merge groups
        GroupPolicy[] upGps = up.getGroupPolicies();
        for (int i = 0; i < upGps.length; i++) {
            boolean groupAlreadyExists = false;
            for (int k = 0; k < gps.size(); k++) {
                if (((GroupPolicy) gps.elementAt(k)).getId().equals(upGps[i].getId())) {
                    groupAlreadyExists = true;
                    break;
                }
            }
            if (!groupAlreadyExists) {
                addGroupPolicy(upGps[i]);
            }
        }
    }
}
