package gamemap.icon;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.swing.TransferHandler;

public class DataLoadInfo {
	/** initial state when checking type */
	private static final int DATA_TYPE_UNKNOWN		= -2;
	public static final int	DATA_TYPE_UNSUPPORTED	= -1;
	public static final int	DATA_TYPE_JSON			= 0;
	public static final int	DATA_TYPE_IMAGE_LIST	= 1;
	
	private static int getDataType(List<File> files) {
		int foundType = DATA_TYPE_UNKNOWN;
		for(File f : files) {
			if(f.isDirectory()) return DATA_TYPE_UNSUPPORTED;

			String ext = f.getName().toLowerCase();
			int idx = ext.lastIndexOf('.') + 1;
			if(idx == 0) return DATA_TYPE_UNSUPPORTED;
			
			switch(ext.substring(idx)) {
				case "png":
				case "bmp":
				case "jpg":
				case "jpeg":
					if(foundType == DATA_TYPE_JSON) return DATA_TYPE_UNSUPPORTED;
					foundType = DATA_TYPE_IMAGE_LIST;
					break;
				case "json":
					if(foundType != DATA_TYPE_UNKNOWN) return DATA_TYPE_UNSUPPORTED;
					foundType = DATA_TYPE_JSON;
					break;
				default:
					return DATA_TYPE_UNSUPPORTED;
			}
		}
		// case should only happen if file count is somehow 0 
		return foundType == DATA_TYPE_UNKNOWN ? DATA_TYPE_UNSUPPORTED : foundType;
	}

	public final int		dataType;
	public final List<File>	files;

	@SuppressWarnings("unchecked")
	public DataLoadInfo(Transferable t) {
		List<File> f = null;
		int type = DATA_TYPE_UNSUPPORTED;

		if(Set.of(t.getTransferDataFlavors()).contains(DataFlavor.javaFileListFlavor)) {
			try {
				f = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
				type = getDataType(f);
			} catch(UnsupportedFlavorException | IOException e) {
				e.printStackTrace();
			}
		}
		
		files = f;
		dataType = type;
	}
	
	public DataLoadInfo(DropTargetDragEvent dtde) {
		this(dtde.getTransferable());
	}
	
	public DataLoadInfo(DropTargetDropEvent dtde) {
		this(dtde.getTransferable());
	}
	
	public DataLoadInfo(TransferHandler.TransferSupport support) {
		this(support.getTransferable());
	}
	
	public boolean isLoadable() {
		return dataType != DATA_TYPE_UNSUPPORTED;
	}
}