package gamemap.icon;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import static gamemap.icon.Utils.*;

public class IconTab extends JPanel {
	private Map<String, JComponent>	fieldToComp	= new HashMap<>();
	private Map<JComponent, String>	compToField	= new HashMap<>();
	private JLabel					filename	= new JLabel();
	private JLabel					baseSize	= new JLabel();

	private Icon					currentIcon;
	private CropPane				cropPreview;

	IconTab() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		cropPreview = new CropPane();
		add(cropPreview);

		add(filename);
		filename.setToolTipText("Filename");
		filename.setBorder(new EmptyBorder(2, 2, 2, 2));
		filename.setHorizontalTextPosition(JLabel.LEFT);
		add(baseSize);
		baseSize.setToolTipText("Size before insetting");
		baseSize.setBorder(new EmptyBorder(2, 2, 2, 2));
		baseSize.setHorizontalTextPosition(JLabel.LEFT);
		
		addTextInput("ID", "id", this);
		addTextInput("Name", "name", this);

		add(createNumericGroup("Insets", "sprite_insets", "Left", "Right", "Top", "Bottom"));
		add(createNumericGroup("Center Offset", "center_offset", "X", "Y"));
		add(createNumericGroup("Render Size", "render_size", "Width", "Height"));
		
		addBooleanInput("Blend", "blend", this);
		addBooleanInput("Add Margin", "pad_image", this).setToolTipText(
				"Include a 1 pixel border around the image for linear filtering purposes");
		addBooleanInput("Hidden", "hidden", this);
		addBooleanInput("Has Stem", "has_stem", this);
		
		JPanel spacer = new JPanel();
		spacer.setAlignmentX(LEFT_ALIGNMENT);
		spacer.setMinimumSize(new Dimension(0, 0));
		add(spacer);
	}
	
	JComponent createNumericGroup(String groupName, String groupFieldName, String...labels) {
		JPanel pane = new JPanel();
		pane.setAlignmentX(LEFT_ALIGNMENT);
		pane.setBorder(BorderFactory.createTitledBorder(groupName));
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		for(String label : labels) {
			addNumberInput(label, groupFieldName + '.' + label.toLowerCase(), pane);
		}
		return pane;
	}
	
	private void updateField(String field, Object value) {
		setObjectMember(field, currentIcon, value);
		currentIcon.cropImage();
		IconProjEditor.renderPane.renderImage();
		IconProjEditor.frame.repaint();
	}
	
	void addNumberInput(String label, String field, JComponent container) {
		JFormattedTextField tf = new JFormattedTextField((Object) 0);
		tf.setColumns(5);
		addInput(label, field, tf, container);
	}
	
	void addTextInput(String label, String field, JComponent container) {
		addInput(label, field, new JTextField(12), container);
	}
	
	JCheckBox addBooleanInput(String label, final String field, JComponent container) {
		JCheckBox cb = new JCheckBox(label);
		cb.setAlignmentX(LEFT_ALIGNMENT);
		setAssociation(field, cb);
		cb.addChangeListener(ev -> updateField(field, cb.isSelected()));
		container.add(restrictHeight(cb));
		return cb;
	}
	
	void addInput(String label, final String field, final JTextField input, JComponent container) {
		JPanel row = new JPanel();
		row.setAlignmentX(LEFT_ALIGNMENT);
		row.setLayout(new FlowLayout(FlowLayout.RIGHT));
		row.add(new JLabel(label));
		row.add(input);
		setAssociation(field, input);
		input.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) { onChange(); }

			@Override
			public void removeUpdate(DocumentEvent e) { onChange(); }

			@Override
			public void changedUpdate(DocumentEvent e) { onChange(); }
			
			private void onChange() {
				Object val = input.getText();
				if(input.getClass() == JFormattedTextField.class) {
					val = ((JFormattedTextField) input).getValue();
				}
				updateField(field, val);
			}
		});
		input.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(final FocusEvent e) {
				if(e.getCause().name().startsWith("TRAVERSAL"))
					SwingUtilities.invokeLater(() -> input.selectAll());
			}
        });
		container.add(restrictHeight(row));
	}
	
	@SuppressWarnings("unchecked")
	void setIcon(Icon icon) {
		currentIcon = icon;
		cropPreview.icon = icon;
		filename.setText(icon.path.getName());
		baseSize.setText(icon.sprite.getWidth() + " x " + icon.sprite.getHeight());
		for(Map.Entry<String, JComponent> entry : fieldToComp.entrySet()) {
			Object value = getObjectMember(entry.getKey(), icon);
			JComponent input = entry.getValue();
			Class<?> cls = value == null ? null : value.getClass();
			if(cls == Boolean.class) {
				((JCheckBox) input).setSelected((Boolean) value);
			} else if(cls == Integer.class) {
				((JFormattedTextField) input).setValue(value);
			} else if(input.getClass() == JTextField.class) {
				if(value == null) value = "";
				((JTextField) input).setText((String) value);
			} else { // combo box strings
				value = value == null ? "Inherit" : toTitleCase((String) value);
				((JComboBox<String>) input).setSelectedItem(value);
			}
		}
	}
	
	private void setAssociation(String field, JComponent comp) {
		fieldToComp.put(field, comp);
		compToField.put(comp, field);
	}
}