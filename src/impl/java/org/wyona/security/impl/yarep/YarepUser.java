package org.wyona.security.impl.yarep;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.wyona.security.core.ExpiredIdentityException;
import org.wyona.security.core.UserHistory;
import org.wyona.security.core.api.AccessManagementException;
import org.wyona.security.core.api.Group;
import org.wyona.security.core.api.IdentityManager;
import org.wyona.security.core.api.User;
import org.wyona.security.impl.Password;
import org.wyona.yarep.core.Node;

/**
 *
 */
public class YarepUser extends YarepItem implements User {
    public static final String USER = "user";

    public static final String EMAIL = "email";

    public static final String LANGUAGE = "language";

    public static final String PASSWORD = "password";

    public static final String SALT = "salt";
        
    public static final String EXPIRE = "expire";
    
    public static final String DESCRIPTION = "description";

    /**
     * Date format used for the expired value
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    private String email;

    private String language;

    private String encryptedPassword;

    private String salt;
    
    private String description;
    
    private Date expire;
    
    protected YarepUser() {
        super();
    }
    
    /**
     * Instantiates an existing YarepUser from a repository node.
     *
     * @param node
     */
    public YarepUser(IdentityManager identityManager, Node node) throws AccessManagementException {
        super(identityManager, node); // this will call configure()
    }

    /**
     * Creates a new YarepUser with the given id as a child of the given parent
     * node. The user is not saved.
     *
     * @param parentNode
     * @param id
     * @param plainTextPassword - password as a plain text, it will be encrypted locally
     * @throws AccessManagementException
     */
    public YarepUser(IdentityManager identityManager, Node parentNode, String id, String name,
            String email, String plainTextPassword) throws AccessManagementException {
        super(identityManager, parentNode, id, name, id + ".xml");
        setEmail(email);
        setPassword(plainTextPassword);
    }
    
    /**
     * Creates a new YarepUser with the given id as a child of the given parent
     * node. The user is not saved.
     * 
     * @throws AccessManagementException
     */
    public YarepUser(IdentityManager identityManager, Node parentNode, String id, String name, String nodeName) throws AccessManagementException{
        super(identityManager, parentNode, id, name, nodeName);
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    protected void configure(Configuration config) throws ConfigurationException, AccessManagementException {
        // Compulsory fields
        setID(config.getAttribute(ID));
        setName(config.getChild(NAME, false).getValue(null));
        
        // Optional fields
        if(config.getChild(EMAIL, false) != null){
            setEmail(config.getChild(EMAIL, false).getValue(null));
        }
        
        if(config.getChild(PASSWORD, false) != null){
            // Do not use setter here because it does other things
            this.encryptedPassword = config.getChild(PASSWORD, false).getValue(null);
        }
        
        if(config.getChild(LANGUAGE, false) != null) {
            setLanguage(config.getChild(LANGUAGE, false).getValue(null));
        }
        
        if(config.getChild(SALT,false) != null) {
            this.salt = config.getChild(SALT, false).getValue(null);
        }
        
        if(config.getChild(DESCRIPTION, false) != null) {
            setDescription(config.getChild(DESCRIPTION, false).getValue(null));
        }
        
        if(config.getChild(EXPIRE, false) != null){
            SimpleDateFormat sdf1 = new SimpleDateFormat(DATE_TIME_FORMAT);
            SimpleDateFormat sdf2 = new SimpleDateFormat(DATE_FORMAT);
                String dateAsString = config.getChild(EXPIRE, false).getValue(null);
                Date expire = null;
                
                if(null != dateAsString){
                    try {
                        expire = sdf2.parse(dateAsString);
                    } catch (ParseException e) {
                        try {
                            expire = sdf1.parse(dateAsString);
                        } catch (ParseException e1) {
                            log.error(e.getMessage() + " (The user will be made expired)");
                        }finally{
                            if(expire == null){
                                GregorianCalendar cal = new GregorianCalendar();
                                cal.add(Calendar.YEAR, -10);
                                expire = cal.getTime();
                            }else{
                                // parsed correctly
                            }
                        }
                    }finally{
                        this.setExpirationDate(expire);
                    }
                }
        }
    }

    /**
     * @see org.wyona.security.impl.yarep.YarepItem#createConfiguration()
     */
    protected Configuration createConfiguration() throws AccessManagementException {
        DefaultConfiguration config = new DefaultConfiguration(USER);
        config.setAttribute(ID, getID());
        
        DefaultConfiguration nameNode = new DefaultConfiguration(NAME);
        nameNode.setValue(getName());
        config.addChild(nameNode);
        
        if(getEmail() != null){
            DefaultConfiguration emailNode = new DefaultConfiguration(EMAIL);
            emailNode.setValue(getEmail());
            config.addChild(emailNode);
        }
        
        if(getLanguage() != null){
            DefaultConfiguration languageNode = new DefaultConfiguration(LANGUAGE);
            languageNode.setValue(getLanguage());
            config.addChild(languageNode);
        }
        
        if(getPassword() != null){
            DefaultConfiguration passwordNode = new DefaultConfiguration(PASSWORD);
            passwordNode.setValue(getPassword());
            config.addChild(passwordNode);
        }
        
        if(getDescription() != null){
            DefaultConfiguration descriptionNode = new DefaultConfiguration(DESCRIPTION);
            descriptionNode.setValue(getDescription());
            config.addChild(descriptionNode);
        }
        
        if(getExpirationDate() != null){
            DefaultConfiguration expireNode = new DefaultConfiguration(EXPIRE);
            expireNode.setValue(new SimpleDateFormat(DATE_TIME_FORMAT).format(getExpirationDate()));
            config.addChild(expireNode);
        }
        
        if(getSalt() != null) {
            DefaultConfiguration saltNode = new DefaultConfiguration(SALT);
            saltNode.setValue(getSalt());
            config.addChild(saltNode);
        }

        return config;
    }

    /**
     * @see org.wyona.security.core.api.User#authenticate(java.lang.String)
     */
    public boolean authenticate(String plainTextPassword) throws ExpiredIdentityException, AccessManagementException {
        if(isExpired()){
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
            throw new ExpiredIdentityException("Identity expired on "+sdf.format(getExpirationDate()));
        }
        
        if(getSalt() == null) {
            return getPassword().equals(Password.getMD5(plainTextPassword));
        } else {
            return getPassword().equals(Password.getMD5(plainTextPassword, getSalt()));
        }
    }
    
    protected boolean isExpired(){
        boolean expired = false;
        if(getExpirationDate() != null){
            expired = getExpirationDate().before(new Date()); 
        }else{
            // Never expires
        }
        
        return expired;
    }
    
    public String getDescription() {
        return description;
    }

    /**
     * @see org.wyona.security.core.api.User#getEmail()
     */
    public String getEmail() throws AccessManagementException {
        return this.email;
    }

    /**
     * @see org.wyona.security.core.api.User#getLanguage()
     */
    public String getLanguage() throws AccessManagementException {
        return this.language;
    }

    /**
     * @see org.wyona.security.core.api.User#getSalt()
     */
    public String getSalt() throws AccessManagementException {
        return this.salt;
    }

    /**
     * @see org.wyona.security.core.api.User#getGroups()
     */
    public Group[] getGroups() throws AccessManagementException {
        Group[] allGroups = getIdentityManager().getGroupManager().getGroups();
        ArrayList groups = new ArrayList();
        for (int i = 0; i < allGroups.length; i++) {
            if (allGroups[i].isMember(this)) {
                groups.add(allGroups[i]);
            }
        }
        return (Group[]) groups.toArray(new Group[groups.size()]);
    }

    /**
     * @see org.wyona.security.core.api.User#getGroups(boolean)
     */
    public Group[] getGroups(boolean parents) throws AccessManagementException {
        if (parents) {
            log.error("TODO: Implementation not finished yet!");
            return null;
        } else {
            return getGroups();
        }
    }

    /**
     * @see org.wyona.security.core.api.User#setEmail(java.lang.String)
     */
    public void setEmail(String email) throws AccessManagementException {
        this.email = email;
    }

    /**
     * @see org.wyona.security.core.api.User#setLanguage(java.lang.String)
     */
    public void setLanguage(String language) throws AccessManagementException {
        this.language = language;
    }

    /**
     * @see org.wyona.security.core.api.User#setPassword(java.lang.String)
     */
    public void setPassword(String plainTextPassword) throws AccessManagementException {
        setSalt();
        this.encryptedPassword = Password.getMD5(plainTextPassword, this.salt);
    }

    /**
     * @see org.wyona.security.core.api.User#setSalt()
     */
    public void setSalt() throws AccessManagementException {
        this.salt = Password.getSalt();

    }
    
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the password hash
     * @return encrypted password
     * @throws AccessManagementException
     */
    protected String getPassword() throws AccessManagementException {
        return this.encryptedPassword;
    }

    /**
     *
     */
    public Date getExpirationDate() {
        return expire;
    }

    /**
     *
     */
    public void setExpirationDate(Date date) {
        this.expire = date;
    }

    /**
     *
     */
    public UserHistory getHistory() {
        log.error("TODO: Not implemented yet!");
        return null;
    }

    /**
     *
     */
    public void setHistory(UserHistory history) {
        log.error("TODO: Not implemented yet!");
    }
}
