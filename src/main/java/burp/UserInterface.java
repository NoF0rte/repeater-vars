package burp;

import burp.api.montoya.MontoyaApi;
import javax.swing.*;
import javax.swing.table.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;

public class UserInterface {
	public static final int textHeight = new JTextField().getPreferredSize().height;
	
	private static DefaultTableModel varsTableModel;
	private static JTable varsTable;
	private static JTextField nameTextField;
	private static JTextField valueTextField;

	public static void create(MontoyaApi api) {
		JPanel gridPanel = new JPanel();
		gridPanel.setLayout(new GridBagLayout());
		gridPanel.setPreferredSize(new Dimension(800, 500));
		gridPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		gridPanel.setBorder(BorderFactory.createTitledBorder("Settings"));

		Dimension btnDimension = new Dimension(100, 25);
		Dimension textFieldDimension = new Dimension(250, 25);
		GridBagConstraints c = new GridBagConstraints();

		// Row 1

		JToggleButton enabledBtn = new JToggleButton();
		if (Config.instance().enabled()) {
			enabledBtn.setSelected(true);
			enabledBtn.setText("Enabled");
		} else {
			enabledBtn.setText("Disabled");
		}

		enabledBtn.addChangeListener(e -> {
			if (enabledBtn.isSelected()) {
				enabledBtn.setText("Enabled");
				Config.instance().setEnabled(true);
			} else {
				enabledBtn.setText("Disabled");
				Config.instance().setEnabled(false);
			}
		});
		enabledBtn.setPreferredSize(btnDimension);

		c.gridx = 0;
		c.gridy = 0;
		c.insets.bottom = 10;
		gridPanel.add(enabledBtn, c);

		// Row 2

		JLabel nameLabel = new JLabel("Name: ");

		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.insets.left = 0;
		gridPanel.add(nameLabel, c);

		nameTextField = new JTextField("{EXAMPLE_NAME}");
		nameTextField.setPreferredSize(textFieldDimension);

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		gridPanel.add(nameTextField, c);

		// Row 3

		JLabel valueLabel = new JLabel("Value: ");

		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridx = 0;
		c.gridy = 2;
		c.insets.left = 0;
		gridPanel.add(valueLabel, c);

		valueTextField = new JTextField("example value");
		valueTextField.setPreferredSize(textFieldDimension);

		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 1;
		gridPanel.add(valueTextField, c);

		JButton addBtn = new JButton("Add");
		addBtn.setPreferredSize(btnDimension);
		addBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String name = nameTextField.getText();
				if (name == "") {
					return;
				}

				String value = valueTextField.getText();
				if (value == "") {
					return;
				}

				Object[] row = {name, value};
				varsTableModel.addRow(row);

				nameTextField.setText("");
				valueTextField.setText("");
			}
		});

		c.gridx = 2;
		c.anchor = GridBagConstraints.EAST;
		gridPanel.add(addBtn, c);

		// Row 4

		varsTableModel = new DefaultTableModel(){
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		}; 
		varsTableModel.addColumn("Name");
		varsTableModel.addColumn("Value");

		if (Config.instance().variables().size() > 0) {
			Config.instance().variables().forEach((name, value) -> varsTableModel.addRow(new Object[]{name, value}));
		}

		varsTable = new JTable(varsTableModel);

		JScrollPane scrollPane = new JScrollPane(varsTable);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(600, 200));

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		gridPanel.add(scrollPane, c);

		JButton modifyBtn = new JButton("Modify");
		modifyBtn.setPreferredSize(btnDimension);
		modifyBtn.setMinimumSize(btnDimension);
		modifyBtn.setMaximumSize(btnDimension);
		modifyBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedRow = varsTable.getSelectedRow();
				if (selectedRow == -1) {
					return;
				}

				nameTextField.setText((String)varsTableModel.getValueAt(selectedRow, 0));
				valueTextField.setText((String)varsTableModel.getValueAt(selectedRow, 1));

				varsTableModel.removeRow(selectedRow);
			}
		});

		JButton deleteBtn = new JButton("Delete");
		deleteBtn.setPreferredSize(btnDimension);
		deleteBtn.setMinimumSize(btnDimension);
		deleteBtn.setMaximumSize(btnDimension);
		deleteBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedRow = varsTable.getSelectedRow();
				if (selectedRow == -1) {
					return;
				}

				varsTableModel.removeRow(selectedRow);
			}
		});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 0));
		buttonPane.add(Box.createVerticalGlue());
		buttonPane.add(modifyBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(0, 10)));
		buttonPane.add(deleteBtn);

		c.gridx = 3;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		gridPanel.add(buttonPane, c);

		// Row 5

		JButton saveBtn = new JButton("Save");
		saveBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Hashtable<String, String> variables = new Hashtable<>();

				int numRows = varsTableModel.getRowCount();
				for (int i = 0; i < numRows; i++) {
					variables.put((String)varsTableModel.getValueAt(i, 0), (String)varsTableModel.getValueAt(i, 1));
				}

				Config.instance().setVariables(variables);
			}
		});
		saveBtn.setPreferredSize(btnDimension);

		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.EAST;
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 4;
		gridPanel.add(saveBtn, c);

		JPanel mainPanel = new JPanel();
		mainPanel.add(gridPanel);

		api.userInterface().registerSuiteTab("Repeater Vars", mainPanel);
	}
}
