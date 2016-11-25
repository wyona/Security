package org.wyona.security.impl.yarep;

import java.util.ArrayList;
import java.util.Vector;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.Group;
import org.wyona.security.core.api.UserManager;
import org.wyona.security.core.api.GroupManager;
import org.wyona.security.core.api.Item;
import org.wyona.security.core.api.User;

import org.wyona.yarep.core.Node;

/**
 * Group implementation based on Yarep
 */
public class YarepGroup extends YarepItem implements Group {
    protected static final Logger log = LogManager.getLogger(YarepGroup.class);
    
    private java.util.List<String> memberUserIDs;
    private java.util.List<String> memberGroupIDs;
    private java.util.List<String> parentGroupIDs;

    public static final String MEMBERS = "members";
    public static final String MEMBER = "member";
    public static final String MEMBER_ID = "id";

    public static final String PARENT_GROUPS_TAG_NAME = "parent-groups";
    public static final String PARENT_GROUP_TAG_NAME = "group";
    public static final String PARENT_GROUP_ID_ATTR_NAME = "id";

    private static final String MEMBER_TYPE = "type";
    private static final String USER_TYPE = "user";
    private static final String GROUP_TYPE = "group";
    
    public static final String GROUP_TAG_NAME = "group";

    /**
     * Instantiates an existing YarepGroup from a repository node.
     * 
     * @param userManager User manager
     * @param groupManager Group manager
     * @param node Yarep node containing group data
     * @throws AccessManagementException
     */
    public YarepGroup(UserManager userManager, GroupManager groupManager, Node node) throws AccessManagementException {
        super(userManager, groupManager, node); // this will call configure()
    }

    /**
     * @param userManager User manager
     * @param groupManager Group manager
     * @param id Group ID
     * @param name Group name
     */
    public YarepGroup(UserManager userManager, GroupManager groupManager, String id, String name) {
        super(userManager, groupManager, id, name);

        this.memberUserIDs = new ArrayList<String>();
        this.memberGroupIDs = new ArrayList<String>();
        this.parentGroupIDs = new ArrayList<String>();
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    protected void configure(Configuration config) throws ConfigurationException, AccessManagementException {
        setID(config.getAttribute(ID));
        setName(config.getChild(NAME, false).getValue());

        this.memberUserIDs = new ArrayList<String>();
        this.memberGroupIDs = new ArrayList<String>();

        Configuration[] memberNodes = config.getChild(MEMBERS).getChildren(MEMBER);

        for (int i = 0; i < memberNodes.length; i++) {
            String id = memberNodes[i].getAttribute(MEMBER_ID);
            // type attribute is optional and helps to differentiate between users and groups
            String type = memberNodes[i].getAttribute(MEMBER_TYPE, USER_TYPE);
            if (type.equals(USER_TYPE)) {
                if (getUserManager() != null) {
                    log.debug("Check whether user '" + id + "' exists...");
                    if (getUserManager().existsUser(id)) {
                        log.debug("Append user '" + id + "' to internal member list of group: " + getID());
                        memberUserIDs.add(id);
                    } else {
                        log.warn("No user with id '" + id + "' exists, but is referenced within group '" + getID() + "' (" + getName() + ")");
                    }
                } else {
                    log.error("User manager is NULL! User " + id + " cannot be added to group " + getID());
                }
            } else if (type.equals(GROUP_TYPE)) {
                log.debug("Subgroup '" + id + "' within group '" + getID() + "' detected! Beware of loops when adding groups within groups!");
                if (getGroupManager() != null) {
                    if (getGroupManager().existsGroup(id)) {
                        memberGroupIDs.add(id);
                    } else {
                        log.warn("No group with id '" + id + "' exists, but is referenced within group '" + getID() + "' (" + getName() + ")");
                    }
                } else {
                    log.error("Group manager is NULL! Group " + id + " cannot be added to group " + getID());
                }
            } else {
                log.error("No such member/item type: " + type);
            }
        }

        Configuration parentsNode = config.getChild(PARENT_GROUPS_TAG_NAME, false);
        if (parentsNode != null) {
            Configuration[] parentNodes = parentsNode.getChildren(PARENT_GROUP_TAG_NAME);
            if (parentNodes != null && parentNodes.length > 0) {
                parentGroupIDs = new ArrayList<String>();
                for (int i = 0; i < parentNodes.length; i++) {
                    parentGroupIDs.add(parentNodes[i].getAttribute(PARENT_GROUP_ID_ATTR_NAME));
                }
            } else {
                parentGroupIDs = new ArrayList<String>();
                log.debug("Group '" + getID() + "'  does not seem to have any parent groups.");
            }
        } else {
            log.warn("Group '" + getID() + "' does seem to be an instance of a previous version without '" + PARENT_GROUPS_TAG_NAME + "' tag and hence will be migrated automatically.");
            //if (fixParentGroupIndex) {
            if (true) {
                log.warn("Fix parent group index ...");
                parentGroupIDs = new ArrayList<String>();
                Node[] allGroupNodes = ((YarepGroupManager) getGroupManager()).getAllGroupNodes();
                for (int i = 0; i < allGroupNodes.length; i++) {
                    if (YarepGroup.isGroupMember(allGroupNodes[i], getID())) {
                        try {
                            parentGroupIDs.add(YarepGroup.getGroupID(allGroupNodes[i]));
                        } catch(Exception e) {
                            log.error(e, e);
                        }
                    }
                }
                save();
            }
        }
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#createConfiguration()
     */
    protected Configuration createConfiguration() throws AccessManagementException {
        DefaultConfiguration config = new DefaultConfiguration(GROUP_TAG_NAME);
        config.setAttribute(ID, getID());
        DefaultConfiguration nameNode = new DefaultConfiguration(NAME);
        nameNode.setValue(getName());
        config.addChild(nameNode);

        DefaultConfiguration membersNode = new DefaultConfiguration(MEMBERS);
        config.addChild(membersNode);

        for (int i = 0; i < memberUserIDs.size(); i++) {
            DefaultConfiguration memberNode = new DefaultConfiguration(MEMBER);
            memberNode.setAttribute(MEMBER_ID, (String)memberUserIDs.get(i));
            memberNode.setAttribute(MEMBER_TYPE, "user");
            membersNode.addChild(memberNode);
        }
        for (int i = 0; i < memberGroupIDs.size(); i++) {
            DefaultConfiguration memberNode = new DefaultConfiguration(MEMBER);
            memberNode.setAttribute(MEMBER_ID, (String)memberGroupIDs.get(i));
            memberNode.setAttribute(MEMBER_TYPE, "group");
            membersNode.addChild(memberNode);
        }

        DefaultConfiguration parentGroupsNode = new DefaultConfiguration(PARENT_GROUPS_TAG_NAME);
        config.addChild(parentGroupsNode);
        if (parentGroupIDs != null && parentGroupIDs.size() > 0) {
            for (int i = 0; i < parentGroupIDs.size(); i++) {
                DefaultConfiguration parentGroupNode = new DefaultConfiguration(PARENT_GROUP_TAG_NAME);
                parentGroupNode.setAttribute(PARENT_GROUP_ID_ATTR_NAME, (String) parentGroupIDs.get(i));
                parentGroupsNode.addChild(parentGroupNode);
            }
        } else {
            parentGroupIDs = new ArrayList();
        }

        return config;
    }

    /**
     * @see org.wyona.security.core.api.Group#addMember(org.wyona.security.core.api.Item)
     */
    public void addMember(Item item) throws AccessManagementException {
        if (null != item){
            if (item instanceof User) {
                log.debug("Add user '" + item.getID() + "' to group: " + getID());
                memberUserIDs.add(item.getID());
                log.debug("Add group '" + getID() + "' to user: " + item.getID());
            
                try {
                    ((YarepUser) item).addGroup(getID());
                } catch (ClassCastException e) {
                    log.error(e, e);
                    throw new AccessManagementException("Item '" + item.getClass().getName() + "' is not instance of YarepUser, hence group '" + getID() + "' won't be added to item '" + item.getID() + "'!");
                }
                save();
            } else if (item instanceof Group) {
                if (wouldCreateGroupLoop(getID(), (Group)item)) {
                    throw new AccessManagementException("Adding group '" + item.getID() + "' to group '" + getID() + "' would create a loop!");
                }
                memberGroupIDs.add(item.getID());
                ((YarepGroup) item).addParentGroup(getID()); // INFO: Add back/bi-directional link
                save();
            } else {
                log.warn("Item '" + item.getID() + "' is neither user nor group: " + item.getClass().getName());
            }
        } else {
            log.warn("Item is null. Can't add item (user or group) to the group '" + getID() + "'");
        }
    }

    /**
     * @see org.wyona.security.core.api.Group#getParents()
     */
    public Group[] getParents() throws AccessManagementException {
        if (parentGroupIDs != null) {
            if (parentGroupIDs.size() > 0) {
                Group[] parents = new Group[parentGroupIDs.size()];
                for (int i = 0; i < parentGroupIDs.size(); i++) {
                    parents[i] = getGroupManager().getGroup(parentGroupIDs.get(i));
                }
                return parents;
            } else {
                log.debug("Group '" + getID() + "' does not seem to have any parents.");
                return null;
            }
        } else {
            log.warn("DEPRECATED: Performance and scalability!");
            Group[] allGroups = getGroupManager().getGroups();
            Vector parents = new Vector();
            for (int i = 0; i < allGroups.length; i++) {
                Item[] members = allGroups[i].getMembers();
                for (int k = 0; k < members.length; k++) {
                    if (members[k] instanceof Group && ((Group)members[k]).getID().equals(getID())) {
                        parents.add(allGroups[i]);
                    }
                }
            }

            Group[] g = new Group[parents.size()];
            for (int i = 0; i < g.length; i++) {
                g[i] = (Group) parents.elementAt(i);
            }
            return g;
        }
    }

    /**
     * @see org.wyona.security.core.api.Group#getMembers()
     */
    public Item[] getMembers() throws AccessManagementException {
        java.util.List<Item> members = new ArrayList<Item>();
        for (int i = 0; i < memberUserIDs.size(); i++) {
            members.add(getUserManager().getUser((String)memberUserIDs.get(i)));
        }
        for (int i = 0; i < memberGroupIDs.size(); i++) {
            members.add(getGroupManager().getGroup((String)memberGroupIDs.get(i)));
        }
        return (Item[])members.toArray(new Item[members.size()]);
    }

    /**
     * @see org.wyona.security.core.api.Group#isMember(org.wyona.security.core.api.Item)
     */
    public boolean isMember(Item item) throws AccessManagementException {
        return item != null && (memberUserIDs.contains(item.getID()) || memberGroupIDs.contains(item.getID()));
    }

    /**
     * @see org.wyona.security.core.api.Group#removeMember(org.wyona.security.core.api.Item)
     */
    public void removeMember(Item item) throws AccessManagementException {
        if (null != item) {
            if (item instanceof User) {
                memberUserIDs.remove(item.getID());
                ((YarepUser) item).removeGroup(getID());
                log.warn("User has been removed: " + item.getID());
            } else if (item instanceof Group) {
                memberGroupIDs.remove(item.getID());
                ((YarepGroup) item).removeParentGroup(getID());
                log.warn("Group has been removed: " + item.getID());
            } else {
                log.warn("Item '" + item.getID() + "' is neither user nor group: " + item.getClass().getName());
            }
        } else {
            log.warn("Item is null. Can't remove item/user from the group '" + getID() + "'");
        }
    }
    
    /**
     * Two groups are equal if they have the same id.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Group) {
            String id1;
            try {
                id1 = getID();
                String id2 = ((Group)obj).getID();
                return id1.equals(id2);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return false;
    }

    /**
     * Check whether user is member of a particular group
     * @param node Yarep node of group
     * @param id User ID
     */
    public static boolean isUserMember(Node node, String id) {
        try {
            DefaultConfigurationBuilder configBuilder = new DefaultConfigurationBuilder(true);
            Configuration config = configBuilder.build(node.getInputStream());

            if (!config.getName().equals(GROUP_TAG_NAME)) {
                log.warn("Node '" + node.getPath() + "' does not seem to be a group node!");
                return false;
            }

            Configuration[] memberNodes = config.getChild(MEMBERS).getChildren(MEMBER);

            for (int i = 0; i < memberNodes.length; i++) {
                String memberID = memberNodes[i].getAttribute(MEMBER_ID);
                // type attribute is optional and helps to differentiate between users and groups
                String type = memberNodes[i].getAttribute(MEMBER_TYPE, USER_TYPE);
                if (type.equals(USER_TYPE) && memberID.equals(id)) {
                    return true;
                }
            }
            return false;
        } catch(Exception e) {
            log.error(e, e);
            return false;
        }
    }

    /**
     * Check whether group is member of a particular group
     * @param node Yarep node of group
     * @param id Group ID
     */
    public static boolean isGroupMember(Node node, String id) {
        try {
            DefaultConfigurationBuilder configBuilder = new DefaultConfigurationBuilder(true);
            Configuration config = configBuilder.build(node.getInputStream());

            if (!config.getName().equals(GROUP_TAG_NAME)) {
                log.warn("Node '" + node.getPath() + "' does not seem to be a group node!");
                return false;
            }

            Configuration[] memberNodes = config.getChild(MEMBERS).getChildren(MEMBER);

            for (int i = 0; i < memberNodes.length; i++) {
                String memberID = memberNodes[i].getAttribute(MEMBER_ID);
                // type attribute is optional and helps to differentiate between users and groups
                String type = memberNodes[i].getAttribute(MEMBER_TYPE, GROUP_TYPE);
                if (type.equals(GROUP_TYPE) && memberID.equals(id)) {
                    return true;
                }
            }
            return false;
        } catch(Exception e) {
            log.error(e, e);
            return false;
        }
    }

    /**
     * Get group ID
     * @param node Yarep node of group
     */
    public static String getGroupID(Node node) throws Exception {
        String nodeName = node.getName();
        return nodeName.substring(0, nodeName.indexOf(".xml"));
    }

    /**
     * Add parent group (creating a bi-directional link)
     * @param id Parent group ID
     */
    private void addParentGroup(String id) throws AccessManagementException {
        log.debug("Add parent group '" + id + "' to group: " + getID());
        if (parentGroupIDs == null) {
            throw new AccessManagementException("Group '" + getID() + "' has parent groups not initialized yet!");
        }
        if (parentGroupIDs.indexOf(id) < 0) {
            parentGroupIDs.add(id);
        } else {
            throw new AccessManagementException("Group '" + id + "' already is parent of group '" + getID() + "'!");
        }
        save();
    }

    /**
     * Remove parent group (remove bi-directional link)
     * @param id Parent group ID
     */
    private void removeParentGroup(String id) throws AccessManagementException {
        if (parentGroupIDs != null) {
            if (parentGroupIDs.indexOf(id) >= 0) {
                parentGroupIDs.remove(parentGroupIDs.indexOf(id));
            } else {
                throw new AccessManagementException("Group '" + id + "' is not parent of group '" + getID() + "'!");
            }
        } else {
            throw new AccessManagementException("Group '" + getID() + "' has no parent groups!");
        }
        save();
    }

    /**
     * @see org.wyona.security.core.api.Item#delete()
     */
    @Override
    public void delete() throws AccessManagementException {
        log.info("Remove members from this group: " + getID());
        Item[] members = getMembers();
        for (int i = 0; i < members.length; i++) {
            Item item = members[i];
            this.removeMember(item);
        }
        super.delete();
    }

    /**
     * Check whether adding one group as a member to another group would create a loop
     * @param parentID ID of parent group
     * @param memberGroup Intended member group which might generate a loop by adding it to the parent group as member
     * @return true if a loop would be created, false otherwise
     */
    private boolean wouldCreateGroupLoop(String parentID, Group memberGroup) throws AccessManagementException {
        log.info("Make sure no loop is being created!");
        if (parentID.equals(memberGroup.getID())) {
            log.warn("Parent group and intended member group are the same: " + parentID);
            return true;
        }
        String[] groupMemberIDs = org.wyona.security.util.GroupUtil.getGroupIDs(memberGroup, true);
        if (groupMemberIDs != null) {
            for (int i = 0; i < groupMemberIDs.length; i++) {
                if (groupMemberIDs[i].equals(parentID)) {
                    log.warn("Intended member group '" + memberGroup.getID() + "' contains parent group '" + parentID + "' already as sub-group!");
                    return true;
                }
            }
        }
        return false;
    }
}
