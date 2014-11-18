package org.wyona.security.core.api;

import org.wyona.commons.io.Path;
import org.wyona.security.core.AuthorizationException;
import org.wyona.yarep.core.Repository;

/**
 * Policy manager interface in order to get, set policies and also to check authorization
 */
public interface PolicyManager {

    /**
     * @deprecated Use {@link authorize(String , Identity , Usecase)} instead.
     */
    public boolean authorize(Path path, Identity identity, Role role) throws AuthorizationException;
    
    /**
     * @deprecated Use {@link authorize(String , Identity , Usecase)} instead.
     */
    public boolean authorize(String path, Identity identity, Role role) throws AuthorizationException;

    /**
     * Check whether a particular identity is authorized to execute a specific usecase for a specific path/URL and query string
     * @param path Requested path
     * @param queryString Query string attached to original request
     * @param identity User requesting path
     * @param usecase Usecase associated with requested path
     * @return true when authorized and false otherwise
     */
    public boolean authorize(String path, String queryString, Identity identity, Usecase usecase) throws AuthorizationException;

    /**
     * Check whether a particular identity is authorized to execute a specific usecase for a specific path/URL
     * @param identity User
     */
    public boolean authorize(String path, Identity identity, Usecase usecase) throws AuthorizationException;
    
    /**
     * @param policy TODO
     */
    public boolean authorize(Policy policy, Identity identity, Usecase usecase) throws AuthorizationException;
   
    /**
     * Get policies repository
     * @deprecated It's not good to reveal the actual data repository
     */
     public Repository getPoliciesRepository();

    /**
     * Get policy of a specific node
     * @param path Path of content, e.g. /hello/world.html
     * @param aggregate Boolean which specifies if implementation shall return an aggregated policy, e.g. an aggregation of the policies for /, /hello/ and /hello/world.html
     * @return Policy which is associated with content path and if no policy exists, then return null
     */
    public Policy getPolicy(String path, boolean aggregate) throws AuthorizationException;

    /**
     * TBD/TODO (WARNING: Backwards compatibility)
     */
    //public boolean existsPolicy(String path) throws AuthorizationException;

    /**
     * Set new or modified policy
     * @param path Path of content, e.g. /hello/world.html
     * @param policy New or modified policy
     */
    public void setPolicy(String path, Policy policy) throws AuthorizationException;
    
    /**
     * Removes the policy from the given path.
     * @param path
     * @throws AuthorizationException
     */
    public void removePolicy(String path) throws AuthorizationException;
    
    /**
     * Creates an empty policy.
     * @return empty policy
     */
    public Policy createEmptyPolicy() throws AuthorizationException;

    /**
     * @return All the usecases which the policy manager supports. For example this can be useful for a policy editor in order to select from a list of usecases/actions/rights.
     */
    public String[] getUsecases() throws AuthorizationException;

    /**
     * @return Get usecase label, for example return "Read" as the label for the usecaseId "r"
     */
    public String getUsecaseLabel(String usecaseId, String language) throws AuthorizationException;
}
