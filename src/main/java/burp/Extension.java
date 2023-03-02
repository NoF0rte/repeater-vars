package burp;

import java.awt.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Range;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.*;

public class Extension implements BurpExtension, HttpHandler, ContextMenuItemsProvider {

	private MontoyaApi api;

	@Override
	public void initialize(MontoyaApi api) {
		this.api = api;

		api.extension().setName("Repeater Vars");

		Config.setInstance(new Config(api));
		UserInterface.create(api);

		api.http().registerHttpHandler(this);
		api.userInterface().registerContextMenuItemsProvider(this);
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

	@Override
	public List<Component> provideMenuItems(ContextMenuEvent event) {
		if (!event.isFrom(InvocationType.INTRUDER_PAYLOAD_POSITIONS, InvocationType.MESSAGE_EDITOR_REQUEST) && !event.isFromTool(ToolType.INTRUDER, ToolType.REPEATER)) {
			return null;
		}

		if (!Config.instance().enabled() || Config.instance().variables().size() == 0) {
			return null;
		}

		List<Component> items = new ArrayList<>();
		JMenu menu = new JMenu("Insert");

		MessageEditorHttpRequestResponse editor = event.messageEditorRequestResponse().get();
		for (String var : Config.instance().variables().keySet()) {
			JMenuItem item = new JMenuItem(var);
			item.addActionListener(e -> {
				replaceSelection(editor, var);
			});

			menu.add(item);
		}
		items.add(menu);

		if (event.messageEditorRequestResponse().get().selectionOffsets().isPresent()) {
			JMenuItem replaceItem = new JMenuItem("Replace with value");
			replaceItem.addActionListener(e -> {
				String selected = getSelectedText(editor);
				if (selected.isBlank() || selected.isEmpty()) {
					return;
				}

				for (String var : Config.instance().variables().keySet()) {
					if (selected.equals(var)) {
						replaceSelection(editor, Config.instance().variables().get(var));
						return;
					}
				}
			});

			items.add(replaceItem);
		}

		return items;
	}

	private void replaceSelection(MessageEditorHttpRequestResponse editor, String value) {
		api.logging().logToOutput(String.format("Replacing selection with %s", value));
		int[] bounds = new int[]{editor.caretPosition(), editor.caretPosition()};

		if (editor.selectionOffsets().isPresent()) {
			bounds[0] = editor.selectionOffsets().get().startIndexInclusive();
			bounds[1] = editor.selectionOffsets().get().endIndexExclusive();
		}

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		byte[] message = editor.requestResponse().request().toByteArray().getBytes();

		try {
			outputStream.write(Arrays.copyOfRange(message, 0, bounds[0]));
			outputStream.write(api.utilities().byteUtils().convertFromString(value));
			outputStream.write(Arrays.copyOfRange(message, bounds[1],message.length));
			outputStream.flush();

			editor.setRequest(HttpRequest.httpRequest(editor.requestResponse().httpService(), outputStream.toString()));
		} catch (IOException e1) {
			api.logging().logToError(e1.toString());
		}
	}

	private String getSelectedText(MessageEditorHttpRequestResponse editor) {
		if (editor.selectionOffsets().isEmpty()) {
			return "";
		}

		Range range = editor.selectionOffsets().get();
		HttpRequest request = editor.requestResponse().request();

		return request.toByteArray().subArray(range.startIndexInclusive(), range.endIndexExclusive()).toString();
	}
}
