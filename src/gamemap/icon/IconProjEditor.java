package gamemap.icon;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.google.gson.Gson;

public class IconProjEditor {
	static IterableListModel<Icon>	allIcons	= new IterableListModel<>();
	static File						projectFile;
	static JFrame					frame;
	static RenderPane				renderPane;
	static IconTab					iconTab;
	static JFileChooser				fileChooser;
	static JList<Icon>				iconList;

	public static void main(String[] args) {
		/*
		try {
			for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				if(info.getName().equalsIgnoreCase("Nimbus")) {
					UIManager.setLookAndFeel(info.getClassName());
				}
			}
		} catch(ReflectiveOperationException | UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		//*/
		frame = createWindow();
		frame.setVisible(true);
	}
	
	static class SaveStruct {
		List<Icon>	icons;
		String		bg_color;
	}
	
	private static void saveProject() {
		if(projectFile == null) {
			if(fileChooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
			
			projectFile = fileChooser.getSelectedFile();
			// add extension if the user didn't include it
			if(!projectFile.exists() && projectFile.getName().lastIndexOf('.') == -1) {
				projectFile = new File(projectFile.getParentFile(),
						projectFile.getName() + ".json");
			}
		}
		
		SaveStruct struct = new SaveStruct();
		struct.icons = new ArrayList<>();
		for(Icon icon : allIcons) {
			struct.icons.add(icon);
		}
		struct.bg_color = Utils.colorToHex(renderPane.imgBackground);
		Gson gson = Utils.getGson();
		String json = gson.toJson(struct);
		try {
			Files.write(projectFile.toPath(), json.getBytes(StandardCharsets.UTF_8));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	// abstraction so we can load json files from
	// drag & drop in addition to the filechooser
	private static void loadProject(File file) {
		projectFile = file;
		try {
			byte[] bytes = Files.readAllBytes(projectFile.toPath());
		
			String json = new String(bytes, StandardCharsets.UTF_8);
			Gson gson = Utils.getGson();
			SaveStruct struct = gson.fromJson(json, SaveStruct.class);
			
			Color bgColor = Utils.hexToColor(struct.bg_color);
			renderPane.imgBackground = bgColor;
			allIcons.clear();
			for(Icon icon : struct.icons) {
				icon.loadImage();
				icon.cropImage();
				allIcons.addElement(icon);
			}
			renderPane.renderImage();
			frame.repaint();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void loadProject() {
		if(fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
			loadProject(fileChooser.getSelectedFile());
		}
	}
	
	private static JScrollPane verticalScrollPane(JComponent comp) {
		JScrollPane pane =  new JScrollPane(comp, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pane.getVerticalScrollBar().setUnitIncrement(10);
		return pane;
	}
	
	private static JButton createButton(String text, ActionListener action) {
		JButton button = new JButton(text);
		button.addActionListener(action);
		return button;
	}
	
	private static class ExportIconList {
		List<ExportableIcon> icons = new ArrayList<>();
	}
	
	private static JPanel createMainTab() {
		JPanel tab = new JPanel();
		
		JPanel projBtns = new JPanel();
		projBtns.add(createButton("Save", e -> saveProject()));
		projBtns.add(createButton("Load", e -> loadProject()));
		projBtns.add(createButton("Export", e -> {
			File folder = new File(System.getProperty("user.dir"));
			if(projectFile != null) folder = projectFile.getParentFile();

			File out = new File(folder, "icons.png");
			try {
				ImageIO.write(renderPane.img, "PNG", out);
			} catch(IOException ioe) {
				ioe.printStackTrace();
				return;
			}
			
			out = new File(folder, "icons.json");
			Gson gson = Utils.getGson();
			ExportIconList outIcons = new ExportIconList();
			for(Icon icon : allIcons) {
				outIcons.icons.add(new ExportableIcon(icon));
			}
			
			String json = gson.toJson(outIcons);
			byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
			try {
				Files.write(out.toPath(), bytes);
			} catch(IOException ioe) {
				ioe.printStackTrace();
			}
		}));
		tab.add(Utils.restrictHeight(projBtns));
		
		tab.setLayout(new BoxLayout(tab, BoxLayout.Y_AXIS));
		JList<Icon> list = new JList<>(allIcons);
		list.setFixedCellWidth(192);
		list.addListSelectionListener(ev -> {
			Icon icon = list.getSelectedValue();
			if(icon != null) {
				iconTab.setIcon(icon);
			}
		});
		tab.add(verticalScrollPane(list));
		
		JPanel iconBtns = new JPanel();
		iconBtns.add(createButton("Up", e -> {
			int idx = list.getSelectedIndex();
			if(idx > 0) {
				Icon a = allIcons.get(idx);
				Icon b = allIcons.get(idx - 1);
				allIcons.set(idx, b);
				allIcons.set(idx - 1, a);
				list.setSelectedIndex(idx - 1);
				iconList.repaint();
			}
			
		}));
		iconBtns.add(createButton("Down", e -> {
			int idx = list.getSelectedIndex();
			if(idx != -1 && idx + 1 < allIcons.size()) {
				Icon a = allIcons.get(idx);
				Icon b = allIcons.get(idx + 1);
				allIcons.set(idx, b);
				allIcons.set(idx + 1, a);
				list.setSelectedIndex(idx + 1);
				iconList.repaint();
			}
		}));
		iconBtns.add(createButton("Delete", e -> {
			int idx = list.getSelectedIndex();
			if(idx != -1) {
				allIcons.remove(idx);
				renderPane.renderImage();
				frame.repaint();
			}
		}));
		tab.add(Utils.restrictHeight(iconBtns));
		iconList = list;
		
		return tab;
	}
	
	private static JFrame createWindow() {
		fileChooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter("Project file (*.json)", "json");
		fileChooser.addChoosableFileFilter(filter);
		fileChooser.setFileFilter(filter);
		fileChooser.setMultiSelectionEnabled(false);
		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Gamemap Icon Sheet Generator");
		
		RenderPane layoutArea = new RenderPane();
		layoutArea.setPreferredSize(new Dimension(512, 512));
		
		JTabbedPane tabs = new JTabbedPane();
		
		tabs.addTab("All Icons", createMainTab());
		
		iconTab = new IconTab();
		tabs.addTab("Icon Info", verticalScrollPane(iconTab));
		
		JSplitPane root = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layoutArea, tabs);
		root.setTransferHandler(new TransferHandler() {
			@Override
			public boolean canImport(TransferHandler.TransferSupport support) {
				if(!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;
				try {
					boolean ok = new DataLoadInfo(support).isLoadable();
					if(ok) support.setDropAction(LINK);
					return ok;
				} catch(InvalidDnDOperationException e) {
					// shitty hack to get around a moronic design choice in Java's dnd
					// getTransferable can't be called in the very moment when a drop takes place
					// however a check is performed in that instant to make sure the item is supported
					return true;
				}
			}
			
			@Override
			public boolean importData(TransferHandler.TransferSupport support) {
				support.setDropAction(LINK);
				DataLoadInfo info = new DataLoadInfo(support);
				if(info.dataType == DataLoadInfo.DATA_TYPE_JSON) {
					loadProject(info.files.get(0));
				} else if(info.dataType == DataLoadInfo.DATA_TYPE_IMAGE_LIST) {
					int imgWidth = renderPane.img == null ? 0 : renderPane.img.getWidth();
					int maxWidth = Math.max(imgWidth, 256);
					for(File f : info.files) {
						Icon icon = new Icon(f);
						icon.loadImage();
						icon.generateDefaultPropertiesFromImage();
						icon.render_size = icon.determineDefaultSize(allIcons);
						icon.setDefaultInitialPos(renderPane, allIcons, maxWidth);
						allIcons.addElement(icon);
					}
					renderPane.renderImage();
					frame.repaint();
				} else {
					return false;
				}
				return true;
			}
		});
		root.setResizeWeight(1);
		frame.setContentPane(root);
		
		layoutArea.icons = allIcons;
		renderPane = layoutArea;
		
		frame.pack();
		frame.setLocationRelativeTo(null);
		return frame;
	}
}