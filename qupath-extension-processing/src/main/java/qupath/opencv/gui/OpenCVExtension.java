/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2014 - 2016 The Queen's University of Belfast, Northern Ireland
 * Contact: IP Management (ipmanagement@qub.ac.uk)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package qupath.opencv.gui;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_dnn;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_ml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.icons.PathIconFactory;
import qupath.lib.gui.icons.PathIconFactory.PathIcons;
import qupath.lib.gui.tools.MenuTools;
import qupath.lib.gui.viewer.tools.PathTools;
import qupath.opencv.CellCountsCV;
import qupath.opencv.DetectCytokeratinCV;
import qupath.opencv.features.DelaunayClusteringPlugin;
import qupath.opencv.tools.WandToolCV;

/**
 * QuPath extension to add commands dependent on OpenCV.
 * 
 * @author Pete Bankhead
 *
 */
public class OpenCVExtension implements QuPathExtension {
	
	final private static Logger logger = LoggerFactory.getLogger(OpenCVExtension.class);
	
	static void addQuPathCommands(final QuPathGUI qupath) {
		
		logger.debug("Installing " + OpenCVExtension.class);
		
		Menu menuFeatures = qupath.getMenu("Analyze>Spatial analysis", true);
		MenuTools.addMenuItems(
				menuFeatures,
				qupath.createPluginAction("Delaunay cluster features 2D (experimental)", DelaunayClusteringPlugin.class, null)
				);

		Menu menuRegions = qupath.getMenu("Analyze>Region identification", true);
		MenuTools.addMenuItems(
				menuRegions,
//				QuPathGUI.createCommandAction(new TissueSegmentationCommand(qupath), "Tissue identification (OpenCV, experimental)"),
				qupath.createPluginAction("Create cytokeratin annotations (experimental)", DetectCytokeratinCV.class, null)
				);

		Menu menuCellAnalysis = qupath.getMenu("Analyze>Cell detection", true);
		MenuTools.addMenuItems(
				menuCellAnalysis,
				new SeparatorMenuItem(),
//				qupath.createPluginAction("Watershed nucleus detection (OpenCV, experimental)", WatershedNucleiCV.class, null),
				qupath.createPluginAction("Fast cell counts (brightfield)", CellCountsCV.class, null)
				);

		var classifierCommand = new OpenCvClassifierCommand(qupath);
		Menu menuClassify = qupath.getMenu("Classify>Object classification>Older classifiers", true);
		MenuTools.addMenuItems(
				menuClassify,
				null,
				qupath.createImageDataAction(imageData -> classifierCommand.run(), "Create detection classifier"));
		
		
//		// Add the Wand tool
//		logger.debug("Installing wand tool");
//		Platform.runLater(() -> {
//			WandToolCV wandTool = new WandToolCV(qupath);
//			qupath.putToolForMode(Modes.WAND, wandTool);
//		});
	}
	
	private static void ensureClassesLoaded() {
		logger.debug("Ensuring OpenCV classes are loaded");
		try (@SuppressWarnings("unchecked")
		var scope = new org.bytedeco.javacpp.PointerScope()) {
			Loader.load(opencv_core.class);
			Loader.load(opencv_imgproc.class);
			Loader.load(opencv_ml.class);
			Loader.load(opencv_dnn.class);
		} catch (Exception e) {
			logger.error("Error loading OpenCV: " + e.getLocalizedMessage(), e);
		}
	}


	@Override
	public void installExtension(QuPathGUI qupath) {
		// Can be annoying waiting a few seconds while classes are loaded later on
		var t = new Thread(() -> {
			// Add the Wand tool in a background thread (as it is rather slow to load)
			var wandTool = PathTools.createTool(new WandToolCV(qupath), "Wand tool",
					PathIconFactory.createNode(QuPathGUI.TOOLBAR_ICON_SIZE, QuPathGUI.TOOLBAR_ICON_SIZE, PathIcons.WAND_TOOL));
			logger.debug("Installing wand tool");
			Platform.runLater(() -> {
				qupath.installTool(wandTool, new KeyCodeCombination(KeyCode.W));
			});
			logger.debug("Loading OpenCV classes");
			ensureClassesLoaded();
		});
		t.setDaemon(true);
		t.start();

		
		addQuPathCommands(qupath);
	}
	
	@Override
	public String getName() {
		return "OpenCV extensions";
	}


	@Override
	public String getDescription() {
		return "QuPath commands that depend on the OpenCv computer vision library - http://opencv.org";
	}

}
