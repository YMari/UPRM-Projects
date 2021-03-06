package Game.World;

import Game.Entities.Dynamic.Player;
import Game.Entities.Static.LillyPad;
import Game.Entities.Static.Log;
import Game.Entities.Static.StaticBase;
import Game.Entities.Static.Tree;
import Game.Entities.Static.Turtle;
import Game.GameStates.State;
import Main.Handler;
import UI.UIManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Random;

/**
 * Literally the world. This class is very important to understand.
 * Here we spawn our hazards (StaticBase), and our tiles (BaseArea)
 * 
 * We move the screen, the player, and some hazards. 
 * 				How? Figure it out.
 */
public class WorldManager {

	private ArrayList<BaseArea> AreasAvailables;			// Lake, empty and grass area (NOTE: The empty tile is just the "sand" tile. Ik, weird name.)
	private ArrayList<StaticBase> StaticEntitiesAvailables;	// Has the hazards: LillyPad, Log, Tree, and Turtle.


	public ArrayList<BaseArea> SpawnedAreas;				// Areas currently on world
	public ArrayList<StaticBase> SpawnedHazards;			// Hazards currently on world.

	Long time;
	Boolean reset = true;

	Handler handler;

	private Player player;									// How do we find the frog coordinates? How do we find the Collisions? This bad boy.

	UIManager object = new UIManager(handler);
	UI.UIManager.Vector object2 = object.new Vector();


	private ID[][] grid;									
	private int gridWidth,gridHeight;						// Size of the grid. 
	private int movementSpeed;								// Movement of the tiles going downwards.

	boolean prevLillySpawn = false;


	public WorldManager(Handler handler) {
		this.handler = handler;


		AreasAvailables = new ArrayList<>();				// Here we add the Tiles to be utilized.
		StaticEntitiesAvailables = new ArrayList<>();		// Here we add the Hazards to be utilized.

		AreasAvailables.add(new GrassArea(handler, 0));		
		AreasAvailables.add(new WaterArea(handler, 0));
		AreasAvailables.add(new EmptyArea(handler, 0));

		StaticEntitiesAvailables.add(new LillyPad(handler, 0, 0));
		StaticEntitiesAvailables.add(new Log(handler, 0, 0));
		StaticEntitiesAvailables.add(new Tree(handler, 0, 0));
		StaticEntitiesAvailables.add(new Turtle(handler, 0, 0));

		SpawnedAreas = new ArrayList<>();
		SpawnedHazards = new ArrayList<>();

		player = new Player(handler);       

		gridWidth = handler.getWidth()/64;
		gridHeight = handler.getHeight()/64;
		movementSpeed = 1;
		// movementSpeed = 20; I dare you.

		/* 
		 * 	Spawn Areas in Map (2 extra areas spawned off screen)
		 *  To understand this, go down to randomArea(int yPosition) 
		 */

		for(int i=0; i<gridHeight+2; i++) {
			//			SpawnedAreas.add(randomArea((-2+i)*64));
			// spawns only empty areas at the start, should fix for no-water only on spawn
			SpawnedAreas.add(new EmptyArea(handler, ((-2+i)*64)));
		}

		player.setX((gridWidth/2)*64);
		player.setY((gridHeight-3)*64);

		// Not used atm.
		grid = new ID[gridWidth][gridHeight];
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				grid[x][y]=ID.EMPTY;
			}
		}
	}

	public void tick() {

		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[2])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[1];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[0])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[2];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[1])) {
			this.object2.word = this.object2.word + this.handler.getKeyManager().str[0];
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[3])) {
			this.object2.addVectors();
		}
		if(this.handler.getKeyManager().keyJustPressed(this.handler.getKeyManager().num[4]) && this.object2.isUIInstance) {
			this.object2.scalarProduct(handler);
		}

		if(this.reset) {
			time = System.currentTimeMillis();
			this.reset = false;
		}

		if(this.object2.isSorted) {

			if(System.currentTimeMillis() - this.time >= 2000) {		
				this.object2.setOnScreen(true);	
				this.reset = true;
			}

		}

		for (BaseArea area : SpawnedAreas) {
			area.tick();
		}
		for (StaticBase hazard : SpawnedHazards) {
			hazard.tick();
		}



		for (int i = 0; i < SpawnedAreas.size(); i++) {
			SpawnedAreas.get(i).setYPosition(SpawnedAreas.get(i).getYPosition() + movementSpeed);

			// Check if Area (thus a hazard as well) passed the screen.
			if (SpawnedAreas.get(i).getYPosition() > handler.getHeight()) {
				// Replace with a new random area and position it on top
				SpawnedAreas.set(i, randomArea(-2 * 64));
			}
			//Make sure players position is synchronized with area's movement
			if (SpawnedAreas.get(i).getYPosition() < player.getY()
					&& player.getY() - SpawnedAreas.get(i).getYPosition() < 3) {
				player.setY(SpawnedAreas.get(i).getYPosition());
			}
		}

		HazardMovement();

		player.tick();
		//make player move the same as the areas
		player.setY(player.getY()+movementSpeed); 

		object2.tick();

	}

	public void HazardMovement() {

		for (int i = 0; i < SpawnedHazards.size(); i++) {

			// Moves hazard down
			SpawnedHazards.get(i).setY(SpawnedHazards.get(i).getY() + movementSpeed);

			// Moves Log or Turtle to the right
			if (SpawnedHazards.get(i) instanceof Log) {
				SpawnedHazards.get(i).setX(SpawnedHazards.get(i).getX() + 1);

				// Verifies the hazards Rectangles aren't null and
				// If the player Rectangle intersects with the Log or Turtle Rectangle, then
				// move player to the right.
				if (SpawnedHazards.get(i).GetCollision() != null
						&& player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision())) {
					player.setX(player.getX() + 1);

				}
			}
			else if (SpawnedHazards.get(i) instanceof Turtle) {
				SpawnedHazards.get(i).setX(SpawnedHazards.get(i).getX() - 1);
				if (SpawnedHazards.get(i).GetCollision() != null
						&& player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision())) {
					player.setX(player.getX() - 1);
				}
			}

			//////////////loop logs and turtles/////////////
			
			if (SpawnedHazards.get(i) instanceof Turtle) {
				if (SpawnedHazards.get(i).getX() < 0) {
					SpawnedHazards.add(new Turtle(handler, handler.getWidth() + 64, SpawnedHazards.get(i).getY()));
					SpawnedHazards.remove(i);
				}
			}
			else if (SpawnedHazards.get(i) instanceof Log) {
				if (SpawnedHazards.get(i).getX() > handler.getWidth()) {
					SpawnedHazards.add(new Log(handler, -128, SpawnedHazards.get(i).getY()));
					SpawnedHazards.remove(i);
				}
			}
			
			// if hazard has passed the screen height, then remove this hazard.
			if (SpawnedHazards.get(i).getY() > handler.getHeight()) {
				SpawnedHazards.remove(i);
			}
			

		}
	}


	/*
	 * Given a yPosition, this method will return a random Area out of the Available ones.)
	 * It is also in charge of spawning hazards at a specific condition.
	 */

	public void render(Graphics g){

		for(BaseArea area : SpawnedAreas) {
			area.render(g);
		}

		for (StaticBase hazards : SpawnedHazards) {
			hazards.render(g);

		}

		player.render(g);       
		this.object2.render(g);      

	}

	/////test if player in water////////                //////////failed attempt///////////
//	public void WaterCollision() {
//		for(int i = 0; i < SpawnedAreas.size(); i++) {
//			if (SpawnedAreas.get(i) instanceof WaterArea && SpawnedAreas.get(i).getYPosition() == player.getY()) {
//				if (handler.getWater().water.contains(player.getX(), player.getY(), player.getWidth(), player.getHeight())) {
//					State.setState(handler.getGame().gameOverState);
//				}
//			}
//		}		
//	}
//	public void WaterCollision() {
//		for(int i = 0; i < SpawnedAreas.size(); i++) {
//			if (SpawnedAreas.get(i) instanceof WaterArea && SpawnedAreas.get(i).getYPosition() == player.getY()) {
//				for (int j = 0; j < SpawnedHazards.size(); j++) {
//					if (SpawnedHazards.get(i) instanceof Log || SpawnedHazards.get(i) instanceof Turtle || SpawnedHazards.get(i) instanceof LillyPad ) {
//						if (player.getPlayerCollision().intersects(SpawnedHazards.get(i).GetCollision())){
//							return;
//						}
//					}
//				}
//			}
//			State.setState(handler.getGame().gameOverState);
//		}
//	}

	/*
	 * Given a yPosition, this method will return a random Area out of the Available ones.)
	 * It is also in charge of spawning hazards at a specific condition.
	 */

	public BaseArea randomArea(int yPosition) {
		Random rand = new Random();

		// From the AreasAvailable, get me any random one.
		BaseArea randomArea = AreasAvailables.get(rand.nextInt(AreasAvailables.size())); 

		if(randomArea instanceof GrassArea) {
			randomArea = new GrassArea(handler, yPosition);
			SpawnHazard(yPosition, randomArea);
		}
		else if(randomArea instanceof WaterArea) {
			randomArea = new WaterArea(handler, yPosition);
			SpawnHazard(yPosition, randomArea);
		}
		else {
			randomArea = new EmptyArea(handler, yPosition);
		}

		return randomArea;
	}


	/*
	 * Given a yPositionm this method will add a new hazard to the SpawnedHazards ArrayList
	 */

	public void SpawnHazard(int yPosition, BaseArea area) {
		Random rand = new Random();
		int randInt;
		int choice = rand.nextInt(7);
		// Chooses between Log or Lillypad
		if (area instanceof WaterArea) {
			if (choice <=2) {
				rand = new Random();
				choice = rand.nextInt(4);
				randInt = 64 * rand.nextInt(9);
				if (choice == 0) {
					SpawnedHazards.add(new Log(handler, randInt, yPosition));
				}
				else if (choice == 1) {
					SpawnedHazards.add(new Log(handler, randInt, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
				}
				else if (choice == 2) {
					SpawnedHazards.add(new Log(handler, randInt, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128*2, yPosition));
				}
				else {
					SpawnedHazards.add(new Log(handler, randInt, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128*2, yPosition));
					SpawnedHazards.add(new Log(handler, randInt - 128*3, yPosition));
				}

			}
			else if (choice >=5){
				if (prevLillySpawn == false) {
					rand = new Random();
					choice = rand.nextInt(4);
					for (int i = 0; i <= choice; i++) {
						randInt = 64 * rand.nextInt(9);
						SpawnedHazards.add(new LillyPad(handler, randInt, yPosition));
						prevLillySpawn = true;
					}
				}
				else {
					rand = new Random();
					choice = rand.nextInt(2);
					if (choice <= 1) {
						rand = new Random();
						choice = rand.nextInt(4);
						randInt = 64 * rand.nextInt(4);
						if (choice == 0) {
							SpawnedHazards.add(new Log(handler, randInt, yPosition));
						}
						else if (choice == 1) {
							SpawnedHazards.add(new Log(handler, randInt, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
						}
						else if (choice == 2) {
							SpawnedHazards.add(new Log(handler, randInt, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128*2, yPosition));
						}
						else {
							SpawnedHazards.add(new Log(handler, randInt, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128*2, yPosition));
							SpawnedHazards.add(new Log(handler, randInt - 128*3, yPosition));
						}
					}
					else {
						randInt = 64 * rand.nextInt(3);
						SpawnedHazards.add(new Turtle(handler, randInt, yPosition));
					}
					prevLillySpawn = false;
				}
			}
			else {
				randInt = 64 * rand.nextInt(3);
				SpawnedHazards.add(new Turtle(handler, randInt, yPosition));
			}
		} else if (area instanceof GrassArea) {
			if (choice <=3) {
				rand = new Random();
				choice = rand.nextInt(4);
				for (int i = 0; i < choice; i++) {
					randInt = 64 * rand.nextInt(9);
					SpawnedHazards.add(new Tree(handler, randInt, yPosition));
				}
			}
		}
	}
}

