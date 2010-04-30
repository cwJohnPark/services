/**
 *  This document is a part of the source code and related artifacts
 *  for CollectionSpace, an open source collections management system
 *  for museums and related institutions:

 *  http://www.collectionspace.org
 *  http://wiki.collectionspace.org

 *  Copyright 2009 University of California at Berkeley

 *  Licensed under the Educational Community License (ECL), Version 2.0.
 *  You may not use this file except in compliance with this License.

 *  You may obtain a copy of the ECL 2.0 License at

 *  https://source.collectionspace.org/collection-space/LICENSE.txt

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.

 */
package org.collectionspace.services.authorization;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.collectionspace.services.authorization.spi.CSpaceAuthorizationProvider;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * AuthZ is the authorization service singleton used by the services runtime
 * @author 
 */
public class AuthZ {

    /**
     * volatile is used here to assume about ordering (post JDK 1.5)
     */
    private static volatile AuthZ self = new AuthZ();
    private CSpaceAuthorizationProvider provider;
    final Log log = LogFactory.getLog(AuthZ.class);

    private AuthZ() {
        setupProvider();
    }

    /**
     *
     * @return
     */
    public final static AuthZ get() {
        return self;
    }

    private void setupProvider() {
        String beanConfig = "applicationContext-authorization.xml";
        //system property is only set in test environment
        String beanConfigProp = System.getProperty("spring-beans-config");
        if (beanConfigProp != null && !beanConfigProp.isEmpty()) {
            beanConfig = beanConfigProp;
        }
        if (log.isDebugEnabled()) {
            log.debug("reading beanConfig=" + beanConfig);
        }
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext(
                new String[]{beanConfig});
        provider = (CSpaceAuthorizationProvider) appContext.getBean("cspaceAuthorizationProvider");
        if (log.isDebugEnabled()) {
            log.debug("initialized the authz provider");
        }
    }

    /**
     * addUriPermissions add permissions from given permission configuration
     * with assumption that resource is of type URI
     * @param permission configuration
     */
    //FIXME this method should be in the restful web service resource of authz
    public void addUriPermissions(Permission perm,
            PermissionRole permRole) throws PermissionException {
        List<String> principals = new ArrayList<String>();
        if (!perm.getCsid().equals(permRole.getPermissions().get(0).getPermissionId())) {
            throw new IllegalArgumentException("permission ids do not"
                    + " match for role=" + permRole.getRoles().get(0).getRoleName()
                    + " with permissionId=" + permRole.getPermissions().get(0).getPermissionId()
                    + " for permission with csid=" + perm.getCsid());
        }
        for (RoleValue roleValue : permRole.getRoles()) {
            principals.add(roleValue.getRoleName());
        }
        List<PermissionAction> permActions = perm.getActions();
        for (PermissionAction permAction : permActions) {
            URIResourceImpl uriRes = new URIResourceImpl(perm.getTenantId(),
                    perm.getResourceName(),
                    permAction.getName());
            addPermission(uriRes, principals.toArray(new String[0]));
        }
    }

    /**
     * addPermission for given principals to access given resource
     * -permission is retrieved from the resource
     * @param res
     * @param principals
     */
    public void addPermission(CSpaceResource res, String[] principals) throws PermissionException {
        CSpaceAction action = res.getAction();
        addPermission(res, principals, action);
    }

    /**
     * addPermission add given permission for given principals to access given resource
     * @param res
     * @param principals
     * @param perm
     */
    public void addPermission(CSpaceResource res, String[] principals, CSpaceAction action)
            throws PermissionException {
        provider.getPermissionManager().addPermission(res, principals, action);
        if (log.isDebugEnabled()) {
            log.debug("added permission resource=" + res.getId() + " action=" + action.name());
        }
    }

    /**
     * deletePermission for given principals for given resource
     * permission is retrieve from the resource
     * @param res
     * @param principals
     */
    public void deletePermission(CSpaceResource res, String[] principals)
            throws PermissionNotFoundException, PermissionException {
        CSpaceAction action = res.getAction();
        deletePermission(res, principals, action);
    }

    /**
     * deletePermission given permission for given principals for given resource
     * @param res
     * @param principals
     * @param perm
     */
    public void deletePermission(CSpaceResource res, String[] principals, CSpaceAction action)
            throws PermissionNotFoundException, PermissionException {
        provider.getPermissionManager().deletePermission(res, principals, action);
        if (log.isDebugEnabled()) {
            log.debug("removed permission resource=" + res.getId() + " action=" + action.name());
        }
    }

    /**
     * isAccessAllowed check if authenticated principal is allowed to access
     * given resource, permission is retrieved from the resource
     * @param res
     * @return
     */
    public boolean isAccessAllowed(CSpaceResource res) {
        CSpaceAction action = res.getAction();
        return provider.getPermissionEvaluator().hasPermission(res, action);
    }
}
