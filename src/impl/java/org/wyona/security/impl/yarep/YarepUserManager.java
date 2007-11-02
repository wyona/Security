package org.wyona.security.impl.yarep;

import java.util.HashMap;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.log4j.Category;
import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.Group;
import org.wyona.security.core.api.IdentityManager;
import org.wyona.security.core.api.User;
import org.wyona.security.core.api.UserManager;
import org.wyona.yarep.core.NoSuchNodeException;
import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryException;

/**
 * The YarepUserManager expects to find all existing users under the node /users.
 * If the node /users does not exist, it will look under the root node.
 * All files which have &lt;user&gt; as root element will be recognized as a user
 * configuration. &lt;identity&gt; is also recognized as a user for backwards 
 * compatibility.
 */
public class YarepUserManager implements UserManager {

    private static Category log = Category.getInstance(YarepUserManager.class);
    
    private Repository identitiesRepository;

    private IdentityManager identityManager;

    private HashMap users;

    /**
     * Constructor.
     * @param identityManager
     * @param identitiesRepository
     * @throws AccessManagementException
     */
    public YarepUserManager(IdentityManager identityManager, Repository identitiesRepository)
            throws AccessManagementException {
        this.identityManager = identityManager;
        this.identitiesRepository = identitiesRepository;
        this.users = new HashMap();
        init();
    }

    /**
     * Finds all user nodes in the repository and instantiates the users.
     *
     * Note re caching: If the UserManager is being instantiated only once at the startup of a server for instance, then the users are basically being cached (see getUser) and changes within the repository by a third pary application will not be noticed.
     *
     * @throws AccessManagementException
     */
    protected void init() throws AccessManagementException {
        try {
            Node usersParentNode = getUsersParentNode();
            Node[] userNodes = usersParentNode.getNodes();
            DefaultConfigurationBuilder configBuilder = new DefaultConfigurationBuilder(true);
            for (int i = 0; i < userNodes.length; i++) {
                if (userNodes[i].isResource()) {
                    try {
                        Configuration config = configBuilder.build(userNodes[i].getInputStream());
                        // also support identity for backwards compatibility
                        if (config.getName().equals(YarepUser.USER) || config.getName().equals("identity")) {
                            YarepUser user = new YarepUser(this.identityManager, userNodes[i]);
                            this.users.put(user.getID(), user);
                        }
                    } catch (Exception e) {
                        String errorMsg = "Could not create user from repository node: " 
                            + userNodes[i].getPath() + ": " + e.getMessage();
                        log.error(errorMsg, e);
                        throw new AccessManagementException(errorMsg, e);
                    }
                }
            }
        } catch (RepositoryException e) {
            String errorMsg = "Could not read users from repository: " + e.getMessage();
            log.error(errorMsg, e);
            throw new AccessManagementException(errorMsg, e);
        }
    }

    /**
     * @see org.wyona.security.core.api.UserManager#createUser(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    public User createUser(String id, String name, String email, String password)
            throws AccessManagementException {
        if (existsUser(id)) {
            throw new AccessManagementException("User " + id + " already exists.");
        }
        try {
            Node usersParentNode = getUsersParentNode();
            User user = new YarepUser(this.identityManager, usersParentNode, id, name, email,
                    password);
            user.save();
            this.users.put(id, user);
            return user;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new AccessManagementException(e.getMessage(), e);
        }
    }

    /**
     * @see org.wyona.security.core.api.UserManager#existsUser(java.lang.String)
     */
    public boolean existsUser(String id) throws AccessManagementException {
        // Check the cache
        if (!this.users.containsKey(id)) {
            // Also check the repository
            try {
                Node usersParentNode = getUsersParentNode();

	        Node[] userNodes = usersParentNode.getNodes();
                for (int i = 0; i < userNodes.length; i++) {
                    if (userNodes[i].isResource()) {
                        log.error("DEBUG: User node: " + userNodes[i].getName());
                    }
                }

                if (usersParentNode.hasNode(id + ".iml")) return true;
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            return false;
        }
        return true;
    }

    /**
     * @see org.wyona.security.core.api.UserManager#getUser(java.lang.String)
     */
    public User getUser(String id) throws AccessManagementException {
        if (!existsUser(id)) {
            // TODO: Check if user might have been created by third party application within repository
            log.error("DEBUG: User should be tried to be received from repo instead from cache: " + id);
            return null;
        }
        return (User) this.users.get(id);
    }

    /**
     * @see org.wyona.security.core.api.UserManager#getUser(java.lang.String, boolean)
     */
    public User getUser(String id, boolean refresh) throws AccessManagementException {
        log.warn("TODO: Refresh not implemented yet!");
        return getUser(id);
    }

    /**
     * @see org.wyona.security.core.api.UserManager#getUsers()
     */
    public User[] getUsers() throws AccessManagementException {
        return (User[]) this.users.values().toArray(new User[this.users.size()]);
    }

    /**
     * @see org.wyona.security.core.api.UserManager#getUsers(boolean)
     */
    public User[] getUsers(boolean refresh) throws AccessManagementException {
        log.warn("TODO: Refresh not implemented yet!");
        return getUsers();
    }

    /**
     * @see org.wyona.security.core.api.UserManager#removeUser(java.lang.String)
     */
    public void removeUser(String id) throws AccessManagementException {
        if (!existsUser(id)) {
            throw new AccessManagementException("User " + id + " does not exist.");
        }
        User user = getUser(id);
        Group[] groups = user.getGroups();
        for (int i=0; i<groups.length; i++) {
            groups[i].removeMember(user);
            groups[i].save();
        }
        this.users.remove(id);
        user.delete();
    }

    /**
     * Gets the repository node which is the parent node of all user nodes.
     * @return parent node of users node.
     * @throws NoSuchNodeException
     * @throws RepositoryException
     */
    protected Node getUsersParentNode() throws NoSuchNodeException, RepositoryException {
        if (this.identitiesRepository.existsNode("/users")) {
            return this.identitiesRepository.getNode("/users");
        }
        // fallback to root node for backwards compatibility:
        return this.identitiesRepository.getNode("/");
    }

}
