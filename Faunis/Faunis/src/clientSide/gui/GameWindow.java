/* Copyright 2012 - 2014 Simon Ley alias "skarute"
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
package clientSide.gui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import clientSide.ClientSettings;
import clientSide.client.Client;
import clientSide.graphics.Decoration;
import clientSide.graphics.GuiGraphics;
import clientSide.graphics.MapDrawable;
import clientSide.graphics.ImageScaler;
import clientSide.player.ClientPlayer;
import clientSide.userToClientOrders.UCParseCommandOrder;
import common.Logger;
import common.enums.ClientStatus;
import common.enums.Direction;
import common.graphics.graphicsContentManager.GraphicsContentManager;
import common.graphics.osseous.NotFoundException;
import common.graphics.osseous.path.ClearPath;
import common.movement.MovingTask;

/** Extends GraphWin by functionality specific to Faunis. */
public class GameWindow extends GraphWin {
	protected final Client parent;

	private BufferedImage grassBackground; // TODO: This is just a quick hack, remove it later

	private final MenuManager menuManager;
	private final JPanel commandPanel;
	private final JScrollPane loggingScrollPane;
	private final JTextPane loggingTextPane;
	private final StyledDocument loggingDocument;
	private final JButton commandSendButton;
	protected final JTextField commandEdit;

	private final SimpleAttributeSet errorTextStyle;
	private final SimpleAttributeSet systemTextStyle;
	private final SimpleAttributeSet whisperTextStyle;
	private final SimpleAttributeSet oldCommandTextStyle;
	private final SimpleAttributeSet broadcastTextStyle;

	private MyMouseListener mouseListener;

	public GameWindow(Client parent, int imgWidth, int imgHeight, String title) {
		super(imgWidth, imgHeight, title, false);
		this.parent = parent;

		graphics2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

		errorTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(errorTextStyle, Color.red);
		systemTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(systemTextStyle, new Color(0, 120, 120));
		whisperTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(whisperTextStyle, Color.blue);
		oldCommandTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(oldCommandTextStyle, Color.gray);
		broadcastTextStyle = new SimpleAttributeSet();
		StyleConstants.setForeground(broadcastTextStyle, new Color(140, 80, 0));

		loggingTextPane = new JTextPane();
		loggingTextPane.setEditable(false);
		loggingTextPane.setBackground(new Color(230, 230, 230));

		loggingDocument = loggingTextPane.getStyledDocument();
		assert(loggingDocument != null);

		loggingScrollPane = new JScrollPane(loggingTextPane);
		loggingScrollPane.setPreferredSize(new Dimension(100,100));
		JPanel loggingPanel = new JPanel(new BorderLayout());
		loggingPanel.add(BorderLayout.CENTER, loggingScrollPane);

		commandEdit = new JTextField();
		commandEdit.setPreferredSize(new Dimension(300,25));

		commandSendButton = new JButton("Send");
		commandSendButton.addActionListener(new CommandSendButtonListener());

		commandPanel = new JPanel();
		commandPanel.add(commandEdit);
		commandPanel.add(commandSendButton);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, drawingPanel, loggingPanel);
		loggingPanel.setMinimumSize(new Dimension(100, 100));
		splitPane.setResizeWeight(1); // drawingPanel gets any extra space occurring

		win.getRootPane().setDefaultButton(commandSendButton);
		mouseListener = new MyMouseListener();
		drawingPanel.setBackground(Color.BLACK);
		drawingPanel.addMouseListener(mouseListener);

		menuManager = new MenuManager(parent);
		win.setJMenuBar(menuManager.menuBar);

		win.getContentPane().add(BorderLayout.CENTER, splitPane);
		win.getContentPane().add(BorderLayout.SOUTH, commandPanel);

		win.pack();
	}


	private void loadGrassBackground() {
		ClientSettings settings = parent.getClientSettings();
		BufferedImage grass;
		try {
			grass = parent.getGraphicsContentManager(
			).floorGraphicsContentManager().image(new ClearPath("grass"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (NotFoundException e) {
			throw new RuntimeException(e);
		}
		float scaleFactor = settings.fieldHeight() / (float) grass.getHeight();
		BufferedImage scaledGrass = ImageScaler.scale(grass, scaleFactor);
		grassBackground = new BufferedImage(
			this.getImageWidth(), this.getImageHeight(), BufferedImage.TYPE_INT_RGB
		);
		/* We could draw grass onto grassBackground using FloorGraphicsContentManager, but
		 * since we already resolved the image to get its height, we can also do it by ourselves
		 */
		Graphics grassGraphics = grassBackground.getGraphics();
		for (int y = 0; y < 18; y++) {
			for (int x = 0; x < 20; x++) {
				grassGraphics.drawImage(
					scaledGrass, x*settings.fieldWidth(), y*settings.fieldHeight(), null
				);
			}
		}
	}


	public void notifyClientStatus(ClientStatus newStatus) {
		setTitle("Faunis - "+newStatus);
		menuManager.updateStatusMenu(newStatus);
	}


	@Override
	/** redraws Backbuffer (please call repaint() to copy it to the front) */
	public void draw() {
		GraphicsContentManager graphicsContentManager = parent.getGraphicsContentManager();
		graphics.setColor(parent.getClientSettings().mapBackgroundColor());
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());

		Point mausPos = this.mousePos();
		if (mausPos == null) {
			mausPos = new Point(0,0);
		}

		// depending on Client.clientStatus, other things have to be drawn
		ClientStatus status = parent.getClientStatus();

		switch (status) {
			case disconnected:
			case loggedOut:
			case noCharLoaded:
			try {
				graphicsContentManager.draw(new GuiGraphics("fullscreen_title"), 0, 0, graphics);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (NotFoundException e) {
				throw new RuntimeException(e);
			}
				break;
			case exploring:
			case fighting:
				// hack - draw grass background:
				if (grassBackground == null) {
					loadGrassBackground();
				}
				graphics.drawImage(grassBackground, 0, 0, null);
				// draw grid:
				drawFieldGrid();
				// write map and player name:
				writeMapAndPlayerInfo();
				// draw all player graphics:
				drawAllDrawables();
				break;
		}

		// draw clientStatus:
		drawClientStatus();
	}

	private void drawFieldGrid() {
		graphics.setColor(parent.getClientSettings().gridColor());
		int width = image.getWidth();
		int height = image.getHeight();
		int fieldWidth = parent.getClientSettings().fieldWidth();
		int fieldHeight = parent.getClientSettings().fieldHeight();
		int maxRow = height / fieldHeight + 1;
		int maxColumn = width / fieldWidth + 1;
		// draw horizontal lines:
		for (int y = 0; y < maxRow; y++) {
			graphics.drawRect(0, y*fieldHeight, width, 0);
		}
		// draw vertical lines:
		for (int x = 0; x < maxColumn; x++) {
			graphics.drawRect(x*fieldWidth, 0, 0, height);
		}
	}

	private void writeMapAndPlayerInfo() {
		String mapName = parent.getCurrentMapName();
		String playerName = parent.getCurrentPlayerName();

		graphics.setColor(Color.black);
		if (mapName != null) {
			graphics.drawString(mapName, 10, 20);
		}
		if (playerName != null) {
			ClientPlayer player = parent.getPlayers(playerName);
			if (player != null) {
				int x = player.getX();
				int y = player.getY();
				graphics.drawString(playerName, 10, 30);
				graphics.drawString("("+x+","+y+")", 10, 40);
			}
		}
	}

	private void drawClientStatus() {
		ClientStatus status = parent.getClientStatus();
		graphics.setColor(Color.black);
		graphics.drawString(status.toString(), 10, 10);
	}

	/** Draws all characters. */
	private void drawAllDrawables() {
		ArrayList<MapDrawable> allDrawables = parent.getAllDrawablesToDraw();
		for (MapDrawable drawable : allDrawables) {
			if (drawable instanceof ClientPlayer) {
				ClientPlayer player = (ClientPlayer) drawable;
				Point offset = calculateDrawingOffset(player);
				String playerName = player.getName();
				int playerNameWidth = graphics.getFontMetrics().stringWidth(playerName);
				graphics.setColor(Color.black);
				graphics.drawString(playerName, offset.x - playerNameWidth/2, offset.y + 15);
				try {
					parent.getGraphicsContentManager().draw(drawable, offset.x, offset.y, graphics);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
			} else if (drawable instanceof Decoration) {
				Decoration decoration = (Decoration) drawable;
				Point offset = calculateDrawingOffset(decoration);
				try {
					parent.getGraphicsContentManager().draw(drawable, offset.x, offset.y, graphics);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (NotFoundException e) {
					throw new RuntimeException(e);
				}
			} else {
				throw new RuntimeException(
					"drawAllDrawables(): Could not identify "+drawable.getClass()+"!"
				);
			}
		}
	}

	private Point calculateDrawingOffset(ClientPlayer player) {
		ClientSettings clientSettings = parent.getClientSettings();
		int numDeltaLevels = clientSettings.numberOfDeltaLevelStates();
		int fieldWidth = clientSettings.fieldWidth();
		int fieldHeight = clientSettings.fieldHeight();
		Direction direction = player.getDirection();
		Point delta = MovingTask.directionToDelta(direction);
		int deltaHoriz = (fieldWidth/numDeltaLevels) * delta.x;
		int deltaVert = (fieldHeight/numDeltaLevels) * delta.y;
		int deltaLevel = player.getDeltaLevel();
		int fieldX = player.getX();
		int fieldY = player.getY();
		// calculate resulting coordinates:
		int offsetX = fieldWidth/2 + fieldWidth * fieldX + deltaLevel*deltaHoriz;
		int offsetY = fieldHeight/2 + fieldHeight * fieldY + deltaLevel*deltaVert;
		return new Point(offsetX, offsetY);
	}
	private Point calculateDrawingOffset(Decoration decoration) {
		ClientSettings clientSettings = parent.getClientSettings();
		Point decorationPoint = new Point(decoration.getX(), decoration.getY());
		return clientSettings.mapFieldToCenterPixel(decorationPoint);
	}

	public Point getPosition(){
		return new Point(win.getLocation());
	}

	protected class CommandSendButtonListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent ev) {
			String command = commandEdit.getText();
			logOldCommandMessage(command);
			if (command.length() > 0 &&
				command.startsWith("/"))
			{
				UCParseCommandOrder order = new UCParseCommandOrder(command);
				parent.putUCOrder(order);

				Boolean response = null;
				while (response == null) {
					response = order.getResponse();
					Thread.yield();
				}

				if (response.booleanValue() == true) {
					commandEdit.setText(null);
				}
			}
		}
	}

	protected class MyMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			Point viewPoint = GameWindow.this.calculateViewPoint(e.getPoint());
			parent.mouseClick(viewPoint);
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
		}

		@Override
		public void mouseExited(MouseEvent e) {
		}
	}

	private void logMessage(String message, SimpleAttributeSet set) {
		try {
			loggingDocument.insertString(loggingDocument.getLength(),
										 message+"\n", set);
			this.loggingTextPane.setCaretPosition(loggingDocument.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
			Logger.log("Error appending a message to the loggingPane!");
		}
	}

	public void logErrorMessage(String message) {
		logMessage(message, errorTextStyle);
	}
	public void logWhisperMessage(String message) {
		logMessage(message, whisperTextStyle);
	}
	public void logOldCommandMessage(String message) {
		logMessage(message, oldCommandTextStyle);
	}
	public void logBroadcastMessage(String message) {
		logMessage(message, broadcastTextStyle);
	}

	public void logSystemMessage(String message) {
		logMessage(message, systemTextStyle);
	}
}
