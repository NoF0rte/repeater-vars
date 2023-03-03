package burp;

import java.util.Map.Entry;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;

public class Extension implements BurpExtension, HttpHandler {

	private MontoyaApi api;

	@Override
	public void initialize(MontoyaApi api) {
		this.api = api;

		api.extension().setName("Repeater Vars");

		Config.setInstance(new Config(api));
		
		api.userInterface().registerSuiteTab("Repeater Vars", new SuiteTab(api));

		api.http().registerHttpHandler(this);
		api.userInterface().registerContextMenuItemsProvider(new ContextMenuProvider(api));
	}

	@Override
	public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
		if (!Config.instance().enabled() || !requestToBeSent.toolSource().isFromTool(ToolType.REPEATER)) {
			return RequestToBeSentAction.continueWith(requestToBeSent);
		}

		String request = requestToBeSent.toString();

		for (Entry<String, String> entry : Config.instance().variables().entrySet()) {
			String varName = entry.getKey();
			String varValue = entry.getValue();

			if (request.contains(varName)) {
				request = request.replace(varName, varValue);
			}
		}

		return RequestToBeSentAction.continueWith(HttpRequest.httpRequest(requestToBeSent.httpService(), request));
	}

	@Override
	public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
		return ResponseReceivedAction.continueWith(responseReceived);
	}
}
