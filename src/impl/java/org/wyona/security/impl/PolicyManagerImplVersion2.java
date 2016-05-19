package org.wyona.security.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.wyona.commons.io.Path;
import org.wyona.commons.io.PathUtil;
import org.wyona.security.core.AuthorizationException;
import org.wyona.security.core.GroupPolicy;
import org.wyona.security.core.IdentityPolicy;
import org.wyona.security.core.api.Identity;
import org.wyona.security.core.api.Policy;
import org.wyona.security.core.api.PolicyManager;
import org.wyona.security.core.api.Role;
import org.wyona.security.core.api.Usecase;

import org.wyona.yarep.core.NoSuchNodeException;
import org.wyona.yarep.core.Node;
import org.wyona.yarep.core.Repository;
import org.wyona.yarep.core.RepositoryException;
import org.wyona.yarep.core.RepositoryFactory;
import org.wyona.yarep.util.RepoPath;
import org.wyona.yarep.util.YarepUtil;
import org.xml.sax.SAXException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.commons.io.FilenameUtils;

/**
 * Policy manager implementation version 2
 */
public class PolicyManagerImplVersion2 implements PolicyManager {

    private static Logger log = LogManager.getLogger(PolicyManagerImplVersion2.class);

    private Repository policiesRepository;
    private DefaultConfigurationBuilder configBuilder;

    private static String USECASE_ELEMENT_NAME = "usecase";

    private static final String NEWLINE = System.getProperty("line.separator");

    private static final String POLICY_MAP_FILE = "/policy-map.xml";
    private Map<String, String> policy_map;
    private java.util.Date policyMapLastModified;

    /**
     * @param policiesRepository Repository containing access policies
     */
    public PolicyManagerImplVersion2(Repository policiesRepository) {
        this.policiesRepository = policiesRepository;
        configBuilder = new DefaultConfigurationBuilder();
        policy_map = new HashMap<String, String>();
        readPolicyMap(policiesRepository); // INFO: For peformance reasons we read the policy map at the startup of the policy manager, but which means changes at run-time won't be working.
    }
    
    /**
     * Get policies repository
     */
    public Repository getPoliciesRepository() {
        return policiesRepository;
    }
         
    /**
     * Read policy map.
     * @param repo Repository containing policy map
     */
    protected void readPolicyMap(Repository repo) {
        try {
            if(!repo.existsNode(POLICY_MAP_FILE)) {
                // INFO: No policy map file, abort.
                log.info("No policy map '" + POLICY_MAP_FILE + "' file in repo: " + repo.getName());
                return;
            }
            log.info("Found a policy map file in repo: " + repo.getName());
			
            Node pm_node = repo.getNode(POLICY_MAP_FILE);
            policyMapLastModified = new java.util.Date(pm_node.getLastModified());
            InputStream pm_istream = pm_node.getInputStream();
            DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(true);
            Configuration config = builder.build(pm_istream);
			
            //Configuration root = config.getChild("policy-map");
            Configuration[] mappings = config.getChildren("matcher");
            for(Configuration mapping : mappings) {
                // TODO: Allow custom matching classes?
                String pattern = mapping.getAttribute("pattern");
                String path = mapping.getAttribute("path");
                policy_map.put(pattern, path);
            }
        } catch (RepositoryException e) {
            log.fatal("Problem with policy repository: " + e.getMessage());
            log.fatal(e, e);
        } catch (ConfigurationException e) {
            log.fatal("Problem with policy map: " + e.getMessage());
            log.fatal(e, e);
        } catch (SAXException e) {
            log.fatal("Problem with policy map: " + e.getMessage());
            log.fatal(e, e);
        } catch (IOException e) {
            log.fatal("Input/output error on disk? Got: " + e.getMessage());
            log.fatal(e, e);
        }
    }
    
    /**
     * Match a path against the policy map.
     * @param path Path to be matched, e.g. '/projects/yanel/changes/master/c-c172df5f.html'
     * @param queryString Query string, e.g. 'update-result-of-test=5f119d82' ('/projects/yanel/changes/master/c-c172df5f.html?update-result-of-test=5f119d82')
     * @return Policy path if requested path is matching, otherwise return null
     */
    private String getMappedPath(String path, String queryString) {
        // INFO: Check last modified of policy map node and compare with policyMapLastModified
        try {
            Node pm_node = policiesRepository.getNode(POLICY_MAP_FILE);
            if (new java.util.Date(pm_node.getLastModified()).after(policyMapLastModified)) {
                log.warn("TODO: Reload policy map, because it has been modified '" + new java.util.Date(pm_node.getLastModified()) + "' since it has been loaded the last time '" + policyMapLastModified + "'!");
            } else {
                //log.debug("Policy map has not been modified since it has been loaded the last time '" + policyMapLastModified + "'.");
            }
        } catch(Exception e) {
            log.error(e, e);
        }

        if (queryString != null) {
            path = path + "?" + queryString;
        }
        for(Map.Entry<String, String> mapping : policy_map.entrySet()) {
            if(FilenameUtils.wildcardMatch(path, mapping.getKey())) {
                //log.debug("DEBUG: Path '" + path + "' matched inside policy map onto policy path '" + mapping.getValue() + "'.");
                return mapping.getValue();
            }
        }

        //log.debug("Path '" + path + "' did not match inside policy map.");

        return null;
    }
     
    /**
     * @deprecated Use authorize(String, Identity, Usecase) instead
     */
    public boolean authorize(Path path, Identity identity, Role role) throws AuthorizationException {
        return authorize(path.toString(), identity, role);
    }

    /**
     * @deprecated Use authorize(String, Identity, Usecase) instead
     */
    public boolean authorize(String path, Identity identity, Role role) throws AuthorizationException {
        log.warn("Deprecated method and not implemented! Use method authorize(String, Identity, Usecase) instead!");
        return false;
    }

    /**
     * @see org.wyona.security.core.api.PolicyManager#authorize(Policy, Identity, Usecase)
     */
    public boolean authorize(Policy policy, Identity identity, Usecase usecase) throws AuthorizationException {
        log.error("Not implemented yet!");
        return false;
    }

    /**
     * @see org.wyona.security.core.api.PolicyManager#authorize(String, Identity, Usecase)
     */
    public boolean authorize(String path, Identity identity, Usecase usecase) throws AuthorizationException {
        return authorize(path, null, identity, usecase);
    }

    /**
     * @see org.wyona.security.core.api.PolicyManager#authorize(String, String, Identity, Usecase)
     */
    public boolean authorize(String path, String queryString, Identity identity, Usecase usecase) throws AuthorizationException {
        if(path == null || identity == null || usecase == null) {
            log.error("Path or identity or usecase is null! [" + path + ", " + identity + ", " + usecase + "]");
            throw new AuthorizationException("Path or identity or usecase is null! [" + path + ", " + identity + ", " + usecase + "]");
        }

        try {
            return authorize(getPoliciesRepository(), path, queryString, identity, usecase);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw new AuthorizationException("Error authorizing " + getPoliciesRepository().getID() + ", " + path + ", " + identity + ", " + usecase, e);
        }
    }

    /**
     * @param repo Access control policy repository
     * @param path Requested path
     * @param queryString Query string associated with requested path
     */
    private boolean authorize(Repository repo, String path, String queryString, Identity identity, Usecase usecase) throws Exception {
        if(repo == null) {
            log.error("Repo is null!");
            throw new Exception("Repo is null!");
        } else if(path == null) {
            log.error("Path is null!");
            throw new Exception("Path is null!");
        } else if(identity == null) {
            log.error("Identity is null!");
            throw new Exception("Identity is null!");
        } else if(usecase == null) {
            log.error("Usecase is null!");
            throw new Exception("Usecase is null!");
        }

        //log.debug("Get policy path for requested path '" + path + "' and query string '" + queryString + "' ...");
        String yarepPath = getPolicyPath(path, queryString); 
        log.debug("Policy Yarep Path: " + yarepPath + ", Original Path: " + path + ", Repo: " + repo);
        if (repo.existsNode(yarepPath)) {
            try {
                Configuration config = configBuilder.build(repo.getNode(yarepPath).getInputStream());
                boolean useInheritedPolicies = config.getAttributeAsBoolean("use-inherited-policies", true);

            Configuration[] usecases = config.getChildren(USECASE_ELEMENT_NAME);
            for (int i = 0; i < usecases.length; i++) {
                String usecaseName = usecases[i].getAttribute("id", null);
                if (usecaseName != null && usecaseName.equals(usecase.getName())) {
                    boolean useInheritedRolePolicies = usecases[i].getAttributeAsBoolean("use-inherited-policies", true);
                    Configuration[] accreditableObjects = usecases[i].getChildren();

                    boolean worldCredentialExists = false;
                    boolean worldIsNotAuthorized = true;
                    for (int k = 0; k < accreditableObjects.length; k++) {
                        String aObjectName = accreditableObjects[k].getName();
                        log.debug("Accreditable Object Name: " + aObjectName);

                        if (aObjectName.equals("world")) {
                            worldCredentialExists = true;
                            String permission = accreditableObjects[k].getAttribute("permission", null);
                            if (permission.equals("true")) {
                                log.debug("Access granted: " + path);
                                worldIsNotAuthorized = false;
                                return true;
                            } else {
                                worldIsNotAuthorized = true;
                            }
                        } else if (aObjectName.equals("group")) {
                            if (identity.getGroupnames() != null) {
                                String groupName = accreditableObjects[k].getAttribute("id", null);
                                String[] groupnames = identity.getGroupnames();
                                if (groupnames != null) {
                                    for (int j = 0; j < groupnames.length; j++) {
                                        if (groupName.equals(groupnames[j])) {
                                            String permission = accreditableObjects[k].getAttribute("permission", null);
                                            if (permission.equals("true")) {
                                                log.debug("Access granted: Path = " + path + ", Group = " + groupName);
                                                return true;
                                            } else {
                                                log.debug("Access denied: Path = " + path + ", Group = " + groupName);
                                                return false;
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (aObjectName.equals("user")) {
                            if (identity.getUsername() != null) {
                                String userName = accreditableObjects[k].getAttribute("id", null);
                                if (userName.equals(identity.getUsername())) {
                                    String permission = accreditableObjects[k].getAttribute("permission", null);
                                    if (permission.equals("true")) {
                                        log.debug("Access granted: Path = " + path + ", User = " + userName);
                                        return true;
                                    } else {
                                        log.debug("Access denied: Path = " + path + ", User = " + userName);
                                        return false;
                                    }
                                }
                            }
                        } else if (aObjectName.equals("iprange")) {
                            log.warn("Credential IP Range not implemented yet!");
                            //return false;
                        } else {
                            log.warn("No such accreditable object implemented: " + aObjectName);
                            //return false;
                        }
                    }
                    if (worldCredentialExists && worldIsNotAuthorized) {
                       log.debug("Access for world denied: " + path);
                       return false;
                    }
                    if (!useInheritedRolePolicies){
                        log.debug("Policy inheritance disabled for usecase:" + usecaseName + ". Access denied: "+ path);
                        return false;
                    }
                }
            }
                if (!useInheritedPolicies) {
                    log.debug("Policy inheritance disabled. Access denied: "+ path);
                    return false;
                }
            } catch(NoSuchNodeException e) {
                log.error(e.getMessage(), e);
            }
        } else {
            if (yarepPath.equals("/.policy")) {
                log.warn("No such node: " + yarepPath + " (" + repo + ")");
            } else {
                if (log.isDebugEnabled()) log.debug("No such node: " + yarepPath + " (Fallback to parent policy ...)");
            }
        }

        String parent = PathUtil.getParent(path);
        if (parent != null) {
            // Check policy of parent in order to inherit credentials ...
            log.debug("Check parent policy: " + parent + " ... (Current path: " + path + ")");
            return authorize(repo, parent, null, identity, usecase);
        } else {
            log.debug("Trying to get parent of " + path + " (" + repo + ") failed, hence access denied.");
            return false;
        }
    }

    /**
     * Append '.policy' to path as suffix
     * @param path Path for which we are looking for a policy, e.g. "/en/projects/yanel/invite-user.html"
     * @param queryString Query string associated with requested path
     * @return policy path, e.g. "/en/projects/yanel/invite-user.html.policy"
     */
    private String getPolicyPath(String path, String queryString) {
        //log.debug("Get policy path for requested path '" + path + "' ...");
        // INFO: Remove trailing slash except for ROOT ...
        if (path.length() > 1 && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
 
        // TODO: Make order configurable, such that we can also check first whether individual policy exists (e.g. "/en/projects/yanel/invite-user.html.policy") and if not, then check policy map
        String mapped = getMappedPath(path, queryString);
        if(mapped != null) {
            log.debug("Mapped path: " + path + " -> " + mapped);
            return mapped;
        }
    	
        return path + ".policy";
    }

    /**
     * @see org.wyona.security.core.api.PolicyManager#getPolicy(String, boolean)
     */
    public Policy getPolicy(String path, boolean aggregate) throws AuthorizationException {
        //log.debug("Get policy for requested path '" + path + "' ...");
        try {
            if (aggregate) {
                return org.wyona.security.impl.util.PolicyAggregator.aggregatePolicy(path, this);
            } else {
                if (getPoliciesRepository().existsNode(getPolicyPath(path, null))) {
                    return new PolicyImplV2(getPoliciesRepository().getNode(getPolicyPath(path, null)).getInputStream());
                } else {
                    if (!path.equals("/")) {
                        log.warn("No policy found for '" + path + "' (Policies Repository: " + getPoliciesRepository().getName() + "). Check for parent '" + PathUtil.getParent(path) + "'.");
                        return null;
                        //return getPolicy(PathUtil.getParent(path), false);
                    } else {
                        log.warn("No policies found at all, not even a root policy!");
                        return null;
                    }
                }
            }
        } catch(Exception e) {
            log.error(e, e);
            throw new AuthorizationException(e.getMessage());
        }
    }

    /**
     * @see org.wyona.security.core.api.PolicyManager#setPolicy(String, Policy)
     */
    public void setPolicy(String path, Policy policy) throws java.lang.UnsupportedOperationException {
        try {
            Repository repo = getPoliciesRepository();
            String policyPath = getPolicyPath(path, null);
            log.debug("Set policy: " + policyPath);
            Node node;
            if (!repo.existsNode(policyPath)) {
                log.warn("Create new policy: " + policyPath);              
                node = YarepUtil.addNodes(repo, policyPath, org.wyona.yarep.core.NodeType.RESOURCE);
            } else {
                log.info("Policy '" + policyPath + "' already exists and hence creation request will be ignored!");
                node = repo.getNode(policyPath);
            }

            String parentPath = PathUtil.getParent(path);
            log.debug("Parent path: " + parentPath);
            if (parentPath == null) {
                log.warn("Seems like root policy is set (because parent path is null). Path: " + path);
            }
            StringBuilder sb = generatePolicyXML(policy, parentPath);
            org.apache.commons.io.IOUtils.copy(new java.io.StringBufferInputStream(sb.toString()), node.getOutputStream());
        } catch(Exception e) {
            log.error(e, e);
            new java.lang.UnsupportedOperationException(e.getMessage());
        }
    }

    /**
     * Generate policy XML from a policy object
     * @param policy Policy object
     * @param parentPath Parent path of this policy object
     */
    private StringBuilder generatePolicyXML(Policy policy, String parentPath) {
        // TODO: ...
        log.warn("TODO: Do not disentangle users and groups!");
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\"?>");

        sb.append(NEWLINE + NEWLINE);

        sb.append("<policy xmlns=\"http://www.wyona.org/security/1.0\"");
        boolean inheritPolicy = policy.useInheritedPolicies();
        if (!inheritPolicy) {
            sb.append(" use-inherited-policies=\"false\"");
        }
        sb.append(">");

            org.wyona.security.core.UsecasePolicy[] up = policy.getUsecasePolicies();
            for (int i = 0; i < up.length; i++) {
                org.wyona.security.core.ItemPolicy[] itps = up[i].getItemPolicies();
                if (itps != null && itps.length > 0) {
                    sb.append(NEWLINE);
                    sb.append("  <usecase id=\"" + up[i].getName() + "\">");

                    // Iterate over all users (including WORLD)
                    for (int k = 0; k < itps.length; k++) {
                        if (itps[k] instanceof IdentityPolicy) {
                            IdentityPolicy idp = (IdentityPolicy) itps[k];
                            Identity identity = idp.getIdentity();

                        // INFO: The policy editor can not differentiate yet whether a permissions has been set to false explicitely or has just not been set. Hence if inherit policy is true, then check on parent policies
                        // TODO: Resolve ambiguity within policy editor!
                        if (idp.getPermission() == false && inheritPolicy) { // TODO: Check inheritance flag of identity policy
                            if (identity.getGroupnames() != null) {
                                log.debug("Number of groups: " + identity.getGroupnames().length);
                            } else {
                                log.debug("User '" + identity.getUsername() + "' has no groups!");
                            }

                            try {
                                if (identity.isWorld()) {
                                    sb.append(NEWLINE);
                                    sb.append("    <world permission=\"" + this.authorize(parentPath, identity, new Usecase(up[i].getName())) + "\"/>");
                                } else {
                                    if (parentPath != null) {
                                        log.debug("Identity: " + identity + ", Usecase: " + up[i].getName() + ", Permission: " + this.authorize(parentPath, identity, new Usecase(up[i].getName())));
                                        sb.append(NEWLINE);
                                        sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + this.authorize(parentPath, identity, new Usecase(up[i].getName())) + "\"/>");

                                        // QUESTION: hm?
                                        //log.warn("DEBUG: Identity: " + identity + ", Usecase: " + up[i].getName() + ", Permission: " + this.authorize(policy.getParentPolicy(), identity, new Usecase(up[i].getName())));
                                        //sb.append(NEWLINE);
                                        //sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + this.authorize(policy.getParentPolicy(), identity, new Usecase(up[i].getName())) + "\"/>");
                                     } else {
                                         // TODO: Resolve ambiguity!
                                         log.warn("Seems like root policy is set (because parent path is null): " + identity.getUsername() + ", " + up[i].getName());
                                     }
                                 }
                             } catch(Exception e) {
                                 log.error(e, e);
                             }
                        } else {
                            if (identity.isWorld()) {
                                sb.append(NEWLINE);
                                sb.append("    <world permission=\"" + idp.getPermission() + "\"/>");
                            } else {
                                sb.append(NEWLINE);
                                sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + idp.getPermission() + "\"/>");
                            }
                        }
                        } else if (itps[k] instanceof GroupPolicy) {
                            GroupPolicy gp = (GroupPolicy) itps[k];
                            if (inheritPolicy && gp.getPermission() == false) { // TODO: Check inheritance flag of group policy
                                // TODO: Check group authorization
                                sb.append(NEWLINE);
                                sb.append("    <group id=\"" + gp.getId() + "\" permission=\"" + true + "\"/>");
                                //sb.append("    <group id=\"" + gp.getId() + "\" permission=\"" + this.authorize(policy.getParentPolicy(), TODO, new Usecase(up[i].getName())) + "\"/>");
                            } else {
                                sb.append(NEWLINE);
                                sb.append("    <group id=\"" + gp.getId() + "\" permission=\"" + gp.getPermission() + "\"/>");
                            }
                        } else {
                            log.warn("No such policy type implemented: ");
                        }
                    }

                    sb.append(NEWLINE);
                    sb.append("  </usecase>");
                } else {
                    log.warn("Usecase policy '" + up[i].getName() + "' has neither user, group nor any other policy!");
                }

/* DEPRECATED
                org.wyona.security.core.IdentityPolicy[] idps = up[i].getIdentityPolicies();
                org.wyona.security.core.GroupPolicy[] gps = up[i].getGroupPolicies();
                if ((idps != null && idps.length > 0) || (gps!= null && gps.length > 0)) {
                    sb.append(NEWLINE);
                    sb.append("  <usecase id=\"" + up[i].getName() + "\">");

                    // Iterate over all users (including WORLD)
                    for (int k = 0; k < idps.length; k++) {
                        Identity identity = idps[k].getIdentity();

                        // INFO: The policy editor can not differentiate yet whether a permissions has been set to false explicitely or has just not been set. Hence if inherit policy is true, then check on parent policies
                        // TODO: Resolve ambiguity within policy editor!
                        if (idps[k].getPermission() == false && inheritPolicy) { // TODO: Check inheritance flag of identity policy
                            if (identity.getGroupnames() != null) {
                                log.debug("Number of groups: " + identity.getGroupnames().length);
                            } else {
                                log.debug("User '" + identity.getUsername() + "' has no groups!");
                            }

                            try {
                                if (identity.isWorld()) {
                                    sb.append(NEWLINE);
                                    sb.append("    <world permission=\"" + this.authorize(parentPath, identity, new Usecase(up[i].getName())) + "\"/>");
                                } else {
                                    if (parentPath != null) {
                                        //log.warn("DEBUG: Identity: " + identity + ", Usecase: " + up[i].getName() + ", Permission: " + this.authorize(parentPath, identity, new Usecase(up[i].getName())));
                                        sb.append(NEWLINE);
                                        sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + this.authorize(parentPath, identity, new Usecase(up[i].getName())) + "\"/>");

                                        // QUESTION: hm?
                                        //log.warn("DEBUG: Identity: " + identity + ", Usecase: " + up[i].getName() + ", Permission: " + this.authorize(policy.getParentPolicy(), identity, new Usecase(up[i].getName())));
                                        //sb.append(NEWLINE);
                                        //sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + this.authorize(policy.getParentPolicy(), identity, new Usecase(up[i].getName())) + "\"/>");
                                     } else {
                                         // TODO: Resolve ambiguity!
                                         log.warn("Seems like root policy is set (because parent path is null): " + identity.getUsername() + ", " + up[i].getName());
                                     }
                                 }
                             } catch(Exception e) {
                                 log.error(e, e);
                             }
                        } else {
                            if (identity.isWorld()) {
                                sb.append(NEWLINE);
                                sb.append("    <world permission=\"" + idps[k].getPermission() + "\"/>");
                            } else {
                                sb.append(NEWLINE);
                                sb.append("    <user id=\"" + identity.getUsername() + "\" permission=\"" + idps[k].getPermission() + "\"/>");
                            }
                        }
                    }

                    // Iterate over all groups
                    for (int k = 0; k < gps.length; k++) {
                        if (inheritPolicy && gps[k].getPermission() == false) { // TODO: Check inheritance flag of group policy
                            // TODO: Check group authorization
                            sb.append(NEWLINE);
                            sb.append("    <group id=\"" + gps[k].getId() + "\" permission=\"" + true + "\"/>");
                            //sb.append("    <group id=\"" + gps[k].getId() + "\" permission=\"" + this.authorize(policy.getParentPolicy(), TODO, new Usecase(up[i].getName())) + "\"/>");
                        } else {
                            sb.append(NEWLINE);
                            sb.append("    <group id=\"" + gps[k].getId() + "\" permission=\"" + gps[k].getPermission() + "\"/>");
                        }
                    }
                    sb.append(NEWLINE);
                    sb.append("  </usecase>");
                }
*/
            }

            sb.append(NEWLINE);
            sb.append("</policy>");
            return sb;
    }

    /**
     * @see
     */
    public String[] getUsecases() {
        log.warn("TODO: Implementation not finished yet! Read from configuration instead hardcoded!");
        // TODO: What about configurable usecases such as for example workflow.approve, workflow.publish ...?
        //String[] usecases = {"view", "open", "write", "resource.create", "delete", "yanel.resource.meta", "introspection", "toolbar", "policy.read", "policy.update"};
        String[] usecases = {"view", "open", "write", "resource.create", "delete", "yanel.resource.meta", "introspection", "toolbar", "policy.read", "policy.update", "workflow.write", "workflow.approve", "workflow.publish"};
        return usecases;
    }

    /**
     * @see
     */
    public String getUsecaseLabel(String usecaseId, String language) {
        log.debug("TODO: Implementation not finished yet! Read from configuration instead hardcoded!");
        if (language.equals("de")) {
            if (usecaseId.equals("view")) {
                return "Inhalt lesen";
            } else if (usecaseId.equals("open")) {
                return "Inhalt zum Bearbeiten oeffnen";
            } else if (usecaseId.equals("write")) {
                return "(Bearbeiteter) Inhalt abspeichern";
            } else if (usecaseId.equals("resource.create")) {
                return "Inhalt neu kreieren";
            } else if (usecaseId.equals("delete")) {
                return "Inhalt loeschen";
            } else if (usecaseId.equals("yanel.resource.meta")) {
                return "Meta Informationen lesen";
            } else if (usecaseId.equals("introspection")) {
                return "Introspection anschauen/lesen";
            } else if (usecaseId.equals("toolbar")) {
                return "Yanel Toolbar verwenden";
            } else if (usecaseId.equals("policy.read")) {
                return "Zugriffsberechtigungen anschauen/lesen";
            } else if (usecaseId.equals("policy.update")) {
                return "Zugriffsberechtigungen bearbeiten";
            } else {
                return "No label for \"" + usecaseId + "\" (see " + this.getClass().getName() + ")";
            }
        } else {
            if (usecaseId.equals("view")) {
                return "View/Read";
            } else if (usecaseId.equals("open")) {
                return "Open content for editing";
            } else if (usecaseId.equals("write")) {
                return "Write/Save";
            } else if (usecaseId.equals("resource.create")) {
                return "Create a resource or a collection";
            } else if (usecaseId.equals("delete")) {
                return "Delete a resource or a collection";
            } else if (usecaseId.equals("yanel.resource.meta")) {
                return "View/Read meta information";
            } else if (usecaseId.equals("introspection")) {
                return "View introspection";
            } else if (usecaseId.equals("toolbar")) {
                return "Access Yanel toolbar";
            } else if (usecaseId.equals("policy.read")) {
                return "View access policy";
            } else if (usecaseId.equals("policy.update")) {
                return "Edit access policy";
            } else {
                return "No label for \"" + usecaseId + "\" (see " + this.getClass().getName() + ")";
            }
        }
    }
    
    public Policy createEmptyPolicy() throws AuthorizationException {
        try {
            return new PolicyImplV2();
        } catch (Exception e) {
            throw new AuthorizationException(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public void removePolicy(String path) throws AuthorizationException {
        Repository repo = getPoliciesRepository();
        String policyPath = getPolicyPath(path, null);
        try {
            if (repo.existsNode(policyPath)) {
                repo.getNode(policyPath).delete();
            }
        } catch (RepositoryException e) {
            throw new AuthorizationException("could not remove policy for path: " + path + 
                    ": " + e.getMessage(), e);
        }
    }
}
