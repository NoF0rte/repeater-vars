package burp;

import java.util.Optional;
import java.util.Map.Entry;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.HttpService;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
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

	public static HttpRequest replaceVars(HttpRequest request) {
		return Extension.replaceVars(request.toString(), request.httpService());
	}

	public static HttpRequest replaceVars(String input, HttpService service) {
		for (Entry<String, String> entry : Config.instance().variables().entrySet()) {
			String varName = entry.getKey();
			String varValue = entry.getValue();

			if (input.contains(varName)) {
				input = input.replace(varName, varValue);
			}
		}

		HttpRequest request = HttpRequest.httpRequest(service, input);
		Optional<HttpHeader> contentLength = request.headers().stream().filter(h -> h.name().equalsIgnoreCase("Content-Length")).findFirst();
		if (contentLength.isPresent()) {
			int bodyLength = request.bodyToString().length();
			request = request.withUpdatedHeader(contentLength.get().name(), Integer.toString(bodyLength));
		}

		return request;
	}

	@Override
	public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
		if (!Config.instance().enabled() || !requestToBeSent.toolSource().isFromTool(ToolType.REPEATER)) {
			return RequestToBeSentAction.continueWith(requestToBeSent);
		}

		HttpRequest request = Extension.replaceVars(requestToBeSent.toString(), requestToBeSent.httpService());
		return RequestToBeSentAction.continueWith(request);
	}

	@Override
	public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
		return ResponseReceivedAction.continueWith(responseReceived);
	}
}
