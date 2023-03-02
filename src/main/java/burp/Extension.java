package burp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.HttpHeader;
import burp.api.montoya.http.message.params.*;
import burp.api.montoya.http.message.requests.HttpRequest;

public class Extension implements BurpExtension, HttpHandler {

	private MontoyaApi api;

	@Override
	public void initialize(MontoyaApi api) {
		this.api = api;

		api.extension().setName("Repeater Vars");

		Config.setInstance(new Config(api));
		UserInterface.create(api);

		api.http().registerHttpHandler(this);
	}

	@Override
	public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
		if (!Config.instance().enabled() || !requestToBeSent.toolSource().isFromTool(ToolType.REPEATER)) {
			return RequestToBeSentAction.continueWith(requestToBeSent);
		}

		HttpRequest updatedRequest = requestToBeSent;
		String body = updatedRequest.bodyToString();
		for (Entry<String, String> entry : Config.instance().variables().entrySet()) {
			String varName = entry.getKey();
			String varValue = entry.getValue();

			if (body.contains(varName)) {
				body = body.replace(varName, varValue);
			}

			for (HttpHeader header : updatedRequest.headers()) {
				if (header.value().contains(varName)) {
					updatedRequest = updatedRequest.withUpdatedHeader(header.name(), header.value().replace(varName, varValue));
				}
			}

			List<HttpParameter> updatedParams = new ArrayList<>();
			for (ParsedHttpParameter param : updatedRequest.parameters()) {
				if (param.value().contains(varName) && param.type() == HttpParameterType.URL) {
					updatedParams.add(HttpParameter.urlParameter(param.name(), param.value().replace(varName, varValue)));
				}
			}

			if (updatedParams.size() > 0) {
				updatedRequest = updatedRequest.withUpdatedParameters(updatedParams);
			}
		}

		updatedRequest = updatedRequest.withBody(body);
		return RequestToBeSentAction.continueWith(updatedRequest);
	}

	@Override
	public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
		return ResponseReceivedAction.continueWith(responseReceived);
	}
}
