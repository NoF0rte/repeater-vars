package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.persistence.PersistedObject;

import java.util.Hashtable;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Config {

	public static final String VARIABLES_KEY = "varDictionary";
	public static final String ENABLED_KEY = "enabled";

	private static Hashtable<String, String> variablesCache = null;

	private static Config instance;

	public static Config instance() {
		return instance;
	}

	public static void setInstance(Config config) {
		instance = config;
	}

	private final MontoyaApi api;
	private final PersistedObject extensionData;

	public Config(MontoyaApi api) {
		this.api = api;

		this.extensionData = api.persistence().extensionData();
		setDefaults();
	}

	public void setDefaults() {
		if (enabled() == null) {
			setEnabled(true);
		}

		if (variables() == null) {
			setVariables(new Hashtable<String, String>());
		}
	}

	public Hashtable<String, String> variables() {
		if (variablesCache == null) {
			String json = extensionData.getString(VARIABLES_KEY);
			if (json == null) {
				return null;
			}

			try {
				Type dictType = TypeToken.getParameterized(Hashtable.class, String.class, String.class).getType();
				variablesCache = new Gson().fromJson(json, dictType);
			} catch (Exception e) {
				api.logging().logToError(String.format("Error deserializing fuzzList from config", e.getMessage()));
				return null;
			}
		}

		return variablesCache;
	}

	public void setVariables(Hashtable<String, String> variables) {
		variablesCache = variables;

		String json = new Gson().toJson(variablesCache);
		extensionData.setString(VARIABLES_KEY, json);
	}

	public synchronized Boolean enabled() {
		return extensionData.getBoolean(ENABLED_KEY);
	}

	public synchronized void setEnabled(boolean enabled) {
		extensionData.setBoolean(ENABLED_KEY, enabled);
	}
}
