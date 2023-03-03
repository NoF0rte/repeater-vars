package burp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import java.awt.Component;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.Range;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.contextmenu.InvocationType;
import burp.api.montoya.ui.contextmenu.MessageEditorHttpRequestResponse;

public class ContextMenuProvider implements ContextMenuItemsProvider {

	private final MontoyaApi api;

	public ContextMenuProvider(MontoyaApi api) {
		this.api = api;
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
			String selected = getSelectedText(editor);

			if (!selected.isBlank() && !selected.isEmpty()) {
				for (String var : Config.instance().variables().keySet()) {
					if (selected.trim().equals(var)) {
						JMenuItem replaceItem = new JMenuItem("Replace with value");
						replaceItem.addActionListener(e -> {
							replaceSelection(editor, Config.instance().variables().get(var));
						});

						items.add(replaceItem);
					}
				}
			}
		}

		return items;
	}

	private void replaceSelection(MessageEditorHttpRequestResponse editor, String value) {
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
