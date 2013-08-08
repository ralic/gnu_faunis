/* Copyright 2012, 2013 Simon Ley alias "skarute"
 * 
 * This file is part of Faunis.
 * 
 * Faunis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Faunis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General
 * Public License along with Faunis. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package common.graphics;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import clientSide.animation.AnimationData;
import clientSide.graphics.Bone;

import common.archivist.GraphicsContentArchivist;
import common.enums.AniCompoType;
import common.enums.BodyPart;
import common.enums.CharacterClass;
import common.enums.Direction;


public class GraphicsContentManager {
	private final String playerGraphicsPath;
	private final String decoGraphicsPath;
	private final String fileEnding;
	private HashMap<Direction, List<BodyPart>> drawingOrders;
	private List<BodyPart> compactDrawingOrder;
	private final HashMap<CharacterClass, GraphicsClassData> graphicsDataPerClass;
	private final HashMap<String, DecorationData> decorations;
	private final GraphicsContentArchivist archivist;
	
	/** Don't forget to call loadResourcesForClient() / loadResourcesForServer()
	 * afterwards! */
	public GraphicsContentManager(String playerGraphicsPath, String decoGraphicsPath, String graphicsPath,
								  String fileEnding) {
		this.playerGraphicsPath = playerGraphicsPath;
		this.decoGraphicsPath = decoGraphicsPath;
		this.fileEnding = fileEnding;
		this.archivist = new GraphicsContentArchivist(playerGraphicsPath, decoGraphicsPath, graphicsPath, fileEnding);
		decorations = new HashMap<String, DecorationData>();
		graphicsDataPerClass = new HashMap<CharacterClass, GraphicsClassData>();
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.put(type, new GraphicsClassData(archivist, type, playerGraphicsPath, fileEnding));
	}
	
	/** Must be called by client after calling the content manager's constructor,
	 * will initialise all the fields that the client needs. */
	public void loadResourcesForClient() {
		loadDecorations();
		compactDrawingOrder = new ArrayList<BodyPart>();
		compactDrawingOrder.add(BodyPart.compact);
		drawingOrders = new HashMap<Direction, List<BodyPart>>();
		loadDrawingOrders();
		loadCompositionTypes();
		loadBasicSettings();
		loadAvailableAnimations(true);
		loadAvailableFaces();
		loadBones();
	}

	/** Must be called by server after calling the content manager's constructor,
	 * will initialise all the fields that the server needs. */
	public void loadResourcesForServer() {
		loadBasicSettings();
		loadAvailableAnimations(false);
		loadAvailableFaces();
	}
	
	public Bone getBone(CharacterClass type, String animation, BodyPart part, Direction direction) {
		return graphicsDataPerClass.get(type).getBone(animation, part, direction);
	}
	
	public HashMap<Direction, List<BodyPart>> getDrawingOrders() {
		return this.drawingOrders;
	}
	
	public List<BodyPart> getDrawingOrder(Direction direction) {
		return this.drawingOrders.get(direction);
	}
	
	public List<BodyPart> getCompactDrawingOrder() {
		return compactDrawingOrder;
	}
	
	public Set<String> getAvailableAnimations(CharacterClass type) {
		return graphicsDataPerClass.get(type).getAvailableAnimations();
	}
	
	public AnimationData getAnimationData(CharacterClass type, String animation) {
		return graphicsDataPerClass.get(type).getAnimationData(animation);
	}
	
	public List<String> getAvailableFaces(CharacterClass type) {
		return graphicsDataPerClass.get(type).getAvailableFaces();
	}
	
	public AniCompoType getCompositionType(CharacterClass type) {
		return graphicsDataPerClass.get(type).getCompositionType();
	}
	
	public float getScale(CharacterClass type) {
		return graphicsDataPerClass.get(type).getScale();
	}
	
	public float getDecoScale(String decorationName) {
		return decorations.get(decorationName).getScale();
	}
	
	public GraphicsContentArchivist getArchivist() {
		return archivist;
	}
	
	private void loadBasicSettings() {
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.get(type).loadBasicSettings();
		for (DecorationData decorationData : decorations.values())
			decorationData.loadBasicSettings();
	}
	
	/** Determines which animations are available for which character class.
	 * If given flag countFrames is set, stores the number of frames available for
	 * an animation (Takes body/down as reference, though all limbs/directions must have
	 * the same number). <br/>
	 * Note that if frames should be counted, this method will require compositionTypes
	 * to be initialised. */
	private void loadAvailableAnimations(boolean countFrames) {
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.get(type).loadAvailableAnimations(countFrames);
	}
	
	private void loadAvailableFaces() {
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.get(type).loadAvailableFaces();
	}
	
	private void loadCompositionTypes() {
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.get(type).loadCompositionType();
	}
	
	/** determines in which order body parts have to be drawn */
	private void loadDrawingOrders() {
		List<BodyPart> leftList = Arrays.asList(
				  new BodyPart[] {BodyPart.rightArm,
				  BodyPart.rightLeg, BodyPart.body, BodyPart.head,
				  BodyPart.leftLeg, BodyPart.leftArm}
				);
		drawingOrders.put(Direction.left, leftList);
		
		List<BodyPart> rightList = Arrays.asList(
				  new BodyPart[] {BodyPart.leftArm,
				  BodyPart.leftLeg, BodyPart.body, BodyPart.head,
				  BodyPart.rightLeg, BodyPart.rightArm}
				);
		drawingOrders.put(Direction.right, rightList);
				
		List<BodyPart> downList = Arrays.asList(
				  new BodyPart[] {BodyPart.body,
				  BodyPart.leftLeg, BodyPart.rightLeg, BodyPart.leftArm,
				  BodyPart.rightArm, BodyPart.head}
				);
		drawingOrders.put(Direction.down, downList);
				
		List<BodyPart> upList = Arrays.asList(
				  new BodyPart[] {BodyPart.body,
				  BodyPart.leftLeg, BodyPart.rightLeg, BodyPart.leftArm,
				  BodyPart.rightArm, BodyPart.head}
				);
		drawingOrders.put(Direction.up, upList);
	}
	
	private void loadDecorations() {
		File[] decoDirs = archivist.listDecorationDirs();
		if (decoDirs == null) return;
		for (File decoDir : decoDirs) {
			String decoName = decoDir.getName();
			DecorationData decorationData = new DecorationData(archivist, decoName, decoGraphicsPath, fileEnding);
			decorations.put(decoName, decorationData);
		}
	}
	
	public void loadBones() {
		for (CharacterClass type : CharacterClass.values())
			graphicsDataPerClass.get(type).loadBones(this);
		for (DecorationData decorationData: decorations.values())
			decorationData.loadBone(this);
	}
	
	
	public Bone getDecoBone(String name) {
		return decorations.get(name).getBone();
	}
	
	public String getPlayerGraphicsPath() {
		return playerGraphicsPath;
	}
	
	public String getDecoGraphicsPath() {
		return decoGraphicsPath;
	}
	
	public String getFileEnding() {
		return fileEnding;
	}
}
