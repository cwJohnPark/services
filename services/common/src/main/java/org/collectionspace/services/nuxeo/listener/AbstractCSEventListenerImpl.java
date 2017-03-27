package org.collectionspace.services.nuxeo.listener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.collectionspace.services.common.api.Tools;
import org.collectionspace.services.config.tenant.EventListenerConfig;
import org.collectionspace.services.config.tenant.Param;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.collections.ScopedMap;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;

public abstract class AbstractCSEventListenerImpl implements CSEventListener {
	private static List<String> repositoryNameList = new ArrayList<String>();
	private static Map<String, Map<String, Map<String, String>>> eventListenerParamsMap = new HashMap<String, Map<String, Map<String, String>>>();  // <repositoryName, Map<EventListenerId, Map<key, value>>>
	private static Map<String, String> nameMap = new HashMap<String, String>();
	
	static final String DOCMODEL_CONTEXT_PROPERTY_PREFIX = ScopeType.DEFAULT.getScopePrefix();
	
	public AbstractCSEventListenerImpl() {
		// Intentionally left blank
	}
	
	protected List<String> getRepositoryNameList() {
		return repositoryNameList;
	}
	
	protected Map<String, Map<String, Map<String, String>>> getEventListenerParamsMap() {
		return eventListenerParamsMap;
	}

	@Override
	public boolean isRegistered(Event event) {
		boolean result = false;
		
		if (event != null && event.getContext() != null) {
			result = repositoryNameList.contains(event.getContext().getRepositoryName());
		}
		
		return result;
	}


	/**
	 * Returns 'true' if this collection changed as a result of the call. 
	 */
	@Override
	public boolean register(String respositoryName, EventListenerConfig eventListenerConfig) {
		boolean result = false;
		
		// Using the repositoryName as a qualifier, register this event listener's name as specified in the tenant bindings.
		setName(respositoryName, eventListenerConfig.getId());
		
		// Register this event listener with the given repository name
		if (getRepositoryNameList().add(respositoryName)) {
			result = true;
		}
		
		// Set this event listeners parameters, if any.  Params are qualified with the repositoryName since multiple tenants might be registering the same event listener but with different params.
		List<Param> paramList = eventListenerConfig.getParamList().getParam(); // values from the tenant bindings that we need to copy into the event listener
		if (paramList != null) {
			//
			// Get the list of event listeners for a given repository
			Map<String, Map<String, String>> eventListenerRepoParams = getEventListenerParamsMap().get(respositoryName); // Get the set of event listers for a given repository
			if (eventListenerRepoParams == null) {
				eventListenerRepoParams = new HashMap<String, Map<String, String>>();
				getEventListenerParamsMap().put(respositoryName, eventListenerRepoParams); // create and put an empty map
				result = true;
			}
			//
			// Get the list of params for a given event listener for a given repository
			Map<String, String> eventListenerParams = eventListenerRepoParams.get(eventListenerConfig.getId()); // Get the set of params for a given event listener for a given repository
			if (eventListenerParams == null) {
				eventListenerParams = new HashMap<String, String>();
				eventListenerRepoParams.put(eventListenerConfig.getId(), eventListenerParams); // create and put an empty map
				result = true;
			}
			//
			// copy all the values from the tenant bindings into the event listener
			for (Param params : paramList) {
				String key = params.getKey();
				String value = params.getValue();
				if (Tools.notBlank(key)) {
					eventListenerParams.put(key, value);
					result = true;
				}
			}
		}
		
		return result;
	}
	
	protected void setName(String repositoryName, String eventListenerName) {
		nameMap.put(repositoryName, eventListenerName);
	}

	@Override
	public Map<String, String> getParams(Event event) {
		String repositoryName = event.getContext().getRepositoryName();
		return getEventListenerParamsMap().get(repositoryName).get(getName(repositoryName));  // We need to qualify with the repositoryName since this event listener might be register by multiple tenants using different params
	}
	
	@Override
	public String getName(String repositoryName) {
		return nameMap.get(repositoryName);
	}
	
	//
	// Set a property in the document model's transient context.
	//
	@Override
    public void setDocModelContextProperty(DocumentModel collectionObjectDocModel, String key, Serializable value) {
    	ScopedMap contextData = collectionObjectDocModel.getContextData();
    	contextData.putIfAbsent(DOCMODEL_CONTEXT_PROPERTY_PREFIX + key, value);        
    }
	
    //
    // Clear a property from the docModel's context
	//
	@Override
	public void clearDocModelContextProperty(DocumentModel docModel, String key) {
    	ScopedMap contextData = docModel.getContextData();
    	contextData.remove(DOCMODEL_CONTEXT_PROPERTY_PREFIX + key);	
	}

}
